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

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyRuntimeException;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.python.Version;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Scriptlet;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.StackFrame;

/**
 * A {@link LanguageAdapter} that supports the <a
 * href="http://groovy.codehaus.org/">Groovy</a> language.
 * 
 * @author Tal Liron
 */
public class GroovyAdapter extends LanguageAdapterBase
{
	//
	// Constants
	//

	/**
	 * The Groovy binding attribute.
	 */
	public static final String GROOVY_BINDING = "groovy.binding";

	/**
	 * The default base directory for cached executables.
	 */
	public static final String GROOVY_CACHE_DIR = "groovy";

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
		if( x instanceof GroovyRuntimeException )
		{
			GroovyRuntimeException groovyRuntimeException = (GroovyRuntimeException) x;
			Throwable cause = x.getCause();
			if( cause instanceof ExecutionException )
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause );
				executionException.getStack().addAll( ( (ExecutionException) cause ).getStack() );
				executionException.getStack().add( new StackFrame( documentName, groovyRuntimeException.getNode().getLineNumber(), groovyRuntimeException.getNode().getColumnNumber() ) );
				return executionException;
			}
			else if( cause instanceof ParsingException )
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause );
				executionException.getStack().addAll( ( (ParsingException) cause ).getStack() );
				executionException.getStack().add( new StackFrame( documentName, groovyRuntimeException.getNode().getLineNumber(), groovyRuntimeException.getNode().getColumnNumber() ) );
				return executionException;
			}
			else
				return new ExecutionException( documentName, groovyRuntimeException.getNode().getLineNumber(), groovyRuntimeException.getNode().getColumnNumber(), groovyRuntimeException.getMessageWithoutLocationText(),
					x );
		}
		else
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
	public GroovyAdapter() throws LanguageAdapterException
	{
		super( "Groovy", Version.getVersion(), "Groovy", Version.getVersion(), Arrays.asList( "gv" ), null, Arrays.asList( "groovy", "gv" ), null );

		// This will allow the class loader to load our auxiliary classes (see
		// GroovyScriptlet.prepare)
		groovyClassLoader.addClasspath( getCacheDir().getPath() );
	}

	//
	// Attributes
	//

	/**
	 * Gets a Groovy binding stored in the execution context, creating it if it
	 * doesn't exist. Each execution context is guaranteed to have its own
	 * Groovy binding. The binding is updated to match the writers and exposed
	 * variables in the execution context.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The Groovy binding
	 */
	public Binding getBinding( ExecutionContext executionContext )
	{
		Binding binding = (Binding) executionContext.getAttributes().get( GROOVY_BINDING );

		if( binding == null )
		{
			binding = new Binding();
			executionContext.getAttributes().put( GROOVY_BINDING, binding );
		}

		binding.setVariable( "out", executionContext.getWriter() );
		binding.setVariable( "err", executionContext.getErrorWriter() );

		// Expose variables in binding
		for( Map.Entry<String, Object> entry : executionContext.getExposedVariables().entrySet() )
			binding.setVariable( entry.getKey(), entry.getValue() );

		return binding;
	}

	/**
	 * The base directory for cached executables.
	 * 
	 * @return The cache directory
	 */
	public File getCacheDir()
	{
		return new File( LanguageManager.getCachePath(), GROOVY_CACHE_DIR );
	}

	//
	// LanguageAdapter
	//

	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = literal.replaceAll( "\\n", "\\\\n" );
		literal = literal.replaceAll( "\\'", "\\\\'" );
		return "print('" + literal + "');";
	}

	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "print(" + expression + ");";
	}

	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		return executable.getExposedExecutableName() + ".container.includeDocument(" + expression + ");";
	}

	public Scriptlet createScriptlet( String sourceCode, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new GroovyScriptlet( sourceCode, position, startLineNumber, startColumnNumber, executable, this );
	}

	public Object invoke( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		Binding binding = getBinding( executionContext );
		Object o = binding.getVariable( entryPointName );
		if( !( o instanceof Closure ) )
			throw new NoSuchMethodException( entryPointName );
		try
		{
			Closure closure = (Closure) o;
			return closure.call( arguments );
		}
		catch( Exception x )
		{
			throw GroovyAdapter.createExecutionException( executable.getDocumentName(), x );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	protected final GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

	// //////////////////////////////////////////////////////////////////////////
	// Private
}
