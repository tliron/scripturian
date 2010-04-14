/**
 * Copyright 2009-2010 Three Crickets LLC.
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
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import clojure.lang.Compiler;
import clojure.lang.LineNumberingPushbackReader;
import clojure.lang.LispReader;
import clojure.lang.Namespace;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import clojure.lang.Compiler.CompilerException;
import clojure.lang.LispReader.ReaderException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.Scriptlet;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;
import com.threecrickets.scripturian.exception.StackFrame;

/**
 * @author Tal Liron
 */
public class ClojureScriptlet implements Scriptlet
{
	//
	// Construction
	//

	public ClojureScriptlet( String sourceCode, int startLineNumber, int startColumnNumber, Executable executable )
	{
		this.sourceCode = sourceCode;
		this.startLineNumber = startLineNumber;
		this.startColumnNumber = startColumnNumber;
		this.executable = executable;
	}

	//
	// Scriptlet
	//

	public void prepare() throws PreparationException
	{
		forms = new ArrayList<ClojureScriptlet.Form>();

		// This code was extracted from Compiler.load()

		Object EOF = new Object();
		LineNumberingPushbackReader pushbackReader = new LineNumberingPushbackReader( new StringReader( sourceCode ) );

		try
		{
			for( Object form = LispReader.read( pushbackReader, false, EOF, false ); form != EOF; form = LispReader.read( pushbackReader, false, EOF, false ) )
				forms.add( new Form( form, startLineNumber + pushbackReader.getLineNumber() ) );
		}
		catch( ReaderException x )
		{
			forms = null;
			// Note that we can only detect the first column
			throw new PreparationException( executable.getDocumentName(), startLineNumber + pushbackReader.getLineNumber(), pushbackReader.atLineStart() ? 0 : -1, x.getCause() );
		}
		catch( Exception x )
		{
			forms = null;
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

	public Object execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		try
		{
			// We must push *ns* in order to use (in-ns) below
			Var.pushThreadBindings( RT.map( RT.CURRENT_NS, RT.CURRENT_NS.deref(), RT.OUT, executionContext.getWriter(), RT.ERR, executionContext.getErrorWriter() ) );

			Object r = null;

			Namespace ns = ClojureAdapter.getClojureNamespace( executionContext );

			try
			{
				ClojureAdapter.IN_NS.invoke( ns.getName() );
				ClojureAdapter.REFER.invoke( ClojureAdapter.CLOJURE_CORE );
			}
			catch( Exception x )
			{
				throw new ExecutionException( executable.getDocumentName(), startLineNumber, startColumnNumber, x );
			}

			if( forms != null )
			{
				// This code is mostly identical to Compiler.load().

				Var.pushThreadBindings( RT.map( Compiler.LOADER, RT.makeClassLoader(), Compiler.SOURCE_PATH, executable.getDocumentName(), Compiler.SOURCE, executable.getDocumentName(), RT.CURRENT_NS, RT.CURRENT_NS
					.deref(), Compiler.LINE_BEFORE, startLineNumber, Compiler.LINE_AFTER, startLineNumber, Compiler.LINE, startLineNumber ) );

				try
				{
					ClojureScriptlet.Form lastForm = null;
					for( ClojureScriptlet.Form form : forms )
					{
						if( lastForm != null )
							Compiler.LINE_BEFORE.set( lastForm.lineNumber );
						Compiler.LINE_AFTER.set( form.lineNumber );
						Compiler.LINE.set( form.lineNumber );
						r = Compiler.eval( form.form );
						lastForm = form;
					}
				}
				catch( CompilerException x )
				{
					throw createExecutionException( x );
				}
				catch( Exception x )
				{
					throw new ExecutionException( (String) Compiler.SOURCE.deref(), startLineNumber + (Integer) Compiler.LINE.deref(), -1, x );
				}
				finally
				{
					Var.popThreadBindings();
				}
			}
			else
			{
				// This code mostly identical to Compiler.load().

				Object EOF = new Object();
				LineNumberingPushbackReader pushbackReader = new LineNumberingPushbackReader( new StringReader( sourceCode ) );

				Var.pushThreadBindings( RT.map( Compiler.LOADER, RT.makeClassLoader(), Compiler.SOURCE_PATH, executable.getDocumentName(), Compiler.SOURCE, executable.getDocumentName(), RT.CURRENT_NS, RT.CURRENT_NS
					.deref(), Compiler.LINE_BEFORE, startLineNumber, Compiler.LINE_AFTER, startLineNumber, Compiler.LINE, startLineNumber ) );

				try
				{
					for( Object form = LispReader.read( pushbackReader, false, EOF, false ); form != EOF; form = LispReader.read( pushbackReader, false, EOF, false ) )
					{
						Compiler.LINE_AFTER.set( pushbackReader.getLineNumber() );
						Compiler.LINE.set( pushbackReader.getLineNumber() );
						r = Compiler.eval( form );
						Compiler.LINE_BEFORE.set( pushbackReader.getLineNumber() );
					}
				}
				catch( ReaderException x )
				{
					// Note that we can only detect the first column
					throw new ParsingException( executable.getDocumentName(), startLineNumber + pushbackReader.getLineNumber(), pushbackReader.atLineStart() ? 1 : -1, x );
				}
				catch( CompilerException x )
				{
					throw createExecutionException( x );
				}
				catch( Exception x )
				{
					throw new ExecutionException( (String) Compiler.SOURCE.deref(), startLineNumber + (Integer) Compiler.LINE.deref(), -1, x );
				}
				finally
				{
					Var.popThreadBindings();
				}
			}

			return r;
		}
		finally
		{
			Var.popThreadBindings();
		}
	}

	public String getSourceCode()
	{
		return sourceCode;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final String sourceCode;

	private final int startLineNumber;

	private final int startColumnNumber;

	private final Executable executable;

	private Collection<ClojureScriptlet.Form> forms;

	/**
	 * Annoyingly, Clojure's CompilerException accepts stack information
	 * (Compiler.SOURCE, Compiler.LINE) in its constructor, but does not store
	 * it directly. This information is indirectly available to us in the error
	 * message. We'll just have to parse this error message to get our valuable
	 * data!
	 * 
	 * @param message
	 * @param stack
	 * @return
	 */
	private String extractStack( String message, Collection<StackFrame> stack )
	{
		int length = message.length();
		if( length > 0 )
		{
			if( message.charAt( length - 1 ) == ')' )
			{
				int lastParens1 = message.lastIndexOf( '(' );
				if( lastParens1 != -1 )
				{
					String stackFrame = message.substring( lastParens1 + 1, message.length() - 1 );
					String[] split = stackFrame.split( ":" );
					if( split.length == 2 )
					{
						String documentName = split[0];
						try
						{
							int lineNumber = Integer.parseInt( split[1] );

							// System.out.println( message );
							stack.add( new StackFrame( documentName, lineNumber, -1 ) );

							message = message.substring( 0, lastParens1 ).trim();
							// return extractStack( message, stack );
						}
						catch( NumberFormatException x )
						{
						}
					}
				}
			}
		}

		return message;
	}

	/**
	 * @param x
	 * @return
	 */
	private ExecutionException createExecutionException( CompilerException x )
	{
		String message = x.getMessage();
		ArrayList<StackFrame> stack = new ArrayList<StackFrame>();

		if( x.getCause() instanceof ExecutionException )
		{
			if( message.startsWith( "com.threecrickets.scripturian.exception.ExecutionException: " ) )
				message = message.substring( "com.threecrickets.scripturian.exception.ExecutionException: ".length() );

			// Add the cause's stack to ours
			stack.addAll( ( (ExecutionException) x.getCause() ).getStack() );
		}

		message = extractStack( message, stack );

		if( !stack.isEmpty() )
		{
			ExecutionException executionException = new ExecutionException( message, x );
			executionException.getStack().addAll( stack );
			return executionException;
		}
		else
			return new ExecutionException( (String) Compiler.SOURCE.deref(), startLineNumber + (Integer) Compiler.LINE.deref(), -1, x );
	}

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