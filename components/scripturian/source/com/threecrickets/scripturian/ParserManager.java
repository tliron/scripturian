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

package com.threecrickets.scripturian;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.threecrickets.scripturian.internal.ServiceLoader;

/**
 * Provides access to {@link Parser} instances.
 * <p>
 * Instances of this class are safe for concurrent access.
 * 
 * @author Tal Liron
 * @see Parser
 */
public class ParserManager
{
	//
	// Construction
	//

	/**
	 * Adds all parsers found in the
	 * {@code META-INF/services/com.threecrickets.scripturian.Parser} resource.
	 * resource using the current thread's context class loader.
	 * 
	 * @see ServiceLoader
	 */
	public ParserManager()
	{
		this( Thread.currentThread().getContextClassLoader() );
	}

	/**
	 * Adds all parsers found in the
	 * {@code META-INF/services/com.threecrickets.scripturian.Parser} resource.
	 * 
	 * @param classLoader
	 *        The class loader
	 * @see ServiceLoader
	 */
	public ParserManager( ClassLoader classLoader )
	{
		// Initialize parsers
		ServiceLoader<Parser> parsers = ServiceLoader.load( Parser.class, classLoader );
		for( Parser parser : parsers )
		{
			try
			{
				addParser( parser );
			}
			catch( Throwable x )
			{
				throw new RuntimeException( "Could not initialize " + parser, x );
			}
		}
	}

	//
	// Attributes
	//

	public Parser getParser( String name )
	{
		return parsers.get( name );
	}

	//
	// Operations
	//

	public void addParser( Parser parser )
	{
		parsers.put( parser.getName(), parser );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The parsers.
	 */
	private final ConcurrentMap<String, Parser> parsers = new ConcurrentHashMap<String, Parser>();
}
