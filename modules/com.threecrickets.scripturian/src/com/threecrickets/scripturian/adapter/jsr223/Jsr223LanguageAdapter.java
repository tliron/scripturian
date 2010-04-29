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

package com.threecrickets.scripturian.adapter.jsr223;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * Common implementation base for language adapters over JSR-223.
 * 
 * @author Tal Liron
 */
public abstract class Jsr223LanguageAdapter implements LanguageAdapter
{
	//
	// Constants
	//

	public static final String JSR223_SCRIPT_ENGINE_MANAGER = "jsr223.scriptEngineManager";

	public static final String JSR223_SCRIPT_ENGINE_NAME = "jsr223.scriptEngineName";

	public static final String JSR223_SCRIPT_CONTEXT = "jsr223.scriptContext";

	public static final String JSR223_SCRIPT_ENGINES = "jsr223.scriptEngines";

	//
	// Static operations
	//

	/**
	 * Gets a JSR-223 script engine manager stored in the language manager
	 * associated with the execution context, creating it if it doesn't exist.
	 * Each language manager is guaranteed to have its own script context.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The language manager
	 */
	public static ScriptEngineManager getScriptEngineManager( ExecutionContext executionContext )
	{
		ScriptEngineManager scriptEngineManager = (ScriptEngineManager) executionContext.getManager().getAttributes().get( JSR223_SCRIPT_ENGINE_MANAGER );
		if( scriptEngineManager == null )
		{
			scriptEngineManager = new ScriptEngineManager();
			ScriptEngineManager existing = (ScriptEngineManager) executionContext.getManager().getAttributes().putIfAbsent( JSR223_SCRIPT_ENGINE_MANAGER, scriptEngineManager );
			if( existing != null )
				scriptEngineManager = existing;
		}
		return scriptEngineManager;
	}

	/**
	 * Gets a JSR-223 script engine stored in the execution context, creating it
	 * if it doesn't exist. Each execution context is guaranteed to have at most
	 * one script engine instance per script engine.
	 * 
	 * @param scriptEngineName
	 *        The script engine name
	 * @param executable
	 *        The executable
	 * @param executionContext
	 *        The execution context
	 * @return The script engine
	 * @throws ParsingException
	 */
	@SuppressWarnings("unchecked")
	public static ScriptEngine getScriptEngine( LanguageAdapter adapter, String scriptEngineName, Executable executable, ExecutionContext executionContext ) throws LanguageAdapterException
	{
		Map<String, ScriptEngine> scriptEngines = (Map<String, ScriptEngine>) executionContext.getAttributes().get( JSR223_SCRIPT_ENGINES );
		if( scriptEngines == null )
		{
			scriptEngines = new HashMap<String, ScriptEngine>();
			executionContext.getAttributes().put( JSR223_SCRIPT_ENGINES, scriptEngines );
		}

		ScriptEngine scriptEngine = scriptEngines.get( scriptEngineName );

		if( scriptEngine == null )
		{
			ScriptEngineManager scriptEngineManager = getScriptEngineManager( executionContext );
			scriptEngine = scriptEngineManager.getEngineByName( scriptEngineName );
			if( scriptEngine == null )
				throw new LanguageAdapterException( adapter.getClass(), "Unsupported script engine: " + scriptEngineName );

			// (Note that some script engines do not even
			// provide a default context -- Jepp, for example -- so
			// it's generally a good idea to explicitly set one)
			scriptEngine.setContext( getScriptContext( executionContext ) );

			scriptEngines.put( scriptEngineName, scriptEngine );
		}

		return scriptEngine;
	}

	/**
	 * Gets a JSR-223 script context stored in the execution context, creating
	 * it if it doesn't exist. Each execution context is guaranteed to have its
	 * own script context.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The script context
	 */
	public static ScriptContext getScriptContext( ExecutionContext executionContext )
	{
		ScriptContext scriptContext = (ScriptContext) executionContext.getAttributes().get( JSR223_SCRIPT_CONTEXT );

		if( scriptContext == null )
		{
			scriptContext = new SimpleScriptContext();
			executionContext.getAttributes().put( JSR223_SCRIPT_CONTEXT, scriptContext );
			scriptContext.setBindings( new SimpleBindings(), ScriptContext.ENGINE_SCOPE );
			scriptContext.setBindings( new SimpleBindings(), ScriptContext.GLOBAL_SCOPE );
		}

		scriptContext.setWriter( executionContext.getWriter() );
		scriptContext.setErrorWriter( executionContext.getErrorWriter() );
		for( Map.Entry<String, Object> entry : executionContext.getExposedVariables().entrySet() )
			scriptContext.setAttribute( entry.getKey(), entry.getValue(), ScriptContext.ENGINE_SCOPE );

		return scriptContext;
	}

