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

import java.io.File;

import com.caucho.quercus.QuercusExitException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.page.InterpretedPage;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.vfs.Path;
import com.caucho.vfs.StringPath;
import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * @author Tal Liron
 */
class QuercusProgram extends ProgramBase<QuercusAdapter>
{
	//
	// Construction
	//

	/**
	 * Construction.
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
	// Scriptlet
	//

	public void prepare() throws PreparationException
	{
		File classFile = ScripturianUtil.getFileForProgramClass( adapter.getCacheDir(), executable, position );
		String classname = ScripturianUtil.getClassnameForProgram( executable, position );

		synchronized( classFile )
		{
			// if( page.getCompiledPage() != null )
			// page = page.getCompiledPage();
		}
	}

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Env environment = adapter.getEnvironment( executionContext );

		if( page == null )
		{
			try
			{
				// Note that we're caching the resulting parsed page for the
				// future. Might as well!

				Path path = new StringPath( isScriptlet ? "<?php " + sourceCode + " ?>" : sourceCode );
				QuercusParser parser = new QuercusParser( adapter.quercusRuntime, path, path.openRead() );
				parser.setLocation( executable.getDocumentName(), startLineNumber );
				com.caucho.quercus.program.QuercusProgram program = parser.parse();
				page = new InterpretedPage( program );
			}
			catch( Exception x )
			{
				throw QuercusAdapter.createExecutionException( executable.getDocumentName(), x );
			}
		}

		try
		{
			page.init( environment );
			page.importDefinitions( environment );
			page.execute( environment );

			environment.getOut().flushBuffer();
		}
		catch( QuercusExitException x )
		{
			throw QuercusAdapter.createExecutionException( executable.getDocumentName(), x );
		}
		catch( Exception x )
		{
			throw QuercusAdapter.createExecutionException( executable.getDocumentName(), x );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private QuercusPage page;
}