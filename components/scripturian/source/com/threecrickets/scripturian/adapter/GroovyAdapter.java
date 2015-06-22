/**
 * Copyright 2009-2015 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
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
import groovy.lang.GroovySystem;
import groovy.lang.MissingPropertyException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;

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
	public static final String GROOVY_BINDING = GroovyAdapter.class.getCanonicalName() + ".binding";

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
	 * @param executable
	 *        The executable
	 * @param startLineNumber
	 *        The line number in the document for where the program's source
	 *        code begins
	 * @param x
	 *        The exception
	 * @return The execution exception
	 */
	public static ExecutionException createExecutionException( Executable executable, int startLineNumber, Exception x )
	{
		if( x instanceof ExecutionException )
			return (ExecutionException) x;
		else if( x instanceof MultipleCompilationErrorsException )
		{
			MultipleCompilationErrorsException multiple = (MultipleCompilationErrorsException) x;
			String message = multiple.getMessage();
			ArrayList<StackFrame> stack = new ArrayList<StackFrame>();
			for( Object error : multiple.getErrorCollector().getErrors() )
			{
				if( error instanceof SyntaxErrorMessage )
				{
					SyntaxErrorMessage syntaxError = (SyntaxErrorMessage) error;
					message = syntaxError.getCause().getOriginalMessage();
					SyntaxException syntaxException = syntaxError.getCause();
					stack.add( new StackFrame( syntaxException.getSourceLocator(), syntaxException.getLine(), syntaxException.getStartColumn() ) );
				}
			}
			ExecutionException executionException = new ExecutionException( message, multiple );
			executionException.getStack().addAll( stack );
			return executionException;
		}
		else if( x instanceof GroovyRuntimeException )
		{
			GroovyRuntimeException groovyRuntimeException = (GroovyRuntimeException) x;
			Throwable cause = x.getCause();
			if( cause instanceof ExecutionException )
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause );
				executionException.getStack().addAll( ( (ExecutionException) cause ).getStack() );
				executionException.getStack().add(
					new StackFrame( executable.getDocumentName(), groovyRuntimeException.getNode() != null ? groovyRuntimeException.getNode().getLineNumber() : startLineNumber,
						groovyRuntimeException.getNode() != null ? groovyRuntimeException.getNode().getColumnNumber() : -1 ) );
				return executionException;
			}
			else if( cause instanceof ParsingException )
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause );
				executionException.getStack().addAll( ( (ParsingException) cause ).getStack() );
				executionException.getStack().add(
					new StackFrame( executable.getDocumentName(), groovyRuntimeException.getNode() != null ? groovyRuntimeException.getNode().getLineNumber() : startLineNumber,
						groovyRuntimeException.getNode() != null ? groovyRuntimeException.getNode().getColumnNumber() : -1 ) );
				return executionException;
			}
			else
				return new ExecutionException( executable.getDocumentName(), groovyRuntimeException.getNode() != null ? groovyRuntimeException.getNode().getLineNumber() : startLineNumber,
					groovyRuntimeException.getNode() != null ? groovyRuntimeException.getNode().getColumnNumber() : -1, groovyRuntimeException.getMessage(), x );
		}
		else
			return new ExecutionException( executable.getDocumentName(), x );
	}

	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 *         In case of an initialization error
	 */
	public GroovyAdapter() throws LanguageAdapterException
	{
		super( "Groovy", GroovySystem.getVersion(), "Groovy", GroovySystem.getVersion(), Arrays.asList( "groovy", "gv" ), "groovy", Arrays.asList( "groovy", "gv" ), "groovy" );

		// This will allow the class loader to load our auxiliary classes (see
		// GroovyProgram.prepare)
		groovyClassLoader.addClasspath( getCacheDir().getPath() );
	}

	//
	// Attributes
	//

	/**
	 * Gets a Groovy binding stored in the execution context, creating it if it
	 * doesn't exist. Each execution context is guaranteed to have its own
	 * Groovy binding. The binding is updated to match the writers and services
	 * in the execution context.
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

		// Set out/err
		binding.setVariable( "out", executionContext.getWriterOrDefault() );
		binding.setVariable( "err", executionContext.getErrorWriterOrDefault() );

		// Expose services in binding
		for( Map.Entry<String, Object> entry : executionContext.getServices().entrySet() )
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

	@Override
	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = ScripturianUtil.doubleQuotedLiteral( literal );
		return "print(" + literal + ");";
	}

	@Override
	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "print(" + expression + ");";
	}

	@Override
	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		String containerIncludeCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_COMMAND_ATTRIBUTE );
		return executable.getExecutableServiceName() + ".container." + containerIncludeCommand + "(" + expression + ");";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new GroovyProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	@Override
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		Binding binding = getBinding( executionContext );
		
		Object o;
		try
		{
			o = binding.getProperty( entryPointName );
		}
		catch( MissingPropertyException x )
		{
			throw new NoSuchMethodException( entryPointName );
		}
		if( !( o instanceof Closure ) )
			throw new NoSuchMethodException( entryPointName );
		try
		{
			Closure<?> closure = (Closure<?>) o;
			return closure.call( arguments );
		}
		catch( Exception x )
		{
			throw GroovyAdapter.createExecutionException( executable, 0, x );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	protected final GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
}
