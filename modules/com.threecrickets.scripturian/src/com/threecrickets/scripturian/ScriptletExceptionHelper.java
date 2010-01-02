/**
 * Copyright 2009 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://www.threecrickets.com/
 */

package com.threecrickets.scripturian;

import com.threecrickets.scripturian.exception.DocumentRunException;

/**
 * @author Tal Liron
 */
public interface ScriptletExceptionHelper
{
	/**
	 * @param documentName
	 *        The document name
	 * @param exception
	 *        The exception to process
	 * @return A document run exception or null
	 */
	public DocumentRunException getDocumentRunException( String documentName, Exception exception );
}
