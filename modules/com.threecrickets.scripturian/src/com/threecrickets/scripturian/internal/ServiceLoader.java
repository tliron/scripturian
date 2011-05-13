/**
 * Copyright 2009-2011 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * This is a clean-room implementation of the similarly named <a
 * href="http://java.sun.com/javase/6/docs/api/java/util/ServiceLoader.html"
 * >Java 6 class</a> in order to allow us an easy upgrade path from Java 5.
 * 
 * @author Tal Liron
 * @param <S>
 */
public class ServiceLoader<S> implements Iterable<S>
{
	//
	// Static operations
	//

	/**
	 * Creates a service loader for a service using the system class loader.
	 * 
	 * @param <S>
	 *        The service class
	 * @param service
	 *        The service
	 * @return The service loader
	 */
	public static <S> ServiceLoader<S> load( Class<S> service )
	{
		return load( service, ClassLoader.getSystemClassLoader() );
	}

	/**
	 * Creates a service loader for a service.
	 * 
	 * @param <S>
	 *        The service class
	 * @param service
	 *        The service
	 * @param loader
	 *        The class loader
	 * @return The service loader
	 */
	public static <S> ServiceLoader<S> load( Class<S> service, ClassLoader loader )
	{
		return new ServiceLoader<S>( service, loader );
	}

	//
	// Iterable
	//

	public Iterator<S> iterator()
	{
		return services.iterator();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The service instances.
	 */
	private ArrayList<S> services = new ArrayList<S>();

	/**
	 * Constructor.
	 * 
	 * @param service
	 *        The service class
	 * @param loader
	 *        The class loader
	 */
	private ServiceLoader( Class<S> service, ClassLoader loader )
	{
		String resourceName = "META-INF/services/" + service.getCanonicalName();
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
						try
						{
							@SuppressWarnings("unchecked")
							S instance = (S) loader.loadClass( line ).newInstance();
							services.add( instance );
						}
						catch( InstantiationException x )
						{
						}
						catch( IllegalAccessException x )
						{
						}
						catch( ClassNotFoundException x )
						{
						}
						catch( Throwable x )
						{
						}
					}
					line = reader.readLine();
				}
				stream.close();
				reader.close();
			}
		}
		catch( IOException x )
		{
		}
	}
}
