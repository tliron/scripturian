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
 * This is the <code>executable</code> variable exposed to the executable. The
 * name is set according to {@link Executable#getExposedExecutableName()}.
 * 
 * @author Tal Liron
 * @see Executable
 */
public class ExposedExecutable
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @param container
	 *        The container or null
	 */
	public ExposedExecutable( ExecutionContext executionContext, Object container )
	{
		this.executionContext = executionContext;
		this.container = container;
	}

	//
	// Attributes
	//

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
	 * state shared by all executables.
	 * 
	 * @return The values
	 */
	public ConcurrentMap<String, Object> getMeta()
	{
		return MetaScope.getInstance().getValues();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The execution context.
	 */
	private final ExecutionContext executionContext;

	/**
	 * The container.
	 */
	private final Object container;
}