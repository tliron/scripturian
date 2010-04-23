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
 * A segment within an executable. "Text-with-scriptlets" executables can have
 * multiple segments, some of which are plain text and some of which are
 * scriptlets in various languages.
 * 
 * @author Tal Liron
 * @see Executable
 */
public class ExecutableSegment
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param startLineNumber
	 *        The start line number
	 * @param startColumnNumber
	 *        The start column number
	 * @param isScriptlet
	 *        Whether this segment is a scriptlet
	 * @param languageTag
	 *        The language tag for scriptlets
	 */
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

	/**
	 * Whether this segment is a scriptlet.
	 */
	public final boolean isScriptlet;

	/**
	 * The language tag for scriptlets.
	 */
	public final String languageTag;

	/**
	 * The source code.
	 */
	public String sourceCode;

	/**
	 * The scriptlet position in the document.
	 */
	public int position = 0;

	/**
	 * The start line number.
	 */
	public int startLineNumber;

	/**
	 * The start column number.
	 */
	public int startColumnNumber;

	/**
	 * The scriptlet.
	 * 
	 * @see #createScriptlet(Executable, LanguageManager, boolean)
	 */
	public Scriptlet scriptlet;

	//
	// Operations
	//

	/**
	 * Creates a scriptlet for this segment using the appropriate language
	 * adapter.
	 * 
	 * @param executable
	 *        The executable
	 * @param manager
	 *        The language manager
	 * @param prepare
	 *        Whether to prepare the scriptlet
	 * @throws ParsingException
	 * @see #isScriptlet
	 * @see #languageTag
	 * @see #scriptlet
	 */
	public void createScriptlet( Executable executable, LanguageManager manager, boolean prepare ) throws ParsingException
	{
		LanguageAdapter adapter = manager.getAdapterByTag( languageTag );
		if( adapter == null )
			throw ParsingException.adapterNotFound( executable.getDocumentName(), startLineNumber, startColumnNumber, languageTag );

		scriptlet = adapter.createScriptlet( sourceCode, position, startLineNumber, startColumnNumber, executable );

		if( prepare )
			scriptlet.prepare();
	}
}