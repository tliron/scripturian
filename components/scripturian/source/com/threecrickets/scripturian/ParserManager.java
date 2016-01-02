/**
 * Copyright 2009-2016 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
		for( Iterator<Parser> i = parsers.iterator(); i.hasNext(); )
		{
			Parser parser = null;
			try
			{
				parser = i.next();
			}
			catch( Throwable x )
			{
				// Probably a ClassNotFoundException
				continue;
			}
			addParser( parser );
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
		if( parsers.containsKey( parser.getName() ) )
			throw new RuntimeException( "Can't add more than one parser with the same name to the same parser manager: " + parser );

		parsers.put( parser.getName(), parser );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The parsers.
	 */
	private final ConcurrentMap<String, Parser> parsers = new ConcurrentHashMap<String, Parser>();
}
