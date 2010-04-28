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
import com.caucho.quercus.program.QuercusProgram;
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
class QuercusScriptlet extends ScriptletBase<QuercusAdapter>
{
	//
	// Construction
	//

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
	public QuercusScriptlet( String sourceCode, int position, int startLineNumber, int startColumnNumber, Executable executable, QuercusAdapter adapter )
	{
		super( sourceCode, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Scriptlet
	//

	public void prepare() throws PreparationException
	{
		File mainClassFile = ScripturianUtil.getFileForScriptletClass( adapter.getCacheDir(), executable, position );
		String classname = ScripturianUtil.getClassnameForScriptlet( executable, position );

		synchronized( mainClassFile )
		{
		}
	}

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Env env = adapter.getEnvironment( executionContext );

		try
		{
			Path path = new StringPath( "<?php " + sourceCode + " ?>" );
			QuercusParser parser = new QuercusParser( adapter.quercusRuntime, path, path.openRead() );
			parser.setLocation( executable.getDocumentName(), startLineNumber );
			QuercusProgram program = parser.parse();
			QuercusPage page = new InterpretedPage( program );

			if( page.getCompiledPage() != null )
				page = page.getCompiledPage();

			page.init( env );
			page.importDefinitions( env );
			page.execute( env );

			env.getOut().flushBuffer();
			env.getOut().free();

			// program.execute( env );
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
}