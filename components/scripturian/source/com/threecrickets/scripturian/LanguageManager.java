/**
 * Copyright 2009-2013 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
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
	public static final String CONTAINER_INCLUDE_EXPRESSION_COMMAND = "scripturian.containerIncludeExpressionCommand";

	/**
	 * The attribute prefix for adapter priorities.
	 */
	public static final String ADAPTER_PRIORITY = "scripturian.priority.";

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
	 * resource using the current thread's context class loader.
	 * 
	 * @see ServiceLoader
	 */
	public LanguageManager()
	{
		this( Thread.currentThread().getContextClassLoader() );
	}

	/**
	 * Adds all language adapters found in the
	 * {@code META-INF/services/com.threecrickets.scripturian.LanguageAdapter}
	 * resource.
	 * 
	 * @param classLoader
	 *        The class loader
	 * @see ServiceLoader
	 */
	public LanguageManager( ClassLoader classLoader )
	{
		attributes.put( CONTAINER_INCLUDE_EXPRESSION_COMMAND, DEFAULT_CONTAINER_INCLUDE_EXPRESSION_COMMAND );

		// Initialize adapters
		ServiceLoader<LanguageAdapter> adapterLoader = ServiceLoader.load( LanguageAdapter.class, classLoader );
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
		return Collections.unmodifiableSet( new HashSet<LanguageAdapter>( adapters.values() ) );
	}

	/**
	 * Gets an adapter by its name.
	 * 
	 * @param name
	 *        The name
	 * @return The language adapter
	 * @throws ParsingException
	 * @see LanguageAdapter#NAME
	 */
	public LanguageAdapter getAdapterByName( String name ) throws ParsingException
	{
		return adapters.get( name );
	}

	/**
	 * A language adapter for a scriptlet tag.
	 * 
	 * @param tag
	 *        The scriptlet tag
	 * @return The language adapter or null if not found
	 * @throws ParsingException
	 */
	public LanguageAdapter getAdapterByTag( String tag ) throws ParsingException
	{
		if( tag == null )
			return null;

		Set<LanguageAdapter> adaptersForTag = adaptersByTag.get( tag );
		if( adaptersForTag == null )
			return null;

		adaptersForTag = new HashSet<LanguageAdapter>( adaptersForTag );
		if( adaptersForTag.isEmpty() )
			return null;
		else if( adaptersForTag.size() == 1 )
			return adaptersForTag.iterator().next();
		else
			return getHighestPriorityAdapter( adaptersForTag );
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
			throw new ParsingException( documentName, "Document name must have an extension" );

		Set<LanguageAdapter> adaptersForExtension = adaptersByExtension.get( extension );
		if( adaptersForExtension == null )
			return null;

		adaptersForExtension = new HashSet<LanguageAdapter>( adaptersForExtension );
		if( adaptersForExtension.isEmpty() )
			return null;
		else if( adaptersForExtension.size() == 1 )
			return adaptersForExtension.iterator().next();
		else
			return getHighestPriorityAdapter( adaptersForExtension );
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
	public void addAdapter( LanguageAdapter adapter )
	{
		if( adapter.getManager() != null )
			throw new RuntimeException( "Can't add language adapter instance to more than one language manager" );

		adapters.put( (String) adapter.getAttributes().get( LanguageAdapter.NAME ), adapter );

		@SuppressWarnings("unchecked")
		Iterable<String> tags = (Iterable<String>) adapter.getAttributes().get( LanguageAdapter.TAGS );
		for( String tag : tags )
		{
			Set<LanguageAdapter> adaptersForTag = adaptersByTag.get( tag );
			if( adaptersForTag == null )
			{
				adaptersForTag = new CopyOnWriteArraySet<LanguageAdapter>();
				Set<LanguageAdapter> existing = adaptersByTag.putIfAbsent( tag, adaptersForTag );
				if( existing != null )
					adaptersForTag = existing;
			}
			adaptersForTag.add( adapter );
		}

		@SuppressWarnings("unchecked")
		Iterable<String> extensions = (Iterable<String>) adapter.getAttributes().get( LanguageAdapter.EXTENSIONS );
		for( String extension : extensions )
		{
			Set<LanguageAdapter> adaptersForExtension = adaptersByExtension.get( extension );
			if( adaptersForExtension == null )
			{
				adaptersForExtension = new CopyOnWriteArraySet<LanguageAdapter>();
				Set<LanguageAdapter> existing = adaptersByExtension.putIfAbsent( extension, adaptersForExtension );
				if( existing != null )
					adaptersForExtension = existing;
			}
			adaptersForExtension.add( adapter );
		}

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
	private final ConcurrentMap<String, LanguageAdapter> adapters = new ConcurrentHashMap<String, LanguageAdapter>();

	/**
	 * A map of language tags to their {@link LanguageAdapter} instances.
	 */
	private final ConcurrentMap<String, Set<LanguageAdapter>> adaptersByTag = new ConcurrentHashMap<String, Set<LanguageAdapter>>();

	/**
	 * A map of filename extensions to their {@link LanguageAdapter} instances.
	 */
	private final ConcurrentMap<String, Set<LanguageAdapter>> adaptersByExtension = new ConcurrentHashMap<String, Set<LanguageAdapter>>();

	/**
	 * Finds the highest priority adapter in a set.
	 * <p>
	 * The last adapter used in the current execution context always has the
	 * highest priority.
	 * 
	 * @param adapters
	 *        The adapters
	 * @return The highest priority adapter
	 */
	private LanguageAdapter getHighestPriorityAdapter( Set<LanguageAdapter> adapters )
	{
		ExecutionContext executionContext = ExecutionContext.getCurrent();
		if( executionContext != null )
		{
			LanguageAdapter lastAdapter = executionContext.getAdapter();
			if( adapters.contains( lastAdapter ) )
				return lastAdapter;
		}

		int highestPriority = Integer.MIN_VALUE;
		LanguageAdapter highestPriorityAdapter = null;

		for( LanguageAdapter adapter : adapters )
		{
			String attribute = ADAPTER_PRIORITY + adapter.getAttributes().get( LanguageAdapter.NAME );

			int priority = 0;
			Object priorityObject = attributes.get( attribute );
			if( priorityObject instanceof Number )
				priority = ( (Number) priorityObject ).intValue();
			else if( priorityObject != null )
			{
				try
				{
					priority = Integer.parseInt( priorityObject.toString() );
					attributes.put( attribute, priority );
				}
				catch( NumberFormatException x )
				{
				}
			}

			if( priority > highestPriority )
			{
				highestPriority = priority;
				highestPriorityAdapter = adapter;
			}
		}

		return highestPriorityAdapter;
	}
}
