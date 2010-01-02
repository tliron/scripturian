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

package com.threecrickets.scripturian.exception;

/**
 * @author Tal Liron
 */
public class DocumentInitializationException extends Exception
{
	//
	// Static operations
	//

	public static DocumentInitializationException scriptEngineNotFound( String documentName, String scriptEngineName )
	{
		return new DocumentInitializationException( documentName, "Unsupported script engine: " + scriptEngineName );
	}

	public static DocumentInitializationException scriptletParsingHelperNotFound( String documentName, String scriptEngineName )
	{
		return new DocumentInitializationException( documentName, "Scriptlet parsing helper not available for script engine: " + scriptEngineName );
	}

	//
	// Construction
	//

	public DocumentInitializationException( String documentName, String message )
	{
		super( message );
		this.documentName = documentName;
	}

	public DocumentInitializationException( String documentName, String message, Throwable cause )
	{
		super( message, cause );
		this.documentName = documentName;
	}

	//
	// Attributes
	//

	public String getDocumentName()
	{
		return documentName;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;

	private final String documentName;
}
