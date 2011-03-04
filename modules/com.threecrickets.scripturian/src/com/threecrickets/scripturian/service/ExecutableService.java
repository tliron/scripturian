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

package com.threecrickets.scripturian.service;

import java.io.Writer;
import java.util.concurrent.ConcurrentMap;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.GlobalScope;
import com.threecrickets.scripturian.LanguageManager;

/**
 * This is the <code>executable</code> service exposed to the executable. The
 * name is set according to {@link Executable#getExecutableServiceName()}.
 * 
 * @author Tal Liron
 * @see Executable
 */
public class ExecutableService
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @param manager
	 *        The language manager used to parse, prepare and execute the
	 *        executable
	 * @param container
	 *        The container or null
	 */
	public ExecutableService( ExecutionContext executionContext, LanguageManager manager, Object container )
	{
		this.executionContext = executionContext;
		this.manager = manager;
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
	 * The language manager used to parse, prepare and execute the executable.
	 * 
	 * @return The language manager
	 */
	public LanguageManager getManager()
	{
		return manager;
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
		ConcurrentMap<String, Object> globals = getGlobals();
		Object value = globals.get( name );
		if( ( value == null ) && ( defaultValue != null ) )
		{
			value = defaultValue;
			Object existing = globals.putIfAbsent( name, value );
			if( existing != null )
				value = existing;
		}
		return value;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The execution context.
	 */
	private final ExecutionContext executionContext;

	/**
	 * The language manager used to parse, prepare and execute the executable.
	 */
	private final LanguageManager manager;

	/**
	 * The container.
	 */
	private final Object container;
}