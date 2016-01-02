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

package com.threecrickets.scripturian.adapter;

import java.io.IOException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * @author Tal Liron
 */
public class PegdownProgram extends ProgramBase<PegdownAdapter>
{
	//
	// Construction
	//

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
	public PegdownProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, PegdownAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		try
		{
			executionContext.getWriter().write( adapter.processor.markdownToHtml( sourceCode ) );
		}
		catch( IOException x )
		{
			throw new ExecutionException( executable.getDocumentName(), x );
		}
	}
}