	/**
	 * Creates an execution exception from a JSR-223 script exception.
	 * 
	 * @param documentName
	 *        The document name
	 * @param scriptException
	 *        The script exception
	 * @return The execution exception
	 */
	public static ExecutionException createExecutionException( String documentName, ScriptException scriptException )
	{
		return new ExecutionException( documentName, scriptException.getLineNumber(), scriptException.getColumnNumber(), scriptException.getMessage(), scriptException.getCause() );
	}

	/**
	 * Creates an execution exception from a throwable, making sure to let every
	 * JSR-223 language adapter have a chance at unpacking it.
	 * 
	 * @param documentName
	 *        The document name
	 * @param manager
	 *        The language manager
	 * @param throwable
	 *        The throwable
	 * @return The execution exception
	 */
	public static ExecutionException createExecutionException( String documentName, LanguageManager manager, Throwable throwable )
	{
		Throwable wrapped = throwable;
		while( wrapped != null )
		{
			if( wrapped instanceof ExecutionException )
				return (ExecutionException) wrapped;

			// Try JSR-223 language adapters
			Throwable causeOrExecutionException = null;
			for( LanguageAdapter adapter : manager.getAdapters() )
			{
				if( adapter instanceof Jsr223LanguageAdapter )
				{
					causeOrExecutionException = ( (Jsr223LanguageAdapter) adapter ).getCauseOrExecutionException( documentName, wrapped );
					if( causeOrExecutionException != null )
						break;
				}
			}

			if( causeOrExecutionException != null )
			{
				// Found it!
				if( causeOrExecutionException instanceof ExecutionException )
					return (ExecutionException) causeOrExecutionException;

				// We are unwrapped
				wrapped = causeOrExecutionException;
				continue;
			}

			// Unwrap
			wrapped = wrapped.getCause();
		}

		if( throwable instanceof ScriptException )
			// Extract from ScriptException
			return createExecutionException( documentName, (ScriptException) throwable );
		else
			// Unknown
			return new ExecutionException( documentName, throwable.getMessage(), throwable );
	}

	//
	// Construction
	//

	/**
	 * Construction. Uses the JSR-223 script engine factory to gather the
	 * language adapter attributes. Sub-classes must be annotated with
	 * {@link ScriptEngines}.
	 * 
	 * @throws LanguageAdapterException
	 */
	public Jsr223LanguageAdapter() throws LanguageAdapterException
	{
		ScriptEngines scriptEngineNames = getClass().getAnnotation( ScriptEngines.class );
		if( scriptEngineNames == null )
			throw new LanguageAdapterException( getClass(), getClass().getName() + " does not have a ScriptEngines annotation" );

		String scriptEngineName = scriptEngineNames.value()[0];
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		scriptEngine = scriptEngineManager.getEngineByName( scriptEngineName );
		if( scriptEngine == null )
			throw new LanguageAdapterException( getClass(), getClass().getName() + " could not load ScriptEngine " + scriptEngineName );

		ScriptEngineFactory factory = scriptEngine.getFactory();

		attributes.put( NAME, "JSR-223/" + factory.getEngineName() );
		attributes.put( VERSION, factory.getEngineVersion() );
		attributes.put( LANGUAGE_NAME, factory.getLanguageName() );
		attributes.put( LANGUAGE_VERSION, factory.getLanguageVersion() );
		attributes.put( EXTENSIONS, new ArrayList<String>( factory.getExtensions() ) );
		attributes.put( DEFAULT_EXTENSION, factory.getExtensions().get( 0 ) );
		attributes.put( TAGS, Arrays.asList( scriptEngineNames.value() ) );
		attributes.put( DEFAULT_TAG, factory.getNames().get( 0 ) );

		attributes.put( JSR223_SCRIPT_ENGINE_NAME, scriptEngineName );
	}

