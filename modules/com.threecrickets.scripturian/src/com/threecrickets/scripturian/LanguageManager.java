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
import java.util.concurrent.CopyOnWriteArraySet;

import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.internal.ServiceLoader;

/**
 * Provides access to {@link LanguageAdapter} instances.
 * 
 * @author Tal Liron
 * @see LanguageAdapter
 */
public class LanguageManager
{
	//
	// Construction
	//

	@SuppressWarnings("unchecked")
	public LanguageManager()
	{
		// Initialize adapters
		ServiceLoader<LanguageAdapter> adapterLoader = ServiceLoader.load( LanguageAdapter.class );
		for( LanguageAdapter adapter : adapterLoader )
		{
			try
			{
				languageAdapters.add( adapter );

				Iterable<String> tags = (Iterable<String>) adapter.getAttributes().get( LanguageAdapter.TAGS );
				for( String tag : tags )
					languageAdapterByTag.put( tag, adapter );

				Iterable<String> extensions = (Iterable<String>) adapter.getAttributes().get( LanguageAdapter.EXTENSIONS );
				for( String extension : extensions )
					languageAdapterByExtension.put( extension, adapter );
			}
			catch( Throwable x )
			{
				throw new RuntimeException( "Could not initialize " + adapter, x );
			}
		}
	}

	//
	// Attributes
	//

	public ConcurrentMap<String, Object> getAttributes()
	{
		return attributes;
	}

	public Collection<LanguageAdapter> getAdapters()
	{
		return languageAdapters;
	}

	public LanguageAdapter getAdapterByTag( String tag ) throws ParsingException
	{
		if( tag == null )
			return null;
		return languageAdapterByTag.get( tag );
	}

	public LanguageAdapter getAdapterByExtension( String name, String defaultExtension ) throws ParsingException
	{
		int slash = name.lastIndexOf( '/' );
		if( slash != -1 )
			name = name.substring( slash + 1 );

		int dot = name.lastIndexOf( '.' );
		String extension = dot != -1 ? name.substring( dot + 1 ) : defaultExtension;
		if( extension == null )
			throw new ParsingException( name, -1, -1, "Name must have an extension" );

		return languageAdapterByExtension.get( extension );
	}

	public String getLanguageTagByExtension( String name, String defaultExtension, String defaultTag ) throws ParsingException
	{
		LanguageAdapter languageAdapter = getAdapterByExtension( name, defaultExtension );
		if( languageAdapter == null )
			languageAdapter = getAdapterByTag( defaultTag );
		if( languageAdapter != null )
			return (String) languageAdapter.getAttributes().get( LanguageAdapter.DEFAULT_TAG );
		else
			return null;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	private final CopyOnWriteArraySet<LanguageAdapter> languageAdapters = new CopyOnWriteArraySet<LanguageAdapter>();

	/**
	 * A map of script engine names to their {@link LanguageAdapter}. Note that
	 * Scripturian documents will not work without the appropriate adapters
	 * installed.
	 * <p>
	 * This map is automatically initialized when this class loads according to
	 * resources named
	 * <code>META-INF/services/com.threecrickets.scripturian.ScripturianAdapter</code>
	 * . Each resource is a simple text file with class names, one per line.
	 * Each class listed must implement the {@link LanguageAdapter} interface.
	 * <p>
	 * You may also manipulate this map yourself, adding and removing helpers as
	 * necessary.
	 * <p>
	 * The default implementation of this library already contains a few useful
	 * adapter, under the com.threecrickets.scripturian.adapter package.
	 */
	private final ConcurrentMap<String, LanguageAdapter> languageAdapterByTag = new ConcurrentHashMap<String, LanguageAdapter>();

	/**
	 * Map of extensions named to script engine names. For Scripturian, these
	 * mappings supplement and override those extensions declared by individual
	 * script engines.
	 * <p>
	 * This map is automatically initialized when this class loads according to
	 * resources named
	 * <code>META-INF/services/com.threecrickets.scripturian.ScriptletParsingHelper</code>
	 * . Each resource is a simple text file with class names, one per line.
	 * Each class listed must implement the {@link LanguageAdapter} interface.
	 * <p>
	 * You may also manipulate this map yourself, adding and removing helpers as
	 * necessary.
	 */
	private final ConcurrentMap<String, LanguageAdapter> languageAdapterByExtension = new ConcurrentHashMap<String, LanguageAdapter>();
}
