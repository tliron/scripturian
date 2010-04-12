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

package com.threecrickets.scripturian.internal;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Scriptlet;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * @author Tal Liron
 * @see Executable
 */
public class ExecutableSegment
{
	//
	// Construction
	//

	public ExecutableSegment( String sourceCode, int startLineNumber, int startColumnNumber, boolean isScriptlet, String languageTag )
	{
		this.sourceCode = sourceCode;
		this.startLineNumber = startLineNumber;
		this.startColumnNumber = startColumnNumber;
		this.isScriptlet = isScriptlet;
		this.languageTag = languageTag;
	}

	//
	// Attributes
	//

	public final boolean isScriptlet;

	public final String languageTag;

	public String sourceCode;

	public int startLineNumber;

	public int startColumnNumber;

	public Scriptlet scriptlet;

	//
	// Operations
	//

	public void createScriptlet( Executable executable, LanguageManager manager, boolean prepare ) throws ParsingException
	{
		LanguageAdapter adapter = manager.getAdapterByTag( languageTag );
		if( adapter == null )
			throw ParsingException.adapterNotFound( executable.getDocumentName(), startLineNumber, startColumnNumber, languageTag );

		scriptlet = adapter.createScriptlet( sourceCode, startLineNumber, startColumnNumber, executable );

		if( prepare )
			scriptlet.prepare();
	}
}