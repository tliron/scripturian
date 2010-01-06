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

import javax.script.ScriptException;

import com.threecrickets.scripturian.ScriptletExceptionHelper;
import com.threecrickets.scripturian.Scripturian;

/**
 * @author Tal Liron
 */
public class DocumentRunException extends Exception
{
	//
	// Static operations
	//

	public static DocumentRunException create( String documentName, Exception exception )
	{
		// Simple wrap
		Throwable cause = exception.getCause();
		if( cause instanceof DocumentRunException )
			return (DocumentRunException) cause;

		// Try helpers
		for( ScriptletExceptionHelper scriptletExceptionHelper : Scripturian.scriptletExceptionHelpers )
		{
			DocumentRunException documentRunException = scriptletExceptionHelper.getDocumentRunException( documentName, exception );
			if( documentRunException != null )
				return documentRunException;
		}

		if( exception instanceof ScriptException )
			// Extract from ScriptException
			return new DocumentRunException( documentName, (ScriptException) exception );
		else
			// Unknown
			return new DocumentRunException( documentName, exception );
	}

	//
	// Construction
	//

	public DocumentRunException( String documentName, String message )
	{
		this( documentName, message, -1, -1 );
	}

	public DocumentRunException( String documentName, String message, int lineNumber, int columnNumber )
	{
		super( message );
		this.documentName = documentName;
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
	}

	//
	// Attributes
	//

	public String getDocumentName()
	{
		return documentName;
	}

	public int getLineNumber()
	{
		return lineNumber;
	}

	public int getColumnNumber()
	{
		return columnNumber;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;

	private final String documentName;

	private final int lineNumber;

	private final int columnNumber;

	private DocumentRunException( String documentName, ScriptException scriptException )
	{
		super( scriptException.getMessage(), scriptException.getCause() );
		this.documentName = documentName;
		this.lineNumber = scriptException.getLineNumber();
		this.columnNumber = scriptException.getColumnNumber();
	}

	private DocumentRunException( String documentName, Throwable cause )
	{
		super( cause.getMessage(), cause );
		this.documentName = documentName;
		this.lineNumber = -1;
		this.columnNumber = -1;
	}
}
