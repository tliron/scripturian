package com.threecrickets.scripturian.internal;

import java.io.Writer;
import java.util.concurrent.ConcurrentMap;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.threecrickets.scripturian.CompositeScript;
import com.threecrickets.scripturian.CompositeScriptContext;

/**
 * This is the <code>script</code> variable exposed to the script. The name is
 * set according to {@link CompositeScript#getScriptVariableName()}.
 * 
 * @author Tal Liron
 * @see CompositeScript
 */
public class ExposedScript
{
	//
	// Construction
	//

	public ExposedScript( CompositeScript compositeScript, CompositeScriptContext compositeScriptContext, ScriptEngine scriptEngine, Object container )
	{
		this.compositeScript = compositeScript;
		this.compositeScriptContext = compositeScriptContext;
		this.scriptEngine = scriptEngine;
		this.container = container;
	}

	//
	// Attributes
	//

	/**
	 * Setting this to something greater than 0 enables caching of the script
	 * results for a maximum number of milliseconds. By default cacheDuration is
	 * 0, so that each request causes the script to be evaluated. This class
	 * does not handle caching itself. Caching can be provided by your
	 * environment if appropriate.
	 * 
	 * @return The cache duration in milliseconds
	 * @see #setCacheDuration(long)
	 * @see CompositeScript#cacheDuration
	 */
	public long getCacheDuration()
	{
		return compositeScript.getCacheDuration();
	}

	/**
	 * @param cacheDuration
	 *        The cache duration in milliseconds
	 * @see #getCacheDuration()
	 */
	public void setCacheDuration( long cacheDuration )
	{
		compositeScript.setCacheDuration( cacheDuration );
	}

	/**
	 * This is the {@link ScriptContext} used by the script. Scripts may use it
	 * to get access to the {@link Writer} objects used for standard output and
	 * standard error.
	 * 
	 * @return The script context
	 */
	public ScriptContext getContext()
	{
		return compositeScriptContext.getScriptContext();
	}

	/**
	 * This is the {@link ScriptEngine} used by the script. Scripts may use it
	 * to get information about the engine's capabilities.
	 * 
	 * @return The script engine
	 */
	public ScriptEngine getEngine()
	{
		return scriptEngine;
	}

	/**
	 * This is the {@link ScriptEngineManager} used to create the script engine.
	 * Scripts may use it to get information about what other engines are
	 * available.
	 * 
	 * @return The script engine manager
	 */
	public ScriptEngineManager getEngineManager()
	{
		return compositeScriptContext.getScriptEngineManager();
	}

	/**
	 * The container.
	 * 
	 * @return The container (or null if none was provided)
	 */
	public Object getContainer()
	{
		return container;
	}

	/**
	 * This {@link ConcurrentMap} provides a convenient location for global
	 * values shared by all scripts, run by all engines.
	 * 
	 * @return The values
	 */
	public ConcurrentMap<String, Object> getMeta()
	{
		return MetaScope.getInstance().getValues();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final CompositeScript compositeScript;

	private final CompositeScriptContext compositeScriptContext;

	private final ScriptEngine scriptEngine;

	private final Object container;
}