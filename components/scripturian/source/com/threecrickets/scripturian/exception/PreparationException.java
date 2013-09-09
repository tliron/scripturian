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

import com.threecrickets.scripturian.Executable;

/**
 * A preparation exception. Can occur during the construction phase of an
 * executable.
 * 
 * @author Tal Liron
 * @see Executable
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

	public PreparationException( String message, Throwable cause )
	{
		super( message != null ? message : cause.getClass().getName(), cause );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;
}
