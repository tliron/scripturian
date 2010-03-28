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

package com.threecrickets.scripturian.internal;

import java.io.Writer;
import java.util.concurrent.ConcurrentMap;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;

/**
 * This is the <code>document</code> variable exposed to scriptlets. The name is
 * set according to {@link Executable#getExecutableVariableName()}.
 * 
 * @author Tal Liron
 * @see Executable
 */
public class ExposedExecutable
{
	//
	// Construction
	//

	public ExposedExecutable( Executable executable, ExecutionContext executionContext, Object container )
	{
		this.executable = executable;
		this.executionContext = executionContext;
		this.container = container;
	}

	//
	// Attributes
	//

	/**
	 * Setting this to something greater than 0 enables caching of the
	 * document's output for a maximum number of milliseconds. By default
	 * cacheDuration is 0, so that each request causes the document to be run.
	 * This class does not handle caching itself. Caching can be provided by
	 * your environment if appropriate.
	 * 
	 * @return The cache duration in milliseconds
	 * @see #setCacheDuration(long)
	 * @see Executable#cacheDuration
	 */
	public long getCacheDuration()
	{
		return executable.getCacheDuration();
	}

	/**
	 * @param cacheDuration
	 *        The cache duration in milliseconds
	 * @see #getCacheDuration()
	 */
	public void setCacheDuration( long cacheDuration )
	{
		executable.setCacheDuration( cacheDuration );
	}

	/**
	 * This is the {@link ExecutionContext} used by the document. Scriptlets may
	 * use it to get access to the {@link Writer} objects used for standard
	 * output and standard error.
	 * 
	 * @return The document context
	 */
	public ExecutionContext getContext()
	{
		return executionContext;
	}

	/**
	 * The container.
	 * 
	 * @return The container (or null if none was provided)
	 */
	public Object getContainer()
	{
		return container;
	}

	/**
	 * This {@link ConcurrentMap} provides a convenient location for global
	 * values shared by all scriptlets in all documents.
	 * 
	 * @return The values
	 */
	public ConcurrentMap<String, Object> getMeta()
	{
		return MetaScope.getInstance().getValues();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final Executable executable;

	private final ExecutionContext executionContext;

	private final Object container;
}