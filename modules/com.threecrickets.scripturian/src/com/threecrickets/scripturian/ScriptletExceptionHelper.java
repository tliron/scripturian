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

/**
 * @author Tal Liron
 */
public interface ScriptletExceptionHelper
{
	/**
	 * @param documentName
	 *        The document name
	 * @param throwable
	 *        The throwable to process
	 * @return A document run exception, a wrapped cause, or null
	 */
	public Throwable getCauseOrDocumentRunException( String documentName, Throwable throwable );
}
