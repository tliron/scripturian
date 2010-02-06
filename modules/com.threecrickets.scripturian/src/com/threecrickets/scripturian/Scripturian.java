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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.threecrickets.scripturian.annotation.ScriptEnginePriorityExtensions;
import com.threecrickets.scripturian.annotation.ScriptEngines;
import com.threecrickets.scripturian.internal.ServiceLoader;

/**
 * Shared static attributes for Scripturian.
 * 
 * @author Tal Liron
 */
public abstract class Scripturian
{
	//
	// Static attributes
	//

	/**
	 * A map of script engine names to their {@link ScriptletHelper}. Note that
	 * Scripturian documents will not work without the appropriate parsing
	 * helpers installed.
	 * <p>
	 * This map is automatically initialized when this class loads according to
	 * resources named
	 * <code>META-INF/services/com.threecrickets.scripturian.ScriptletParsingHelper</code>
	 * . Each resource is a simple text file with class names, one per line.
	 * Each class listed must implement the {@link ScriptletHelper} interface
	 * and specify which engine names it supports via the {@link ScriptEngines}
	 * annotation.
	 * <p>
	 * You may also manipulate this map yourself, adding and removing helpers as
	 * necessary.
	 * <p>
	 * The default implementation of this library already contains a few useful
	 * parsing helpers, under the com.threecrickets.scripturian.helper package.
	 */
	public static final ConcurrentMap<String, ScriptletHelper> scriptletHelpers = new ConcurrentHashMap<String, ScriptletHelper>();

	/**
	 * Map of extensions named to script engine names. For Scripturian, these
	 * mappings supplement and override those extensions declared by individual
	 * script engines.
	 * <p>
	 * This map is automatically initialized when this class loads according to
	 * resources named
	 * <code>META-INF/services/com.threecrickets.scripturian.ScriptletParsingHelper</code>
	 * . Each resource is a simple text file with class names, one per line.
	 * Each class listed must implement the {@link ScriptletHelper} interface
	 * and specify which extensions it supports via the
	 * {@link ScriptEnginePriorityExtensions} annotation.
	 * <p>
	 * You may also manipulate this map yourself, adding and removing helpers as
	 * necessary.
	 */
	public static final ConcurrentMap<String, String> scriptEngineExtensionPriorities = new ConcurrentHashMap<String, String>();

	static
	{
		// Initialize scriptletHelpers
		ServiceLoader<ScriptletHelper> scriptletHelperLoader = ServiceLoader.load( ScriptletHelper.class );
		for( ScriptletHelper scriptletHelper : scriptletHelperLoader )
		{
			ScriptEngines scriptEngines = scriptletHelper.getClass().getAnnotation( ScriptEngines.class );
			if( scriptEngines == null )
				throw new RuntimeException( "" + scriptletHelper + " does not have a ScriptEngines annotation" );

			for( String scriptEngine : scriptEngines.value() )
				scriptletHelpers.put( scriptEngine, scriptletHelper );

			ScriptEnginePriorityExtensions scriptEngineExtensions = scriptletHelper.getClass().getAnnotation( ScriptEnginePriorityExtensions.class );
			if( scriptEngineExtensions != null )
			{
				String scriptEngine = scriptEngines.value()[0];
				for( String scriptEngineExtension : scriptEngineExtensions.value() )
					scriptEngineExtensionPriorities.put( scriptEngineExtension, scriptEngine );
			}
		}
	}

	//
	// Main
	//

	public static void main( String[] arguments )
	{
		// Delegate to MainDocument
		MainDocument.main( arguments );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private Scripturian()
	{
	}
}
