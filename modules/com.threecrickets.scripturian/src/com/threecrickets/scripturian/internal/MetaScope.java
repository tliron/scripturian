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
 * Used as global space for sharing state between executables. Metas will stay
 * alive for the duration of the Java virtual machine. Any object can be stored
 * here.
 * 
 * @author Tal Liron
 */
public final class MetaScope
{
	//
	// Static attributes
	//

	/**
	 * The meta scope.
	 * 
	 * @return The meta scope
	 */
	public static MetaScope getInstance()
	{
		return instance;
	}

	/**
	 * Any object can be stored here.
	 */
	public ConcurrentMap<String, Object> getValues()
	{
		return values;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The meta scope singleton.
	 */
	private final static MetaScope instance = new MetaScope();

	/**
	 * The values.
	 */
	private final ConcurrentMap<String, Object> values = new ConcurrentHashMap<String, Object>();
}
