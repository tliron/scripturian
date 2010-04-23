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
import com.threecrickets.scripturian.Scriptlet;

/**
 * Common implementation base for language adapters.
 * 
 * @author Tal Liron
 */
public abstract class ScriptletBase implements Scriptlet
{
	/**
	 * Construction.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param startLineNumber
	 *        The start line number
	 * @param startColumnNumber
	 *        The start column number
	 * @param executable
	 *        The executable
	 */
	public ScriptletBase( String sourceCode, int startLineNumber, int startColumnNumber, Executable executable )
	{
		this.sourceCode = sourceCode;
		this.startLineNumber = startLineNumber;
		this.startColumnNumber = startColumnNumber;
		this.executable = executable;
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
}
