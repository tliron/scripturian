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

package com.threecrickets.scripturian;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import com.threecrickets.scripturian.exception.DocumentInitializationException;
import com.threecrickets.scripturian.exception.DocumentRunException;

/**
 * Encapsulates context for an {@link Document}. Every thread calling
 * {@link Document#run(boolean, boolean, Writer, Writer, boolean, DocumentContext, Object, ScriptletController)}
 * must use its own context.
 * 
 * @author Tal Liron
 */
public class DocumentContext
{
	//
	// Construction
	//

	/**
	 * Creates a context for a specific script engine manager.
	 * 
	 * @param scriptEngineManager
	 *        A script engine manager
	 */
	public DocumentContext( ScriptEngineManager scriptEngineManager )
	{
		this.scriptEngineManager = scriptEngineManager;
	}

	//
	// Attributes
	//

	/**
	 * The script engine manager used for {@link #getScriptEngine(String)}.
	 * 
	 * @return The script engine manager
	 */
	public ScriptEngineManager getScriptEngineManager()
	{
		return scriptEngineManager;
	}

	/**
	 * A cached script engine. All script engines will use the same
	 * {@link ScriptContext}.
	 * 
	 * @param scriptEngineName
	 *        The script engine name
	 * @param documentName
	 *        The document name for debugging
	 * @return The cached script engine
	 * @throws DocumentInitializationException
	 */
	public ScriptEngine getScriptEngine( String scriptEngineName, String documentName ) throws DocumentInitializationException
	{
		if( scriptEngines == null )
			scriptEngines = new HashMap<String, ScriptEngine>();

		lastScriptEngine = scriptEngines.get( scriptEngineName );

		if( lastScriptEngine == null )
		{
			lastScriptEngine = scriptEngineManager.getEngineByName( scriptEngineName );
			if( lastScriptEngine == null )
				throw DocumentInitializationException.scriptEngineNotFound( documentName, scriptEngineName );

			// (Note that some script engines do not even
			// provide a default context -- Jepp, for example -- so
			// it's generally a good idea to explicitly set one)
			lastScriptEngine.setContext( getScriptContext() );

			scriptEngines.put( scriptEngineName, lastScriptEngine );
		}

		lastScriptEngineName = scriptEngineName;

		return lastScriptEngine;
	}

	/**
	 * The single script context used by all scriptlets in the document.
	 * 
	 * @return The script context
	 */
	public ScriptContext getScriptContext()
	{
		if( scriptContext == null )
		{
			scriptContext = new SimpleScriptContext();
			scriptContext.setBindings( new SimpleBindings(), ScriptContext.ENGINE_SCOPE );
			scriptContext.setBindings( new SimpleBindings(), ScriptContext.GLOBAL_SCOPE );
		}
		return scriptContext;
	}

	/**
	 * @return
	 */
	public Set<String> getVariableNames()
	{
		Set<String> variables = new HashSet<String>();
		ScriptContext scriptContext = getScriptContext();
		for( Integer scope : scriptContext.getScopes() )
		{
			for( String var : scriptContext.getBindings( scope ).keySet() )
				variables.add( var );
		}
		return variables;
	}

	/**
	 * @param name
	 * @param object
	 * @return
	 */
	public Object setVariable( String name, Object object )
	{
		ScriptContext scriptContext = getScriptContext();
		Object old = scriptContext.getAttribute( name, ScriptContext.ENGINE_SCOPE );
		scriptContext.setAttribute( name, object, ScriptContext.ENGINE_SCOPE );
		return old;
	}

	/**
	 * @return
	 */
	public Writer getWriter()
	{
		ScriptContext scriptContext = getScriptContext();
		return scriptContext.getWriter();
	}

	/**
	 * @param writer
	 * @return
	 */
	public Writer setWriter( Writer writer )
	{
		ScriptContext scriptContext = getScriptContext();
		Writer old = scriptContext.getWriter();
		scriptContext.setWriter( writer );
		return old;
	}

	/**
	 * @param writer
	 * @param flushLines
	 * @return
	 */
	public Writer setWriter( Writer writer, boolean flushLines )
	{
		ScriptContext scriptContext = getScriptContext();
		Writer old = scriptContext.getWriter();

		// Note that some script engines (such as Rhino) expect a
		// PrintWriter, even though the spec defines just a Writer
		writer = new PrintWriter( writer, flushLines );

		scriptContext.setWriter( writer );
		return old;
	}

