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

	public static <S> ServiceLoader<S> load( Class<S> service )
	{
		return load( service, ClassLoader.getSystemClassLoader() );
	}

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

	private ArrayList<S> services = new ArrayList<S>();

	@SuppressWarnings("unchecked")
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
						services.add( (S) loader.loadClass( line ).newInstance() );
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