	//
	// Attributes
	//

	/**
	 * Gets a JSR-223 script engine for this language stored in the execution
	 * context, creating it if it doesn't exist.
	 * 
	 * @param executable
	 *        The executable
	 * @param executionContext
	 *        The execution context
	 * @return The script engine
	 * @throws ExecutionException
	 */
	public ScriptEngine getScriptEngine( Executable executable, ExecutionContext executionContext ) throws ExecutionException
	{
		String scriptEngineName = (String) attributes.get( JSR223_SCRIPT_ENGINE_NAME );
		try
		{
			return getScriptEngine( this, scriptEngineName, executable, executionContext );
		}
		catch( LanguageAdapterException x )
		{
			throw new ExecutionException( executable.getDocumentName(), x.getMessage(), x );
		}
	}

	/**
	 * A shared script engine for this language, to be used for compilation.
	 * 
	 * @return The script engine
	 */
	public ScriptEngine getStaticScriptEngine()
	{
		return scriptEngine;
	}

	/**
	 * Though some scripting engines support {@link Compilable}, their
	 * implementation is broken for the purposes of Scripturian. This allows us
	 * to bypass compilation.
	 * 
	 * @return True if compilation is allowed
	 */
	public boolean isCompilable()
	{
		return true;
	}

	/**
	 * Some languages support their own printing facilities, while others don't.
	 * Returning true here will delegate print handling to {@link Executable}.
	 * 
	 * @return True if should print on eval
	 */
	public boolean isPrintOnEval()
	{
		return false;
	}

	/**
	 * Lets implementations do special work before each call.
	 * 
	 * @param scriptEngine
	 *        The script engine
	 * @param executionContext
	 *        The execution context
	 */
	public void beforeCall( ScriptEngine scriptEngine, ExecutionContext executionContext )
	{
	}

	/**
	 * Lets implementations do special work before each call.
	 * 
	 * @param scriptEngine
	 *        The script engine
	 * @param executionContext
	 *        The execution context
	 */
	public void afterCall( ScriptEngine scriptEngine, ExecutionContext executionContext )
	{
	}

	/**
	 * The header is inserted at the beginning of every script. It is useful for
	 * appropriately setting up the script's environment.
	 * 
	 * @param executable
	 *        The composite script instance
	 * @param scriptEngine
	 *        The script engine
	 * @return The header or null
	 */
	public String getScriptletHeader( Executable executable, ScriptEngine scriptEngine )
	{
		return null;
	}

	/**
	 * The footer is appended to the end of every script. It is useful for
	 * cleaning up resources created in the header.
	 * 
	 * @param executable
	 *        The composite script instance
	 * @param scriptEngine
	 *        The script engine
	 * @return The footer or null
	 * @see #getScriptletHeader(Executable, ScriptEngine)
	 */
	public String getScriptletFooter( Executable executable, ScriptEngine scriptEngine )
	{
		return null;
	}

	/**
	 * Turns text into a command or series of commands to print the text to
	 * standard output. Note that the text is of arbitrary length, can span
	 * multiple lines, and include arbitrary characters. The parsing helper
	 * makes sure to escape special characters, partition long text, etc.
	 * 
	 * @param executable
	 *        The composite script instance
	 * @param scriptEngine
	 *        The script engine
	 * @param text
	 *        The text
	 * @return A command or series of commands to print the text
	 */
	public String getSourceCodeForLiteralOutput( Executable executable, ScriptEngine scriptEngine, String text )
	{
		return null;
	}

	/**
	 * Turns an expression into a command or series of commands to print the
	 * evaluation of the expression to standard output. In most cases this
	 * simply involves wrapping the expression in something like a print
	 * command.
	 * 
	 * @param executable
	 *        The composite script instance
	 * @param scriptEngine
	 *        The script engine
	 * @param text
	 *        The content
	 * @return A command or series of commands to print the expression
	 */
	public String getSourceCodeForExpressionOutput( Executable executable, ScriptEngine scriptEngine, String text )
	{
		return null;
	}

