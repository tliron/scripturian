/**
 * Copyright 2009-2012 Three Crickets LLC.
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
import java.util.concurrent.atomic.AtomicReference;

import com.caucho.quercus.QuercusExitException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.page.InterpretedPage;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.parser.QuercusParseException;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.vfs.Path;
import com.caucho.vfs.StringPath;
import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * @author Tal Liron
 */
class QuercusProgram extends ProgramBase<QuercusAdapter>
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
	public QuercusProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, QuercusAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Env environment = adapter.getEnvironment( executionContext );

		com.caucho.quercus.program.QuercusProgram program = programReference.get();
		if( program == null )
		{
			try
			{
				Path path = new StringPath( isScriptlet ? "<?php " + sourceCode + " ?>" : sourceCode );
				QuercusParser parser = new QuercusParser( adapter.quercusRuntime, path, path.openRead() );
				parser.setLocation( executable.getDocumentName(), startLineNumber );
				program = parser.parse();

				programReference.compareAndSet( null, program );
			}
			catch( QuercusParseException x )
			{
				throw QuercusAdapter.createParsingException( executable.getDocumentName(), x );
			}
			catch( Exception x )
			{
				throw QuercusAdapter.createExecutionException( executable.getDocumentName(), x );
			}
		}

		try
		{
			QuercusPage page = new InterpretedPage( program );
			page.init( environment );
			page.importDefinitions( environment );
			page.execute( environment );
		}
		catch( QuercusExitException x )
		{
		}
		catch( Exception x )
		{
			throw QuercusAdapter.createExecutionException( executable.getDocumentName(), x );
		}
		finally
		{
			try
			{
				environment.getOut().flush();
				executionContext.getWriter().flush();
			}
			catch( IOException xx )
			{
			}
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The cached parsed program.
	 */
	private final AtomicReference<com.caucho.quercus.program.QuercusProgram> programReference = new AtomicReference<com.caucho.quercus.program.QuercusProgram>();
}