/**
 * Copyright 2009-2015 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.service;

import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import com.threecrickets.scripturian.GlobalScope;
import com.threecrickets.scripturian.Main;

/**
 * This is the <code>application</code> service exposed by a {@link Shell}.
 * 
 * @author Tal Liron
 * @see Main
 */
public class ApplicationService
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @param shell
	 *        The shell instance
	 */
	public ApplicationService( Shell shell )
	{
		this.shell = shell;
	}

	//
	// Attributes
	//

	/**
	 * An array of the string arguments sent from the shell.
	 * 
	 * @return The arguments
	 */
	public String[] getArguments()
	{
		return shell.getArguments();
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

	/**
	 * The logger.
	 * 
	 * @return The logger
	 * @see #setLogger(Logger)
	 */
	public Logger getLogger()
	{
		return shell.getLogger();
	}

	/**
	 * @param logger
	 *        The logger
	 * @see #getLogger()
	 */
	public void setLogger( Logger logger )
	{
		shell.setLogger( logger );
	}

	/**
	 * A logger with a name appended with a "." to the application's logger
	 * name. This allows inheritance of configuration.
	 * 
	 * @param name
	 *        The sub-logger name
	 * @return The logger
	 * @see #getLogger()
	 */
	public Logger getSubLogger( String name )
	{
		return Logger.getLogger( shell.getLogger().getName() + "." + name );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The shell instance.
	 */
	private final Shell shell;
}
