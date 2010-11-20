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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusErrorException;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.EnvVar;
import com.caucho.quercus.env.EnvVarImpl;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.parser.QuercusParseException;
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
import com.threecrickets.scripturian.exception.StackFrame;
import com.threecrickets.scripturian.internal.ScripturianUtil;

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

	public static ParsingException createParsingException( String documentName, QuercusParseException x )
	{
		ArrayList<StackFrame> stack = new ArrayList<StackFrame>();
		Throwable cause = x.getCause();
		String message = cause != null ? cause.getMessage() : x.getMessage();

		if( cause instanceof ParsingException )
			// Add the cause's stack to ours
			stack.addAll( ( (ParsingException) cause ).getStack() );
		else
			message = extractExceptionStackFromMessage( message, stack );

		if( !stack.isEmpty() )
		{
			ParsingException parsingException = new ParsingException( message, x );
			parsingException.getStack().addAll( stack );
			return parsingException;
		}
		else
			return new ParsingException( documentName, message, x );
	}

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
		ArrayList<StackFrame> stack = new ArrayList<StackFrame>();
		Throwable cause = x.getCause();
		String message = cause != null && cause.getMessage() != null ? cause.getMessage() : x.getMessage();

		if( cause instanceof ExecutionException )
			// Add the cause's stack to ours
			stack.addAll( ( (ExecutionException) cause ).getStack() );
		else if( cause instanceof ParsingException )
			// Add the cause's stack to ours
			stack.addAll( ( (ParsingException) cause ).getStack() );
		else if( x instanceof QuercusException )
			message = extractExceptionStackFromMessage( message, stack );

		if( !stack.isEmpty() )
		{
			ExecutionException executionException = new ExecutionException( message, x );
			executionException.getStack().addAll( stack );
			return executionException;
		}
		else
			return new ExecutionException( documentName, message, x );
	}

	public static String extractExceptionStackFromMessage( String message, Collection<StackFrame> stack )
	{
		if( message != null )
		{
			int firstColon = message.indexOf( ':' );
			if( firstColon != -1 )
			{
				int secondColon = message.indexOf( ':', firstColon + 1 );
				if( secondColon != -1 )
				{
					String documentName = message.substring( 0, firstColon );
					try
					{
						int lineNumber = Integer.parseInt( message.substring( firstColon + 1, secondColon ) );
						message = message.substring( secondColon + 2 );
						stack.add( new StackFrame( documentName, lineNumber, -1 ) );
						return message;
					}
					catch( NumberFormatException x )
					{
					}
				}
			}
		}

		return message;
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
		quercusRuntime.start();
	}

	//
	// Attributes
	//

	/**
	 * Gets a Quercus environment stored in the execution context, creating it
	 * if it doesn't exist. Each execution context is guaranteed to have its own
	 * Quercus environment. The environment is updated to match the writers and
	 * services in the execution context.
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
		else
		{
			try
			{
				environment.getOut().flush();
			}
			catch( IOException x )
			{
			}
		}

		// Env childEnvironment = new Env( quercusRuntime, null, writeStream,
		// null, null );
		// childEnvironment.restoreState( environment.saveState() );
		// environment = childEnvironment;

		// Set writer
		writerStream.setWriter( executionContext.getWriterOrDefault() );

		environment.start();

		// Append library locations to include_path
		for( URI uri : executionContext.getLibraryLocations() )
		{
			try
			{
				String path = new File( uri ).getPath();
				environment.evalCode( "set_include_path(get_include_path().PATH_SEPARATOR.'" + path.replace( "'", "\\'" ) + "');" );
			}
			catch( IllegalArgumentException x )
			{
				// URI is not a file
			}
			catch( IOException x )
			{
			}
		}

		// Expose services as script globals
		for( Map.Entry<String, Object> entry : executionContext.getServices().entrySet() )
		{
			// This is the best way to make sure we override Quercus predefined
			// globals
			EnvVar var = new EnvVarImpl( new Var() );
			var.set( environment.wrapJava( entry.getValue() ) );
			environment.getGlobalEnv().put( environment.createString( entry.getKey() ), var );
		}

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
		literal = ScripturianUtil.doubleQuotedLiteral( literal );
		return "print(" + literal + ");";
	}

	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "print(" + expression + ");";
	}

	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		String containerIncludeExpressionCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_EXPRESSION_COMMAND );
		return "$" + executable.getExecutableServiceName() + "->container->" + containerIncludeExpressionCommand + "(" + expression + ");";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new QuercusProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	@Override
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		entryPointName = toPhpStyle( entryPointName );
		Env environment = getClonedEnvironment( executionContext );

		try
		{
			Value[] quercusArguments = new Value[arguments.length];
			for( int i = arguments.length - 1; i >= 0; i-- )
				quercusArguments[i] = environment.wrapJava( arguments[i] );

			Value r = environment.call( entryPointName, quercusArguments );
			return r.toJavaObject();
		}
		catch( QuercusErrorException x )
		{
			throw new NoSuchMethodException( entryPointName );
		}
		catch( Exception x )
		{
			throw createExecutionException( executable.getDocumentName(), x );
		}
		finally
		{
			try
			{
				environment.getOut().flush();
				executionContext.getWriter().flush();

				try
				{
					environment.close();
				}
				catch( NullPointerException x )
				{
					// close() fails in the middle because we don't have a page
					// set for the environment
				}
			}
			catch( IOException xx )
			{
			}
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
				// close() fails in the middle because we don't have a page set
				// for the environment
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
	 * A static Quercus runtime used for version information.
	 */
	private static final Quercus staticQuercusRuntime = new Quercus();

	private Env getClonedEnvironment( ExecutionContext executionContext )
	{
		Env environment = (Env) executionContext.getAttributes().get( QUERCUS_ENVIRONMENT );
		Env clonedEnvironment = new Env( quercusRuntime, null, environment.getOut(), null, null );
		clonedEnvironment.restoreState( environment.saveState() );
		clonedEnvironment.start();
		return clonedEnvironment;
	}

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
		for( int i = 1, length = camelCase.length(); i < length; i++ )
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
