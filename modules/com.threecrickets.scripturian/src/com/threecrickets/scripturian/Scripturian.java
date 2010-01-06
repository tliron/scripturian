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
import java.util.concurrent.CopyOnWriteArrayList;

import com.threecrickets.scripturian.annotation.ScriptEnginePriorityExtensions;
import com.threecrickets.scripturian.annotation.ScriptEngines;
import com.threecrickets.scripturian.exception.DocumentRunException;
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
	 * A map of script engine names to their {@link ScriptletParsingHelper}.
	 * Note that Scripturian documents will not work without the appropriate
	 * parsing helpers installed.
	 * <p>
	 * This map is automatically initialized when this class loads according to
	 * resources named
	 * <code>META-INF/services/com.threecrickets.scripturian.ScriptletParsingHelper</code>
	 * . Each resource is a simple text file with class names, one per line.
	 * Each class listed must implement the {@link ScriptletParsingHelper}
	 * interface and specify which engine names it supports via the
	 * {@link ScriptEngines} annotation.
	 * <p>
	 * You may also manipulate this map yourself, adding and removing helpers as
	 * necessary.
	 * <p>
	 * The default implementation of this library already contains a few useful
	 * parsing helpers, under the com.threecrickets.scripturian.helper package.
	 */
	public static final ConcurrentMap<String, ScriptletParsingHelper> scriptletParsingHelpers = new ConcurrentHashMap<String, ScriptletParsingHelper>();

	/**
	 * A list of scriptlet exception helpers. The are tried in order when
	 * creating {@link DocumentRunException} instances.
	 * 
	 * @see DocumentRunException
	 */
	public static final CopyOnWriteArrayList<ScriptletExceptionHelper> scriptletExceptionHelpers = new CopyOnWriteArrayList<ScriptletExceptionHelper>();

	/**
	 * Map of extensions named to script engine names. For Scripturian, these
	 * mappings supplement and override those extensions declared by individual
	 * script engines.
	 * <p>
	 * This map is automatically initialized when this class loads according to
	 * resources named
	 * <code>META-INF/services/com.threecrickets.scripturian.ScriptletParsingHelper</code>
	 * . Each resource is a simple text file with class names, one per line.
	 * Each class listed must implement the {@link ScriptletParsingHelper}
	 * interface and specify which extensions it supports via the
	 * {@link ScriptEnginePriorityExtensions} annotation.
	 * <p>
	 * You may also manipulate this map yourself, adding and removing helpers as
	 * necessary.
	 */
	public static final ConcurrentMap<String, String> scriptEngineExtensionPriorities = new ConcurrentHashMap<String, String>();

	static
	{
		// Initialize scriptletParsingHelpers
		ServiceLoader<ScriptletParsingHelper> scriptletParsingHelperLoader = ServiceLoader.load( ScriptletParsingHelper.class );
		for( ScriptletParsingHelper scriptletParsingHelper : scriptletParsingHelperLoader )
		{
			ScriptEngines scriptEngines = scriptletParsingHelper.getClass().getAnnotation( ScriptEngines.class );
			if( scriptEngines == null )
				throw new RuntimeException( "" + scriptletParsingHelper + " does not have a ScriptEngines annotation" );

			for( String scriptEngine : scriptEngines.value() )
				scriptletParsingHelpers.put( scriptEngine, scriptletParsingHelper );

			ScriptEnginePriorityExtensions scriptEngineExtensions = scriptletParsingHelper.getClass().getAnnotation( ScriptEnginePriorityExtensions.class );
			if( scriptEngineExtensions != null )
			{
				String scriptEngine = scriptEngines.value()[0];
				for( String scriptEngineExtension : scriptEngineExtensions.value() )
					scriptEngineExtensionPriorities.put( scriptEngineExtension, scriptEngine );
			}
		}

		// Initialize scriptletExceptionHelpers
		ServiceLoader<ScriptletExceptionHelper> scriptletExceptionHelperLoader = ServiceLoader.load( ScriptletExceptionHelper.class );
		for( ScriptletExceptionHelper scriptletExceptionHelper : scriptletExceptionHelperLoader )
			scriptletExceptionHelpers.add( scriptletExceptionHelper );
	}

	//
	// Main
	//

	public void main( String[] arguments )
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
