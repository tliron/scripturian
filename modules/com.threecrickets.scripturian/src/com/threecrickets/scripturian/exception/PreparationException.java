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

package com.threecrickets.scripturian.exception;

/**
 * @author Tal Liron
 */
public class PreparationException extends ParsingException
{
	//
	// Construction
	//

	public PreparationException( String documentName, int lineNumber, int columnNumber, String message )
	{
		super( documentName, lineNumber, columnNumber, message );
	}

	public PreparationException( String documentName, int lineNumber, int columnNumber, String message, Throwable cause )
	{
		super( documentName, lineNumber, columnNumber, message, cause );
	}

	public PreparationException( String documentName, int lineNumber, int columnNumber, Throwable cause )
	{
		super( documentName, lineNumber, columnNumber, cause );
	}

	public PreparationException( String documentName, String message, Throwable cause )
	{
		super( documentName, message, cause );
	}

	public PreparationException( String documentName, String message )
	{
		super( documentName, message );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;
}
