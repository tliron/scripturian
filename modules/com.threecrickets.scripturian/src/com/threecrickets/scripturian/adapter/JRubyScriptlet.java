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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.compiler.ASTCompiler;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.exceptions.RaiseException;
import org.jruby.util.JRubyClassLoader;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;

/**
 * @author Tal Liron
 */
class JRubyScriptlet extends ScriptletBase<JRubyAdapter>
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param position
	 *        The scriptlet position in the document
	 * @param startLineNumber
	 *        The start line number
	 * @param startColumnNumber
	 *        The start column number
	 * @param executable
	 *        The executable
	 * @param adapter
	 *        The language adapter
	 */
	public JRubyScriptlet( String sourceCode, int position, int startLineNumber, int startColumnNumber, Executable executable, JRubyAdapter adapter )
	{
		super( sourceCode, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Scriptlet
	//

	private File getClassFileForDocument()
	{
		String filename = executable.getPartition() + executable.getDocumentName();
		int lastDot = filename.lastIndexOf( '.' );
		if( lastDot != -1 )
			filename = filename.substring( 0, lastDot );
		filename = filename.replace( ".", "_" );
		filename += "_" + position + ".class";
		File file = new File( new File( "cache/ruby" ), filename );
		//System.out.println( file.getPath() );
		return file;
	}

	private static String getClassNameForFile( File file )
	{
		String className = file.getPath().replace( '/', '.' );
		// Remove ".class"
		className = className.substring( 0, className.length() - 6 );
		return className;
	}

	private static String getFixedClassNameForFile( File file )
	{
		String className = file.getPath();
		// Remove ".class"
		className = className.substring( 0, className.length() - 6 );
		return className;
	}

	public void prepare() throws PreparationException
	{
		try
		{
			// Note that we parse the node for a different runtime than the
			// one we will run in. It's unclear what the repercussions of
			// this would be, but we haven't detected any trouble yet.

			File file = getClassFileForDocument();
			String className = getClassNameForFile( file );

			/*
			 * if( file.exists() ) { // Use cached compiled code byte[]
			 * classByteArray = ScripturianUtil.getBytes( file );
			 * JRubyClassLoader classLoader = new JRubyClassLoader(
			 * compilerRuntime.getJRubyClassLoader() ); classLoader.defineClass(
			 * className, classByteArray ); Class<?> scriptClass =
			 * classLoader.loadClass( className ); script = (Script)
			 * scriptClass.newInstance(); } else
			 */
			{
				Node node = adapter.compilerRuntime.parseEval( sourceCode, executable.getDocumentName(), adapter.compilerRuntime.getCurrentContext().getCurrentScope(), startLineNumber - 1 );

				ASTInspector astInspector = new ASTInspector();
				ASTCompiler astCompiler = adapter.compilerRuntime.getInstanceConfig().newCompiler();
				StandardASMCompiler asmCompiler = new StandardASMCompiler( getFixedClassNameForFile( file ), executable.getDocumentName() );
				JRubyClassLoader classLoader = new JRubyClassLoader( adapter.compilerRuntime.getJRubyClassLoader() );

				astInspector.inspect( node );
				astCompiler.compileRoot( node, asmCompiler, astInspector, true, false );
				script = (Script) asmCompiler.loadClass( classLoader ).newInstance();

				// Cache it!
				file.getParentFile().mkdirs();
				FileOutputStream stream = new FileOutputStream( file );
				stream.write( asmCompiler.getClassByteArray() );
				stream.close();

				// script = compilerRuntime.tryCompile( node );
			}
		}
		catch( RaiseException x )
		{
			// JRuby does not fill in the stack trace correctly, though the
			// error message is fine

			// StackTraceElement[] stack = x.getStackTrace();
			// if( ( stack != null ) && stack.length > 0 )
			// throw new PreparationException( stack[0].getFileName(),
			// stack[0].getLineNumber(), -1, x );

			throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, x );
		}
		catch( InstantiationException x )
		{
			throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, x );
		}
		catch( IllegalAccessException x )
		{
			throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, x );
		}
		catch( ClassNotFoundException x )
		{
			throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, x );
		}
		catch( IOException x )
		{
			throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, x );
		}
	}

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Ruby rubyRuntime = adapter.getRubyRuntime( executionContext );

		try
		{
			if( script != null )
			{
				rubyRuntime.runScript( script );
				// return value.toJava( Object.class );
			}
			else
			{
				rubyRuntime.executeScript( sourceCode, executable.getDocumentName() );
				// rubyRuntime.evalScriptlet( sourceCode );
				// return value.toJava( Object.class );
			}
		}
		catch( RaiseException x )
		{
			throw JRubyAdapter.createExecutionException( x );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The compiled script.
	 */
	private Script script;
}