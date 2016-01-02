/**
 * Copyright 2009-2016 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.exception;

import com.threecrickets.scripturian.LanguageAdapter;

/**
 * A language adapter exception.
 * 
 * @author Tal Liron
 */
public class LanguageAdapterException extends ParsingException
{
	//
	// Construction
	//

	public LanguageAdapterException( Class<? extends LanguageAdapter> adapter, String documentName, int lineNumber, int columnNumber, String message )
	{
		super( documentName, lineNumber, columnNumber, message );
		this.adapter = adapter;
	}

	public LanguageAdapterException( Class<? extends LanguageAdapter> adapter, String documentName, int lineNumber, int columnNumber, String message, Throwable cause )
	{
		super( documentName, lineNumber, columnNumber, message, cause );
		this.adapter = adapter;
	}

	public LanguageAdapterException( Class<? extends LanguageAdapter> adapter, String documentName, int lineNumber, int columnNumber, Throwable cause )
	{
		super( documentName, lineNumber, columnNumber, cause );
		this.adapter = adapter;
	}

	public LanguageAdapterException( Class<? extends LanguageAdapter> adapter, String documentName, String message, Throwable cause )
	{
		super( documentName, message, cause );
		this.adapter = adapter;
	}

	public LanguageAdapterException( Class<? extends LanguageAdapter> adapter, String documentName, String message )
	{
		super( documentName, message );
		this.adapter = adapter;
	}

	public LanguageAdapterException( Class<? extends LanguageAdapter> adapter, String message, Throwable cause )
	{
		super( message, cause );
		this.adapter = adapter;
	}

	public LanguageAdapterException( Class<? extends LanguageAdapter> adapter, Throwable cause )
	{
		super( cause );
		this.adapter = adapter;
	}

	public LanguageAdapterException( Class<? extends LanguageAdapter> adapter, String message )
	{
		super( message );
		this.adapter = adapter;
	}

	//
	// Attributes
	//

	/**
	 * The adapter adapter class.
	 * 
	 * @return The adapter adapter class
	 */
	public Class<? extends LanguageAdapter> getLanguage()
	{
		return adapter;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;

	/**
	 * The language adapter class.
	 */
	private final Class<? extends LanguageAdapter> adapter;
}
