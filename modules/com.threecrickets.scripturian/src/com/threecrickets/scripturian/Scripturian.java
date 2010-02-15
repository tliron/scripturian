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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.threecrickets.scripturian.annotation.ScriptEnginePriorityExtensions;
import com.threecrickets.scripturian.annotation.ScriptEngines;
import com.threecrickets.scripturian.exception.DocumentInitializationException;
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

	public static ScriptletHelper getScriptletHelper( String scriptEngineName, String documentName ) throws DocumentInitializationException
	{
		ScriptletHelper scriptletHelper = Scripturian.scriptletHelpers.get( scriptEngineName );

		if( scriptletHelper == null )
			throw DocumentInitializationException.scriptletHelperNotFound( documentName, scriptEngineName );

		return scriptletHelper;
	}
	
	public static Collection<ScriptletHelper> getScriptletHelpers()
	{
		return scriptletHelpers.values();
	}

	public static String getScriptEngineNameByExtension( String name, String def, ScriptEngineManager scriptEngineManager ) throws DocumentInitializationException
	{
		int slash = name.lastIndexOf( '/' );
		if( slash != -1 )
			name = name.substring( slash + 1 );

		int dot = name.lastIndexOf( '.' );
		String extension = dot != -1 ? name.substring( dot + 1 ) : def;
		if( extension == null )
			throw new DocumentInitializationException( name, "Name must have an extension" );

		// Try our priority mappings first
		String engineName = scriptEngineExtensionPriorities.get( extension );

		if( engineName == null )
		{
			// Try script engine factory's mappings
			ScriptEngine scriptEngine = scriptEngineManager.getEngineByExtension( extension );
			if( scriptEngine == null )
			{
				throw new DocumentInitializationException( name, "Name's extension is not recognized by any script engine: " + extension );
			}
			try
			{
				return scriptEngine.getFactory().getNames().get( 0 );
			}
			catch( IndexOutOfBoundsException x )
			{
				throw new DocumentInitializationException( name, "Script engine has no names: " + scriptEngine );
			}
		}
		else
			return engineName;
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
	private static final ConcurrentMap<String, ScriptletHelper> scriptletHelpers = new ConcurrentHashMap<String, ScriptletHelper>();

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
	private static final ConcurrentMap<String, String> scriptEngineExtensionPriorities = new ConcurrentHashMap<String, String>();

	static
	{
		// Initialize scriptletHelpers
		ServiceLoader<ScriptletHelper> scriptletHelperLoader = ServiceLoader.load( ScriptletHelper.class );
		for( ScriptletHelper scriptletHelper : scriptletHelperLoader )
		{
			try
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
			catch( Throwable x )
			{
				throw new RuntimeException( "Could not initialize " + scriptletHelper, x );
			}
		}
	}

	private Scripturian()
	{
	}
}
