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

package com.threecrickets.scripturian.adapter;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * Common implementation base for language adapters.
 * 
 * @author Tal Liron
 */
public abstract class LanguageAdapterBase implements LanguageAdapter
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param name
	 *        The name of the language adapter implementation
	 * @param version
	 *        The version of the language adapter implementation
	 * @param languageName
	 *        The name of the implemented language
	 * @param languageVersion
	 *        The version of the implemented language
	 * @param extensions
	 *        Standard source code filename extensions
	 * @param defaultExtension
	 *        Default source code filename extension
	 * @param tags
	 *        Language tags supported for scriptlets
	 * @param defaultTag
	 *        Default language tag used for scriptlets
	 * @throws LanguageAdapterException
	 */
	public LanguageAdapterBase( String name, String version, String languageName, String languageVersion, Collection<String> extensions, String defaultExtension, Collection<String> tags, String defaultTag )
		throws LanguageAdapterException
	{
		attributes.put( NAME, "Scripturian/" + name );
		attributes.put( VERSION, version );
		attributes.put( LANGUAGE_NAME, languageName != null ? languageName : name );
		attributes.put( LANGUAGE_VERSION, languageVersion != null ? languageVersion : version );
		attributes.put( EXTENSIONS, extensions );
		attributes.put( DEFAULT_EXTENSION, defaultExtension != null ? defaultExtension : extensions.iterator().next() );
		attributes.put( TAGS, tags );
		attributes.put( DEFAULT_TAG, defaultTag != null ? defaultTag : tags.iterator().next() );
	}

	//
	// LanguageAdapter
	//

	public LanguageManager getManager()
	{
		return manager;
	}

	public void setManager( LanguageManager manager )
	{
		this.manager = manager;
	}

	public Map<String, Object> getAttributes()
	{
		return attributes;
	}

	public boolean isThreadSafe()
	{
		return true;
	}

	public Lock getLock()
	{
		return lock;
	}

	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		throw new UnsupportedOperationException();
	}

	public void releaseContext( ExecutionContext executionContext )
	{
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The attributes.
	 */
	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	/**
	 * The lock.
	 */
	private final ReentrantLock lock = new ReentrantLock();

	/**
	 * The language manager.
	 */
	private volatile LanguageManager manager;
}
