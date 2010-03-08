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

package com.threecrickets.scripturian;

import com.threecrickets.scripturian.exception.DocumentRunException;

/**
 * Used in order to add specialized initialization and finalization for
 * scriptlets. For example, to add extra global variables or to perform cleanup
 * of resources.
 * 
 * @author Tal Liron
 */
public interface ScriptletController
{
	/**
	 * Called when a scriptlet is initialized.
	 * 
	 * @param documentContext
	 *        The document context
	 * @throws DocumentRunException
	 *         If you throw an exception here, the scriptlet will not run
	 */
	public void initialize( DocumentContext documentContext ) throws DocumentRunException;

	/**
	 * Called when a scriptlet finalizes. This is a good place to clean up
	 * resources you set up during {@link #initialize(DocumentContext)}.
	 * 
	 * @param documentContext
	 *        The document context
	 */
	public void finalize( DocumentContext documentContext );
}
