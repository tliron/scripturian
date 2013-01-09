/**
 * Copyright 2009-2013 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.exception;

/**
 * The document dependencies are in a loop.
 * 
 * @author Tal Liron
 */
public class DocumentDependencyLoopException extends DocumentException
{
	//
	// Construction
	//

	public DocumentDependencyLoopException( String message )
	{
		super( message );
	}

	public DocumentDependencyLoopException( String message, Throwable cause )
	{
		super( message, cause );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;
}
