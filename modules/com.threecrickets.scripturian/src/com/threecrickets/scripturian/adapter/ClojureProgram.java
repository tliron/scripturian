/**
 * Copyright 2009-2011 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.adapter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import clojure.lang.Compiler;
import clojure.lang.DynamicClassLoader;
import clojure.lang.LineNumberingPushbackReader;
import clojure.lang.LispReader;
import clojure.lang.Namespace;
import clojure.lang.PersistentArrayMap;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import clojure.lang.Compiler.CompilerException;
import clojure.lang.LispReader.ReaderException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;

/**
 * @author Tal Liron
 */
public class ClojureProgram extends ProgramBase<ClojureAdapter>
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param isScriptlet
	 *        Whether the source code is a scriptlet
	 * @param position
	 *        The program's position in the executable
	 * @param startLineNumber
	 *        The line number in the document for where the program's source
	 *        code begins
	 * @param startColumnNumber
	 *        The column number in the document for where the program's source
	 *        code begins
	 * @param executable
	 *        The executable
	 * @param adapter
	 *        The language adapter
	 */
	public ClojureProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, ClojureAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	@Override
	public void prepare() throws PreparationException
	{
		if( formsReference.get() != null )
			return;

		ArrayList<ClojureProgram.Form> forms = new ArrayList<ClojureProgram.Form>();

		// This code was extracted from Compiler.load()

		Object EOF = new Object();
		LineNumberingPushbackReader pushbackReader = new LineNumberingPushbackReader( new StringReader( sourceCode ) );

		try
		{
			for( Object form = LispReader.read( pushbackReader, false, EOF, false ); form != EOF; form = LispReader.read( pushbackReader, false, EOF, false ) )
				forms.add( new Form( form, startLineNumber + pushbackReader.getLineNumber() ) );

			formsReference.compareAndSet( null, forms );
		}
		catch( ReaderException x )
		{
			// Note that we can only detect the first column
			throw new PreparationException( executable.getDocumentName(), startLineNumber + pushbackReader.getLineNumber(), pushbackReader.atLineStart() ? 0 : -1, x.getCause() );
		}
		catch( Exception x )
		{
			// Note that we can only detect the first column
			throw new PreparationException( executable.getDocumentName(), startLineNumber + pushbackReader.getLineNumber(), pushbackReader.atLineStart() ? 0 : -1, x );
		}
	}

	/**
	 * An unused experiment to compile into bytecode. Not very successful. The
	 * main problem is that we can only compile a known namespace, and we cannot
	 * trivially execute in an on-the-fly namespace we create per thread.
	 * <p>
	 * It remains an open question, too, whether this would significantly
	 * improve performance. Clojure's highly dynamic nature would make compiled
	 * code quite uninteresting. It's likely that the JVM's optimization would
	 * work just as well on "interpreted" Clojure.
	 */
	@SuppressWarnings("unused")
	private void prepare2()
	{
		String compilationName;
		Symbol LOGGER_NAME = Symbol.create( "-logger-name" );
		Symbol COMPILE = Symbol.create( "compile" );

		try
		{
			compilationName = "ScripturianClojure" + this.hashCode();
			FileWriter writer = new FileWriter( "data/code/clojure/" + compilationName + ".clj" );
			String sourceCode = "(ns " + compilationName
				+ " (:gen-class :constructors {[String] []} :init init :state state)) (defn -init [s] [[] (quote s)]) (clojure.core/in-ns 'user) (clojure.core/refer 'clojure.core) " + this.sourceCode;
			writer.write( sourceCode );
			writer.close();
			Var.pushThreadBindings( RT.map( RT.CURRENT_NS, RT.CURRENT_NS.deref(), Compiler.COMPILE_PATH, "data/code/clojure", LOGGER_NAME, "" ) );
			try
			{
				COMPILE.invoke( Symbol.intern( compilationName ) );
			}
			finally
			{
				Var.popThreadBindings();
			}

			// An attempt to run our compiled class.

			try
			{
				Class<?> cls = Class.forName( compilationName + "__init" );
				System.out.println( cls );
				Method load = cls.getMethod( "load" );
				Object r = load.invoke( null );
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
		}
		catch( Exception e1 )
		{
			e1.printStackTrace();
		}
	}

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Namespace ns = ClojureAdapter.getClojureNamespace( executionContext );

		// Append library locations to dynamic class loader
		DynamicClassLoader loader = (DynamicClassLoader) RT.makeClassLoader();
		for( URI uri : executionContext.getLibraryLocations() )
		{
			try
			{
				loader.addURL( uri.toURL() );
			}
			catch( IllegalArgumentException x )
			{
				// URI is not a file
			}
			catch( IOException x )
			{
			}
		}

		HashMap<Var, Object> threadBindings = new HashMap<Var, Object>();

		// bindings.put( RT.CURRENT_NS, RT.CURRENT_NS.deref() );
		// threadBindings.put( ClojureAdapter.ALLOW_UNRESOLVED_VARS, RT.T );

		// We must push *ns* in order to use in-ns below
		threadBindings.put( RT.CURRENT_NS, ns );

		threadBindings.put( Compiler.LOADER, loader );

		// Set out/err
		threadBindings.put( RT.OUT, new PrintWriter( executionContext.getWriterOrDefault() ) );
		threadBindings.put( RT.ERR, new PrintWriter( executionContext.getErrorWriterOrDefault() ) );

		// For debug information
		threadBindings.put( Compiler.SOURCE_PATH, executable.getDocumentName() );
		threadBindings.put( Compiler.SOURCE, executable.getDocumentName() );
		threadBindings.put( Compiler.LINE_BEFORE, startLineNumber );
		threadBindings.put( Compiler.LINE_AFTER, startLineNumber );
		threadBindings.put( Compiler.LINE, startLineNumber );

		for( Map.Entry<String, Object> entry : executionContext.getServices().entrySet() )
			threadBindings.put( Var.intern( ns, Symbol.intern( entry.getKey() ) ), entry.getValue() );

		try
		{
			Var.pushThreadBindings( PersistentArrayMap.create( threadBindings ) );

			try
			{
				// Enter our namspace
				ClojureAdapter.IN_NS.invoke( ns.getName() );

				// Refer to clojure.core
				ClojureAdapter.REFER.invoke( ClojureAdapter.CLOJURE_CORE );

				// Expose context variables as vars in namespace
				for( Map.Entry<String, Object> entry : executionContext.getServices().entrySet() )
					Var.intern( ns, Symbol.intern( entry.getKey() ), entry.getValue() );

				Collection<ClojureProgram.Form> forms = formsReference.get();
				if( forms != null )
				{
					// This code is mostly identical to Compiler.load().

					ClojureProgram.Form lastForm = null;
					for( ClojureProgram.Form form : forms )
					{
						if( lastForm != null )
							Compiler.LINE_BEFORE.set( lastForm.lineNumber );
						Compiler.LINE_AFTER.set( form.lineNumber );
						Compiler.LINE.set( form.lineNumber );
						Compiler.eval( form.form );
						lastForm = form;
					}
				}
				else
				{
					// This code mostly identical to Compiler.load().

					Object EOF = new Object();
					LineNumberingPushbackReader pushbackReader = new LineNumberingPushbackReader( new StringReader( sourceCode ) );

					try
					{
						for( Object form = LispReader.read( pushbackReader, false, EOF, false ); form != EOF; form = LispReader.read( pushbackReader, false, EOF, false ) )
						{
							Compiler.LINE_AFTER.set( pushbackReader.getLineNumber() );
							Compiler.LINE.set( pushbackReader.getLineNumber() );
							Compiler.eval( form );
							Compiler.LINE_BEFORE.set( pushbackReader.getLineNumber() );
						}
					}
					catch( ReaderException x )
					{
						// Note that we can only detect the first column
						throw new ParsingException( executable.getDocumentName(), startLineNumber + pushbackReader.getLineNumber(), pushbackReader.atLineStart() ? 1 : -1, x );
					}
				}
			}
			catch( CompilerException x )
			{
				throw ClojureAdapter.createExecutionException( startColumnNumber, x );
			}
			catch( Exception x )
			{
				throw new ExecutionException( (String) Compiler.SOURCE.deref(), startLineNumber + (Integer) Compiler.LINE.deref(), -1, x );
			}
		}
		finally
		{
			Var.popThreadBindings();
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The cached parsed forms.
	 */
	private final AtomicReference<Collection<ClojureProgram.Form>> formsReference = new AtomicReference<Collection<ClojureProgram.Form>>();

	/**
	 * A simple wrapper for parsed Clojure forms that adds line number
	 * information.
	 * 
	 * @author Tal Liron
	 */
	private static class Form
	{
		public Form( Object form, int lineNumber )
		{
			this.form = form;
			this.lineNumber = lineNumber;
		}

		public final Object form;

		public final int lineNumber;
	}
}