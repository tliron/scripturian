/**
 * Copyright 2009-2015 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian;

import java.util.Collection;

import com.threecrickets.scripturian.exception.DocumentException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * @author Tal Liron
 */
public interface Parser
{
	/**
	 * The parser name.
	 * 
	 * @return The parser name
	 */
	public String getName();

	/**
	 * Parses source code, turning it into executable segments.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param parsingContext
	 *        The parsing context
	 * @param executable
	 *        The executable
	 * @return The executable segments
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws DocumentException
	 *         In case of a document retrieval error
	 */
	public Collection<ExecutableSegment> parse( String sourceCode, ParsingContext parsingContext, Executable executable ) throws ParsingException, DocumentException;
}