	/**
	 * @return
	 */
	public Writer getErrorWriter()
	{
		ScriptContext scriptContext = getScriptContext();
		return scriptContext.getErrorWriter();
	}

	/**
	 * @param writer
	 * @return
	 */
	public Writer setErrorWriter( Writer writer )
	{
		ScriptContext scriptContext = getScriptContext();
		Writer old = scriptContext.getErrorWriter();
		scriptContext.setErrorWriter( writer );
		return old;
	}

	/**
	 * @param writer
	 * @param flushLines
	 * @return
	 */
	public Writer setErrorWriter( Writer writer, boolean flushLines )
	{
		ScriptContext scriptContext = getScriptContext();
		Writer old = scriptContext.getErrorWriter();

		// Note that some script engines (such as Rhino) expect a
		// PrintWriter, even though the spec defines just a Writer
		writer = new PrintWriter( writer, flushLines );

		scriptContext.setErrorWriter( writer );
		return old;
	}

	/**
	 * The last {@link ScriptEngine} used in the last run of the document.
	 * 
	 * @return The last script engine
	 */
	public ScriptEngine getLastScriptEngine()
	{
		return lastScriptEngine;
	}

	/**
	 * The name of the {@link ScriptEngine} used in the last run of the
	 * document.
	 * 
	 * @return The last script engine name
	 */
	public String getLastScriptEngineName()
	{
		return lastScriptEngineName;
	}

	/**
	 * @param scriptEngine
	 * @param compiledScript
	 * @param name
	 * @param program
	 * @return
	 * @throws ScriptException
	 */
	public Object call( ScriptEngine scriptEngine, CompiledScript compiledScript, String name, String program ) throws DocumentRunException
	{
		ScriptContext scriptContext = getScriptContext();
		Object value;
		try
		{
			if( compiledScript != null )
				value = compiledScript.eval( scriptContext );
			else
				// Note that we are wrapping our text with a
				// StringReader. Why? Because some
				// implementations of javax.script (notably
				// Jepp) interpret the String version of eval to
				// mean only one line of code.
				value = scriptEngine.eval( new StringReader( program ), scriptContext );
		}
		catch( ScriptException x )
		{
			throw DocumentRunException.create( name, x );
		}
		catch( Exception x )
		{
			// Some script engines (notably Quercus) throw their
			// own special exceptions
			throw DocumentRunException.create( name, x );
		}

		return value;
	}

	/**
	 * @param scriptEngine
	 * @param scriptEngineName
	 * @param entryPointName
	 * @param name
	 * @param program
	 * @return
	 * @throws DocumentRunException
	 * @throws NoSuchMethodException
	 */
	public Object invoke( ScriptEngine scriptEngine, String scriptEngineName, String entryPointName, String name, String program ) throws DocumentRunException, NoSuchMethodException
	{
		if( program == null )
		{
			if( scriptEngine instanceof Invocable )
			{
				try
				{
					return ( (Invocable) scriptEngine ).invokeFunction( entryPointName );
				}
				catch( ScriptException x )
				{
					throw DocumentRunException.create( name, x );
				}
				catch( NoSuchMethodException x )
				{
					throw x;
				}
				catch( Exception x )
				{
					// Some script engines (notably Quercus) throw their
					// own special exceptions
					throw DocumentRunException.create( name, x );
				}
			}
			else
				throw new DocumentRunException( name, "Script engine " + scriptEngineName + " does not support invocations" );
		}
		else
		{
			try
			{
				return scriptEngine.eval( program );
			}
			catch( ScriptException x )
			{
				throw DocumentRunException.create( name, x );
			}
			catch( Exception x )
			{
				// Some script engines (notably Quercus) throw their
				// own special exceptions
				throw DocumentRunException.create( name, x );
			}
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final ScriptEngineManager scriptEngineManager;

	private ScriptContext scriptContext;

	private Map<String, ScriptEngine> scriptEngines;

	private ScriptEngine lastScriptEngine;

	private String lastScriptEngineName;
}
