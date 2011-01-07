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

package com.threecrickets.scripturian;

import com.threecrickets.scripturian.exception.ExecutionException;

/**
 * Used in order to add custom initialization and finalization for executables.
 * For example, to expose variables or to perform cleanup of resources.
 * 
 * @author Tal Liron
 * @see Executable
 * @see ExecutionContext
 */
public interface ExecutionController
{
	/**
	 * Called before an executable is executed.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @throws ExecutionException
	 *         If you throw an exception here the executable will not run
	 */
	public void initialize( ExecutionContext executionContext ) throws ExecutionException;

	/**
	 * Called after an executable is executed. This is a good place to clean up
	 * resources you set up during {@link #initialize(ExecutionContext)}.
	 * 
	 * @param executionContext
	 *        The execution context
	 */
	public void release( ExecutionContext executionContext );
}
