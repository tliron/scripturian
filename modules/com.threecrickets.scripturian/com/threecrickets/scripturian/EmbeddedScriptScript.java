package com.threecrickets.scripturian;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;

import javax.script.ScriptEngine;

/**
 * This is the type of the "script" variable exposed to the script. The name is
 * set according to {@link EmbeddedScript#scriptVariableName}.
 * 
 * @author Tal Liron
 * @see EmbeddedScript
 */
public class EmbeddedScriptScript
{
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
		return cacheDuration.get();
	}

	/**
	 * @param cacheDuration
	 *        The cache duration in milliseconds
	 * @see #getCacheDuration()
	 */
	public void setCacheDuration( long cacheDuration )
	{
		this.cacheDuration.set( cacheDuration );
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
	 * This {@link Map} provides a convenient location for global values shared
	 * by all scripts, run by all engines. Note that it is not thread safe! In
	 * anything but the most trivial application, you will need to synchronize
	 * access to script.statics. For this reason, script.staticsLock is
	 * provided.
	 * 
	 * @return The statics
	 * @see #getStaticsLock()
	 */
	public Map<String, Object> getStatics()
	{
		return ScriptStatics.statics;
	}

	/**
	 * A {@link ReadWriteLock} meant to be used for the script.statics map,
	 * though exact use is up to your application. It can be used to synchronize
	 * access to the statics across threads. Note that if more locks are needed
	 * for your applications, they can be created and stored as values within
	 * script.statics!
	 * 
	 * @return The statics lock
	 * @see #getStatics()
	 */
	public ReadWriteLock getStaticsLock()
	{
		return ScriptStatics.staticsLock;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	protected EmbeddedScriptScript( AtomicLong cacheDuration, ScriptEngine scriptEngine )
	{
		this.cacheDuration = cacheDuration;
		this.scriptEngine = scriptEngine;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final AtomicLong cacheDuration;

	private final ScriptEngine scriptEngine;

}