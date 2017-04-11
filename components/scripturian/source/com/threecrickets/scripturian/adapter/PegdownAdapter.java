/**
 * Copyright 2009-2017 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.adapter;

import java.util.Arrays;

import org.pegdown.PegDownProcessor;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * A {@link LanguageAdapter} that supports the <a
 * href="http://daringfireball.net/projects/markdown/">Markdown</a> HTML markup
 * language as implemented by <a
 * href="https://github.com/sirthias/pegdown">pegdown</a>.
 * 
 * @author Tal Liron
 */
public class PegdownAdapter extends LanguageAdapterBase
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 *         In case of an initialization error
	 */
	public PegdownAdapter() throws LanguageAdapterException
	{
		super( "pegdown", "", "Markdown", "", Arrays.asList( "md", "markdown" ), "md", Arrays.asList( "markdown", "md", "pegdown" ), "pegdown" );
	}

	//
	// LanguageAdapter
	//

	@Override
	public boolean isEphemeral()
	{
		return true;
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new PegdownProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	protected final PegDownProcessor processor = new PegDownProcessor();
}
