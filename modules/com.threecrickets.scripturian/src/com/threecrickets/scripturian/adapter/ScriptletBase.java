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

package com.threecrickets.scripturian.adapter;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.Scriptlet;

/**
 * Common implementation base for language adapters.
 * 
 * @author Tal Liron
 * @param <A>
 *        The language adapter class
 */
public abstract class ScriptletBase<A extends LanguageAdapter> implements Scriptlet
{
	/**
	 * Construction.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param position
	 *        The scriptlet position in the document
	 * @param startLineNumber
	 *        The start line number
	 * @param startColumnNumber
	 *        The start column number
	 * @param executable
	 *        The executable
	 * @param adapter
	 *        The language adapter
	 */
	public ScriptletBase( String sourceCode, int position, int startLineNumber, int startColumnNumber, Executable executable, A adapter )
	{
		this.sourceCode = sourceCode;
		this.position = position;
		this.startLineNumber = startLineNumber;
		this.startColumnNumber = startColumnNumber;
		this.executable = executable;
		this.adapter = adapter;
	}

	//
	// Scriptlet
	//

	public String getSourceCode()
	{
		return sourceCode;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	/**
	 * The source code.
	 */
	protected final String sourceCode;

	/**
	 * The scriptlet position in the document.
	 */
	protected final int position;

	/**
	 * The start line number.
	 */
	protected final int startLineNumber;

	/**
	 * The start column number.
	 */
	protected final int startColumnNumber;

	/**
	 * The executable.
	 */
	protected final Executable executable;

	/**
	 * The language adapter.
	 */
	protected final A adapter;
}
