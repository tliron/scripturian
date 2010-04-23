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
	 * A map of all values global to the current VM.
	 * 
	 * @return The globals
	 */
	public ConcurrentMap<String, Object> getGlobals()
	{
		return GlobalScope.getInstance().getAttributes();
	}

	/**
	 * Gets a value global to the current VM.
	 * 
	 * @param name
	 *        The name of the global
	 * @return The global's current value
	 */
	public Object getGlobal( String name )
	{
		return getGlobal( name, null );
	}

	/**
	 * Gets a value global to the current VM, atomically setting it to a default
	 * value if it doesn't exist.
	 * 
	 * @param name
	 *        The name of the global
	 * @param defaultValue
	 *        The default value
	 * @return The global's current value
	 */
	public Object getGlobal( String name, Object defaultValue )
	{
		ConcurrentMap<String, Object> attributes = GlobalScope.getInstance().getAttributes();
		Object value = attributes.get( name );
		if( ( value == null ) && ( defaultValue != null ) )
		{
			value = defaultValue;
			Object existing = attributes.putIfAbsent( name, value );
			if( existing != null )
				value = existing;
		}
		return value;
	}

	/**
	 * Sets the value global to the current VM.
	 * 
	 * @param name
	 *        The name of the global
	 * @param value
	 *        The global's new value
	 * @return The global's previous value
	 */
	public Object setGlobal( String name, Object value )
	{
		ConcurrentMap<String, Object> attributes = GlobalScope.getInstance().getAttributes();
		return attributes.put( name, value );
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