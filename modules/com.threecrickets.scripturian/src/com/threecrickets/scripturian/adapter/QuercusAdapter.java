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
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.WriterStreamImpl;
import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * A {@link LanguageAdapter} that supports the PHP language as implemented by <a
 * href="http://quercus.caucho.com/">Quercus</a>. Note that Quercus compilation
 * is only available in Resin Professional, and as such no compilation is
 * supported in this adapter.
 * 
 * @author Tal Liron
 */
public class QuercusAdapter extends LanguageAdapterBase
{
	//
	// Constants
	//

	/**
	 * The Quercus environment attribute.
	 */
	public static final String QUERCUS_ENVIRONMENT = "quercus.environment";

	/**
	 * The Quercus writer stream attribute.
	 */
	public static final String QUERCUS_WRITER_STREAM = "quercus.writerStream";

	/**
	 * The default base directory for cached executables.
	 */
	public static final String PHP_CACHE_DIR = "php";

	//
	// Static operations
	//

	/**
	 * Creates an execution exception with a full stack.
	 * 
	 * @param documentName
	 *        The document name
	 * @param x
	 *        The exception
	 * @return The execution exception
	 */
	public static ExecutionException createExecutionException( String documentName, Exception x )
	{
		if( x instanceof QuercusException )
		{
			// QuercusException quercusException = (QuercusException) x;
			Throwable cause = x.getCause();
			if( cause instanceof ExecutionException )
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause );
				executionException.getStack().addAll( ( (ExecutionException) cause ).getStack() );
				// executionException.getStack().add( new StackFrame(
				// documentName,
				// groovyRuntimeException.getNode().getLineNumber(),
				// groovyRuntimeException.getNode().getColumnNumber() ) );
				return executionException;
			}
			else if( cause instanceof ParsingException )
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause );
				executionException.getStack().addAll( ( (ParsingException) cause ).getStack() );
				// executionException.getStack().add( new StackFrame(
				// documentName,
				// groovyRuntimeException.getNode().getLineNumber(),
				// groovyRuntimeException.getNode().getColumnNumber() ) );
				return executionException;
			}
			// else
			// return new ExecutionException( documentName,
			// groovyRuntimeException.getNode().getLineNumber(),
			// groovyRuntimeException.getNode().getColumnNumber(),
			// groovyRuntimeException.getMessageWithoutLocationText(),
			// x );
		}

		return new ExecutionException( x.getMessage(), x );
	}

	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @throws LanguageAdapterException
	 */
	public QuercusAdapter() throws LanguageAdapterException
	{
		super( "Quercus", staticQuercusRuntime.getVersion(), "PHP", staticQuercusRuntime.getPhpVersion(), Arrays.asList( "php" ), null, Arrays.asList( "php", "quercus" ), null );

		quercusRuntime = new Quercus();
		quercusRuntime.init();
	}

	//
	// Attributes
	//

	/**
	 * Gets a Quercus environment stored in the execution context, creating it
	 * if it doesn't exist. Each execution context is guaranteed to have its own
	 * Quercus environment. The environment is updated to match the writers and
	 * exposed variables in the execution context.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The Quercus environment
	 */
	public Env getEnvironment( ExecutionContext executionContext )
	{
		Env environment = (Env) executionContext.getAttributes().get( QUERCUS_ENVIRONMENT );
		WriterStreamImpl writerStream = (WriterStreamImpl) executionContext.getAttributes().get( QUERCUS_WRITER_STREAM );

		if( environment == null )
		{
			writerStream = new WriterStreamImpl();
			WriteStream writeStream = new WriteStream( writerStream );
			try
			{
				writeStream.setEncoding( "utf-8" );
			}
			catch( UnsupportedEncodingException x )
			{
			}
			environment = new Env( quercusRuntime, null, writeStream, null, null );
			executionContext.getAttributes().put( QUERCUS_ENVIRONMENT, environment );
			executionContext.getAttributes().put( QUERCUS_WRITER_STREAM, writerStream );
		}

		writerStream.setWriter( executionContext.getWriterOrDefault() );

		// Expose variables as script globals
		for( Map.Entry<String, Object> entry : executionContext.getExposedVariables().entrySet() )
			environment.setScriptGlobal( entry.getKey(), entry.getValue() );

		environment.start();

		return environment;
	}

	/**
	 * The base directory for cached executables.
	 * 
	 * @return The cache directory
	 */
	public File getCacheDir()
	{
		return new File( LanguageManager.getCachePath(), PHP_CACHE_DIR );
	}

	//
	// LanguageAdapter
	//

	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = literal.replaceAll( "\\n", "\\\\n" );
		literal = literal.replaceAll( "\\\"", "\\\\\"" );
		return "print(\"" + literal + "\");";
	}

	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "print(" + expression + ");";
	}

	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		return "$" + executable.getExposedExecutableName() + "->container->includeDocument(" + expression + ");";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new QuercusProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	@Override
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		entryPointName = toPhpStyle( entryPointName );
		Env env = getEnvironment( executionContext );
		try
		{
			Value[] quercusArguments = new Value[arguments.length];
			for( int i = 0; i < arguments.length; i++ )
				quercusArguments[i] = env.wrapJava( arguments[i] );
			Value r = env.call( entryPointName, quercusArguments );
			return r.toJavaObject();
		}
		catch( Exception x )
		{
			x.printStackTrace();
			return null;
		}
	}

	@Override
	public void releaseContext( ExecutionContext executionContext )
	{
		Env environment = (Env) executionContext.getAttributes().get( QUERCUS_ENVIRONMENT );
		if( environment != null )
		{
			try
			{
				environment.close();
			}
			catch( NullPointerException x )
			{
				// This fails in the middle because we don't have a page set for
				// the environment
			}
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	/**
	 * The Quercus runtime.
	 */
	protected final Quercus quercusRuntime;

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * A static Quercys runtime used for version information.
	 */
	private static final Quercus staticQuercusRuntime = new Quercus();

	/**
	 * From somethingLikeThis to something_like_this.
	 * 
	 * @param camelCase
	 *        somethingLikeThis
	 * @return something_like_this
	 */
	private static String toPhpStyle( String camelCase )
	{
		StringBuilder r = new StringBuilder();
		char c = camelCase.charAt( 0 );
		if( Character.isUpperCase( c ) )
			r.append( Character.toLowerCase( c ) );
		else
			r.append( c );
		for( int i = 1; i < camelCase.length(); i++ )
		{
			c = camelCase.charAt( i );
			if( Character.isUpperCase( c ) )
			{
				r.append( '_' );
				r.append( Character.toLowerCase( c ) );
			}
			else
				r.append( c );
		}
		return r.toString();
	}
}
