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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Used as global space for sharing state between executables. Globals will stay
 * alive for the duration of the Java virtual machine. Any object can be stored
 * here.
 * 
 * @author Tal Liron
 */
public final class GlobalScope
{
	//
	// Static attributes
	//

	/**
	 * The meta scope.
	 * 
	 * @return The meta scope
	 */
	public static GlobalScope getInstance()
	{
		return instance;
	}

	/**
	 * Any object can be stored here.
	 */
	public ConcurrentMap<String, Object> getAttributes()
	{
		return attributes;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The global scope singleton.
	 */
	private final static GlobalScope instance = new GlobalScope();

	/**
	 * The attributes.
	 */
	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();
}
