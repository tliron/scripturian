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

package com.threecrickets.scripturian.adapter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import jep.Jep;
import jep.JepException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * @author Tal Liron
 */
class JeppProgram extends ProgramBase<JeppAdapter>
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
	public JeppProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, JeppAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		File classFile = ScripturianUtil.getFileForProgramClass( adapter.getCacheDir(), executable, position );
		File sourceFile = new File( classFile.getPath().substring( 0, classFile.getPath().length() - 6 ) + ".py" );

		synchronized( classFile )
		{
			if( !sourceFile.exists() )
			{
				sourceFile.getParentFile().mkdirs();
				try
				{
					Writer writer = new FileWriter( sourceFile );
					try
					{
						writer.write( sourceCode );
					}
					finally
					{
						writer.close();
					}
				}
				catch( IOException x )
				{
					throw new ExecutionException( executable.getDocumentName(), x.getMessage(), x );
				}
			}
		}

		try
		{
			Jep jeppRuntime = JeppAdapter.getJeppRuntime( executable, executionContext );
			jeppRuntime.runScript( sourceFile.getAbsolutePath() );
		}
		catch( JepException x )
		{
			throw new ExecutionException( executable.getDocumentName(), x.getMessage(), x );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private
}