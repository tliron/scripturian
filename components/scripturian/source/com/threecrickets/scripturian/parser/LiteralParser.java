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

package com.threecrickets.scripturian.parser;

import java.util.Collection;
import java.util.Collections;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutableSegment;
import com.threecrickets.scripturian.Parser;
import com.threecrickets.scripturian.ParsingContext;
import com.threecrickets.scripturian.exception.DocumentException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * Turns the source code into a single, literal executable segment.
 * 
 * @author Tal Liron
 */
public class LiteralParser implements Parser
{
	//
	// Constants
	//

	public final static String NAME = "literal";

	//
	// Parser
	//

	public String getName()
	{
		return NAME;
	}

	public Collection<ExecutableSegment> parse( String sourceCode, ParsingContext parsingContext, Executable executable ) throws ParsingException, DocumentException
	{
		ExecutableSegment segment = new ExecutableSegment( sourceCode, 1, 1, false, false, null );
		return Collections.singleton( segment );
	}
}
