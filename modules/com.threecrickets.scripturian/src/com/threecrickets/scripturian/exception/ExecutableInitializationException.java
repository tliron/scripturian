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
public class ExecutableInitializationException extends Exception
{
	//
	// Static operations
	//

	public static ExecutableInitializationException adapterNotFound( String documentName, String languageTag )
	{
		return new ExecutableInitializationException( documentName, "Adapter not available for language: " + languageTag );
	}

	//
	// Construction
	//

	public ExecutableInitializationException( String documentName, String message )
	{
		super( message );
		this.documentName = documentName;
	}

	public ExecutableInitializationException( String documentName, String message, Throwable cause )
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