	/**
	 * Turns an expression into a command or series of commands to include the
	 * script named for the result of the evaluation of the expression. Note
	 * that this requires the script to have access to a global method named
	 * <code>documentnt.container.includeDocument</code>.
	 * 
	 * @param executable
	 *        The composite script instance
	 * @param scriptEngine
	 *        The script engine
	 * @param text
	 *        The content
	 * @return A command or series of commands to include the script named for
	 *         the expression
	 * @see Executable#getExposedExecutableName()
	 */
	public String getSourceCodeForExpressionInclude( Executable executable, ScriptEngine scriptEngine, String text )
	{
		return null;
	}

	/**
	 * Creates a command or series of commands to invoke an entry point
	 * (function, method, closure, etc.) in the script. Note that for engines
	 * that implement {@link Invocable} you must return null.
	 * 
	 * @param executable
	 *        The composite script instance
	 * @param scriptEngine
	 *        The script engine
	 * @param content
	 *        The content
	 * @param arguments
	 *        The arguments
	 * @return A command or series of commands to call the entry point, or null
	 *         to signify that {@link Invocable} should be used
	 */
	public String getInvocationAsProgram( Executable executable, ScriptEngine scriptEngine, String content, Object... arguments )
	{
		return null;
	}

	/**
	 * Lets implementations do their own exception processing.
	 * 
	 * @param documentName
	 *        The document name
	 * @param throwable
	 *        The throwable
	 * @return The processes throwable
	 */
	public Throwable getCauseOrExecutionException( String documentName, Throwable throwable )
	{
		return null;
	}

	//
	// LanguageAdapter
	//

	public ConcurrentMap<String, Object> getAttributes()
	{
		return attributes;
	}

	public boolean isThreadSafe()
	{
		return true;
	}

	public Lock getLock()
	{
		return lock;
	}

	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		return getSourceCodeForLiteralOutput( executable, scriptEngine, literal );
	}

	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return getSourceCodeForExpressionOutput( executable, scriptEngine, expression );
	}

	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		return getSourceCodeForExpressionInclude( executable, scriptEngine, expression );
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		// Add header
		String header = getScriptletHeader( executable, scriptEngine );
		if( header != null )
			sourceCode = header + sourceCode;

		// Add footer
		String footer = getScriptletFooter( executable, scriptEngine );
		if( footer != null )
			sourceCode += footer;

		return new Jsr223Scriptlet( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		ScriptEngine scriptEngine = getScriptEngine( executable, executionContext );
		scriptEngine.setContext( getScriptContext( executionContext ) );

		String code = getInvocationAsProgram( executable, scriptEngine, entryPointName, arguments );
		if( code == null )
		{
			if( scriptEngine instanceof Invocable )
			{
				try
				{
					beforeCall( scriptEngine, executionContext );
					return ( (Invocable) scriptEngine ).invokeFunction( entryPointName, arguments );
				}
				catch( ScriptException x )
				{
					throw createExecutionException( executable.getDocumentName(), executionContext.getManager(), x );
				}
				catch( NoSuchMethodException x )
				{
					throw x;
				}
				catch( Exception x )
				{
					// Some script engines throw their own special exceptions
					throw createExecutionException( executable.getDocumentName(), executionContext.getManager(), x );
				}
				finally
				{
					afterCall( scriptEngine, executionContext );
				}
			}
			else
			{
				String scriptEngineName = (String) attributes.get( JSR223_SCRIPT_ENGINE_NAME );
				throw new ExecutionException( executable.getDocumentName(), "Script engine " + scriptEngineName + " does not support invocations" );
			}
		}
		else
		{
			try
			{
				beforeCall( scriptEngine, executionContext );
				return scriptEngine.eval( code );
			}
			catch( ScriptException x )
			{
				throw createExecutionException( executable.getDocumentName(), executionContext.getManager(), x );
			}
			catch( Exception x )
			{
				// Some script engines (notably Quercus) throw their
				// own special exceptions
				throw createExecutionException( executable.getDocumentName(), executionContext.getManager(), x );
			}
			finally
			{
				afterCall( scriptEngine, executionContext );
			}
		}
	}

	public void releaseContext( ExecutionContext executionContext )
	{
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The attributes.
	 */
	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	/**
	 * The lock.
	 */
	private final Lock lock = new ReentrantLock();

	/**
	 * The shared script engine for compilation.
	 */
	private final ScriptEngine scriptEngine;
}
