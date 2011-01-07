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

package com.threecrickets.scripturian.exception;

/**
 * The document was not found.
 * 
 * @author Tal Liron
 */
public class DocumentNotFoundException extends DocumentException
{
	//
	// Construction
	//

	public DocumentNotFoundException( String message )
	{
		super( message );
	}

	public DocumentNotFoundException( String message, Throwable cause )
	{
		super( message, cause );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;
}
