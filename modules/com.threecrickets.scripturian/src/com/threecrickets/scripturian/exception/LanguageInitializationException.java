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

import com.threecrickets.scripturian.LanguageAdapter;

/**
 * @author Tal Liron
 */
public class LanguageInitializationException extends Exception
{
	//
	// Construction
	//

	public LanguageInitializationException( Class<? extends LanguageAdapter> language, String message )
	{
		super( message );
		this.language = language;
	}

	public LanguageInitializationException( Class<? extends LanguageAdapter> language, Throwable cause )
	{
		super( cause );
		this.language = language;
	}

	public LanguageInitializationException( Class<? extends LanguageAdapter> language, String message, Throwable cause )
	{
		super( message, cause );
		this.language = language;
	}

	//
	// Attributes
	//

	public Class<? extends LanguageAdapter> getLanguage()
	{
		return language;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;

	private final Class<? extends LanguageAdapter> language;
}
