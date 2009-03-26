package com.threecrickets.scripturian.internal;

import java.util.concurrent.ConcurrentMap;

import javax.script.ScriptEngine;

import com.threecrickets.scripturian.EmbeddedScript;

/**
 * This is the type of the "script" variable exposed to the script. The name is
 * set according to {@link EmbeddedScript#getScriptVariableName()}.
 * 
 * @author Tal Liron
 * @see EmbeddedScript
 */
public class EmbeddedScriptScript
{
	//
	// Construction
	//

	public EmbeddedScriptScript( EmbeddedScript embeddedScript, ScriptEngine scriptEngine )
	{
		this.embeddedScript = embeddedScript;
		this.scriptEngine = scriptEngine;
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
	 * @see EmbeddedScript#cacheDuration
	 */
	public long getCacheDuration()
	{
		return embeddedScript.getCacheDuration();
	}

	/**
	 * @param cacheDuration
	 *        The cache duration in milliseconds
	 * @see #getCacheDuration()
	 */
	public void setCacheDuration( long cacheDuration )
	{
		embeddedScript.setCacheDuration( cacheDuration );
	}

	/**
	 * The source of the script.
	 * 
	 * @return The script source
	 */
	public Object getSource()
	{
		return embeddedScript.getSource();
	}

	/**
	 * This is the {@link ScriptEngine} used by the script. Scripts may use it
	 * to get information about the engine's capabilities.
	 * 
	 * @return The script engine
	 */
	public ScriptEngine getScriptEngine()
	{
		return scriptEngine;
	}

	/**
	 * This {@link ConcurrentMap} provides a convenient location for global
	 * values shared by all scripts, run by all engines.
	 * 
	 * @return The values
	 */
	public ConcurrentMap<String, Object> getStaticScope()
	{
		return StaticScope.getInstance().getValues();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final EmbeddedScript embeddedScript;

	private final ScriptEngine scriptEngine;

}