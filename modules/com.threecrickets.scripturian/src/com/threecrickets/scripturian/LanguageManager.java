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

package com.threecrickets.scripturian;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.internal.ServiceLoader;

/**
 * Provides access to {@link LanguageAdapter} instances.
 * <p>
 * Instances of this class are safe for concurrent access.
 * 
 * @author Tal Liron
 * @see LanguageAdapter
 */
public class LanguageManager
{
	//
	// Constants
	//

	/**
	 * The property name for the Scripturian cache path.
	 * 
	 * @see #getCachePath()
	 */
	public static final String SCRIPTURIAN_CACHE_PATH = "scripturian.cache";

	/**
	 * The default Scripturian cache path.
	 * 
	 * @see #getCachePath()
	 */
	public static final String SCRIPTURIAN_CACHE_PATH_DEFAULT = "scripturian/cache";

	/**
	 * The attribute name for the container include expression command.
	 */
	public static final String CONTAINER_INCLUDE_EXPRESSION_COMMAND = "containerIncludeExpressionCommand";

	/**
	 * The default container include expression command.
	 */
	public static final String DEFAULT_CONTAINER_INCLUDE_EXPRESSION_COMMAND = "include";

	//
	// Static attributes
	//

	/**
	 * The base path for language adapter caches.
	 * 
	 * @return The cache path
	 */
	public static File getCachePath()
	{
		File cachePath = LanguageManager.cachePath.get();
		if( cachePath == null )
		{
			// Parse properties
			String cachePathProperty = System.getProperty( SCRIPTURIAN_CACHE_PATH );
			cachePath = new File( cachePathProperty != null ? cachePathProperty : SCRIPTURIAN_CACHE_PATH_DEFAULT );
			if( !LanguageManager.cachePath.compareAndSet( null, cachePath ) )
				cachePath = LanguageManager.cachePath.get();
		}
		return cachePath;
	}

	//
	// Construction
	//

	/**
	 * Adds all language adapters found in the
	 * {@code META-INF/services/com.threecrickets.scripturian.LanguageAdapter}
	 * resource.
	 * 
	 * @see ServiceLoader
	 */
	public LanguageManager()
	{
		attributes.put( CONTAINER_INCLUDE_EXPRESSION_COMMAND, DEFAULT_CONTAINER_INCLUDE_EXPRESSION_COMMAND );

		// Initialize adapters
		ServiceLoader<LanguageAdapter> adapterLoader = ServiceLoader.load( LanguageAdapter.class );
		for( LanguageAdapter adapter : adapterLoader )
		{
			try
			{
				addAdapter( adapter );
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

	/**
	 * General-purpose attributes for this language manager. Language adapters
	 * can use this for sharing state with other language adapters.
	 * 
	 * @return The attributes
	 */
	public ConcurrentMap<String, Object> getAttributes()
	{
		return attributes;
	}

	/**
	 * Find only those attributes with this prefix.
	 * 
	 * @return The attributes with the prefix
	 */
	public Properties getAttributesAsProperties( String prefix )
	{
		Properties prefixed = new Properties();

		int prefixLength = prefix.length();
		for( Map.Entry<String, Object> entry : attributes.entrySet() )
			if( entry.getKey().startsWith( prefix ) )
				prefixed.put( entry.getKey().substring( prefixLength ), entry.getValue() );

		return prefixed;
	}

	/**
	 * All language adapters. Note that this set is unmodifiable. To add an
	 * adapter use {@link #addAdapter(LanguageAdapter)}
	 * 
	 * @return The adapters
	 */
	public Set<LanguageAdapter> getAdapters()
	{
		return Collections.unmodifiableSet( languageAdapters );
	}

	/**
	 * A language adapter for a scriptlet tag.
	 * 
	 * @param tag
	 *        The scriptlet adapter
	 * @return The language adapter or null if not found
	 * @throws ParsingException
	 */
	public LanguageAdapter getAdapterByTag( String tag ) throws ParsingException
	{
		if( tag == null )
			return null;
		return languageAdapterByTag.get( tag );
	}

	/**
	 * A language adapter for a document name according to its filename
	 * extension.
	 * 
	 * @param documentName
	 *        The document name
	 * @param defaultExtension
	 *        The default extension to assume in case the document name does not
	 *        have one
	 * @return The language adapter or null if not found
	 * @throws ParsingException
	 */
	public LanguageAdapter getAdapterByExtension( String documentName, String defaultExtension ) throws ParsingException
	{
		int slash = documentName.lastIndexOf( '/' );
		if( slash != -1 )
			documentName = documentName.substring( slash + 1 );

		int dot = documentName.lastIndexOf( '.' );
		String extension = dot != -1 ? documentName.substring( dot + 1 ) : defaultExtension;
		if( extension == null )
			throw new ParsingException( documentName, -1, -1, "Name must have an extension" );

		return languageAdapterByExtension.get( extension );
	}

	/**
	 * A language adapter for a document name according to its filename
	 * extension.
	 * 
	 * @param documentName
	 *        The document name
	 * @param defaultExtension
	 *        The default extension to assume in case the document name does not
	 *        have one
	 * @param defaultTag
	 *        The language tag to use in case a language adapter wasn't found
	 *        according to the extension
	 * @return The default language adapter tag or null if not found
	 * @throws ParsingException
	 */
	public String getLanguageTagByExtension( String documentName, String defaultExtension, String defaultTag ) throws ParsingException
	{
		LanguageAdapter languageAdapter = getAdapterByExtension( documentName, defaultExtension );
		if( languageAdapter == null )
			languageAdapter = getAdapterByTag( defaultTag );
		if( languageAdapter != null )
			return (String) languageAdapter.getAttributes().get( LanguageAdapter.DEFAULT_TAG );
		else
			return null;
	}

	//
	// Operations
	//

	/**
	 * Adds a language adapter to this manager.
	 * 
	 * @param adapter
	 *        The language adapter
	 */
	@SuppressWarnings("unchecked")
	public void addAdapter( LanguageAdapter adapter )
	{
		if( adapter.getManager() != null )
			throw new RuntimeException( "Can't add language adapter instance to more than one language manager" );

		languageAdapters.add( adapter );

		Iterable<String> tags = (Iterable<String>) adapter.getAttributes().get( LanguageAdapter.TAGS );
		for( String tag : tags )
			languageAdapterByTag.putIfAbsent( tag, adapter );

		Iterable<String> extensions = (Iterable<String>) adapter.getAttributes().get( LanguageAdapter.EXTENSIONS );
		for( String extension : extensions )
			languageAdapterByExtension.putIfAbsent( extension, adapter );

		adapter.setManager( this );
	}

	//
	// Object
	//

	@Override
	public String toString()
	{
		return "LanguageManager: " + cachePath.get();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The base path for language adapter caches.
	 */
	private static final AtomicReference<File> cachePath = new AtomicReference<File>();

	/**
	 * General-purpose attributes for this language manager.
	 */
	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	/**
	 * The language adapters.
	 */
	private final CopyOnWriteArraySet<LanguageAdapter> languageAdapters = new CopyOnWriteArraySet<LanguageAdapter>();

	/**
	 * A map of language tags to their {@link LanguageAdapter} instances.
	 */
	private final ConcurrentMap<String, LanguageAdapter> languageAdapterByTag = new ConcurrentHashMap<String, LanguageAdapter>();

	/**
	 * A map of filename extensions to their {@link LanguageAdapter} instances.
	 */
	private final ConcurrentMap<String, LanguageAdapter> languageAdapterByExtension = new ConcurrentHashMap<String, LanguageAdapter>();
}
