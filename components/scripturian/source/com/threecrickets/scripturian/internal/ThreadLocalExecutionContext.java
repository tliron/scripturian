/**
 * Copyright 2009-2012 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.internal;

import com.threecrickets.scripturian.ExecutionContext;

/**
 * Thread-local reference to an {@link ExecutionContext}.
 * 
 * @author Tal Liron
 */
public class ThreadLocalExecutionContext extends ThreadLocal<ExecutionContext>
{
	//
	// Static attributes
	//

	/**
	 * The thread-local execution context.
	 */
	public static final ThreadLocalExecutionContext current = new ThreadLocalExecutionContext();

	//
	// ThreadLocal
	//

	@Override
	protected ExecutionContext initialValue()
	{
		return null;
	}
}