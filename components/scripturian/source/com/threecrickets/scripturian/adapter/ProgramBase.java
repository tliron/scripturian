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

package com.threecrickets.scripturian.adapter;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.PreparationException;

/**
 * Common implementation base for language adapters.
 * 
 * @author Tal Liron
 * @param <A>
 *        The language adapter class
 */
public abstract class ProgramBase<A extends LanguageAdapter> implements Program
{
	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param isScriptlet
	 *        Whether the source code is a scriptlet
	 * @param position
	 *        The program's position in the executable
	 * @param startLineNumber
	 *        The line number in the document for where the program's source
	 *        code begins
	 * @param startColumnNumber
	 *        The column number in the document for where the program's source
	 *        code begins
	 * @param executable
	 *        The executable
	 * @param adapter
	 *        The language adapter
	 */
	public ProgramBase( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, A adapter )
	{
		this.sourceCode = sourceCode;
		this.isScriptlet = isScriptlet;
		this.position = position;
		this.startLineNumber = startLineNumber;
		this.startColumnNumber = startColumnNumber;
		this.executable = executable;
		this.adapter = adapter;
	}

	//
	// Program
	//

	public String getSourceCode()
	{
		return sourceCode;
	}

	public void prepare() throws PreparationException
	{
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	/**
	 * The source code.
	 */
	protected final String sourceCode;

	/**
	 * Whether the source code is a scriptlet.
	 */
	protected final boolean isScriptlet;

	/**
	 * The program's position in the executable.
	 */
	protected final int position;

	/**
	 * The line number in the document for where the program's source code
	 * begins.
	 */
	protected final int startLineNumber;

	/**
	 * The column number in the document for where the program's source code
	 * begins.
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
