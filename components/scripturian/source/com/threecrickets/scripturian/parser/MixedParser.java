/**
 * Copyright 2009-2014 Three Crickets LLC.
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
import java.util.Iterator;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutableSegment;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Parser;
import com.threecrickets.scripturian.ParsingContext;
import com.threecrickets.scripturian.exception.DocumentException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * Base class for parsers that include a mix of literal executable segments and
 * programs.
 * <p>
 * Includes a facility for optimizing a collection of executable segments, which
 * combines adjacent segments of the same type into single segments, and creates
 * all necessary programs.
 * 
 * @author Tal Liron
 */
public abstract class MixedParser implements Parser
{
	//
	// Operations
	//

	public Collection<ExecutableSegment> optimize( Collection<ExecutableSegment> segments, ParsingContext parsingContext, Executable executable ) throws ParsingException, DocumentException
	{
		LanguageManager languageManager = parsingContext.getLanguageManager();
		String documentName = executable.getDocumentName();

		// Collapse segments of same kind
		ExecutableSegment previous = null;
		ExecutableSegment current;
		for( Iterator<ExecutableSegment> i = segments.iterator(); i.hasNext(); )
		{
			current = i.next();

			if( previous != null )
			{
				if( previous.isProgram == current.isProgram )
				{
					if( current.languageTag.equals( previous.languageTag ) )
					{
						// Collapse current into previous
						i.remove();
						previous.startLineNumber = current.startLineNumber;
						previous.startColumnNumber = current.startColumnNumber;
						previous.sourceCode += current.sourceCode;
						current = previous;
					}
				}
			}

			previous = current;
		}

		// Collapse segments of same language (does not convert first segment
		// into a program)
		previous = null;
		for( Iterator<ExecutableSegment> i = segments.iterator(); i.hasNext(); )
		{
			current = i.next();

			if( ( previous != null ) && previous.isProgram )
			{
				if( previous.languageTag.equals( current.languageTag ) )
				{
					if( current.isProgram )
					{
						previous.sourceCode += current.sourceCode;

						// Collapse current into previous
						i.remove();
					}
					else
					{
						// Converting to program if necessary
						LanguageAdapter adapter = languageManager.getAdapterByTag( current.languageTag );
						if( adapter == null )
							throw ParsingException.adapterNotFound( documentName, current.startLineNumber, current.startColumnNumber, current.languageTag );

						String literalOutput = adapter.getSourceCodeForLiteralOutput( current.sourceCode, executable );
						if( literalOutput != null )
						{
							previous.sourceCode += literalOutput;

							// Collapse current into previous
							i.remove();
						}
					}

					current = previous;
				}
			}

			previous = current;
		}

		// Update positions and create programs
		int position = 0;
		boolean prepare = parsingContext.isPrepare();
		boolean debug = parsingContext.isDebug();
		for( ExecutableSegment segment : segments )
		{
			segment.position = position++;
			if( segment.isProgram )
				segment.createProgram( executable, languageManager, prepare, debug );
		}

		return segments;
	}
}
