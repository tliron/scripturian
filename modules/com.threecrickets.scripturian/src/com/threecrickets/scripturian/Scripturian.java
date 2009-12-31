/**
 * Copyright 2009 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://www.threecrickets.com/
 */

package com.threecrickets.scripturian;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Shared static attributes for Scripturian.
 * 
 * @author Tal Liron
 */
public class Scripturian
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
	 * Map of extensions named to script engine names. For Scripturian, these
	 * mappings supplement and override those extensions declared by individual
	 * script engnies.
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
		// Initialize scriptletParsingHelpers (look for them in META-INF)

		// For Java 6

		/*
		 * ServiceLoader<ScriptletParsingHelper> serviceLoader =
		 * ServiceLoader.load( ScriptletParsingHelper.class ); for(
		 * ScriptletParsingHelper scriptletParsingHelper : serviceLoader ) {
		 * ScriptEngines scriptEngines =
		 * scriptletParsingHelper.getClass().getAnnotation( ScriptEngines.class
		 * ); if( scriptEngines != null ) for( String scriptEngine :
		 * scriptEngines.value() ) scriptletParsingHelpers.put( scriptEngine,
		 * scriptletParsingHelper ); }
		 */

		// For Java 5
		String resourceName = "META-INF/services/" + ScriptletParsingHelper.class.getCanonicalName();
		try
		{
			Enumeration<URL> resources = ClassLoader.getSystemResources( resourceName );
			while( resources.hasMoreElements() )
			{
				InputStream stream = resources.nextElement().openStream();
				BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) );
				String line = reader.readLine();
				while( line != null )
				{
					line = line.trim();
					if( ( line.length() > 0 ) && !line.startsWith( "#" ) )
					{
						ScriptletParsingHelper scriptletParsingHelper = (ScriptletParsingHelper) Class.forName( line ).newInstance();

						ScriptEngines scriptEngines = scriptletParsingHelper.getClass().getAnnotation( ScriptEngines.class );
						if( scriptEngines != null )
						{
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
						else
							throw new RuntimeException( "ScriptletParsingHelper \"" + line + "\" does not have a ScriptEngines annotation" );
					}
					line = reader.readLine();
				}
				stream.close();
				reader.close();
			}
		}
		catch( IOException x )
		{
			throw new RuntimeException( x );
		}
		catch( InstantiationException x )
		{
			throw new RuntimeException( x );
		}
		catch( IllegalAccessException x )
		{
			throw new RuntimeException( x );
		}
		catch( ClassNotFoundException x )
		{
			throw new RuntimeException( x );
		}
	}
}
