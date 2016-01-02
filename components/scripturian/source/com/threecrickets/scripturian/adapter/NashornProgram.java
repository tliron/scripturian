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

import jdk.nashorn.internal.runtime.Context;
import jdk.nashorn.internal.runtime.ErrorManager;
import jdk.nashorn.internal.runtime.ScriptFunction;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.ScriptRuntime;
import jdk.nashorn.internal.runtime.Source;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;

/**
 * @author Tal Liron
 */
public class NashornProgram extends ProgramBase<NashornAdapter>
{
	//
	// Constructor
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
	public NashornProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, NashornAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	@Override
	public void prepare() throws PreparationException
	{
		// Nashorn handles this internally using the "persistent.code.cache"
		// context option and the "nashorn.persistent.code.cache" system
		// property.
	}

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		ScriptObject oldGlobal = Context.getGlobal();
		try
		{
			ScriptObject globalScope = adapter.getGlobalScope( executionContext );
			ErrorManager errorManager = adapter.context.getErrorManager();

			// long s = System.currentTimeMillis();
			ScriptFunction script = adapter.context.compileScript( Source.sourceFor( executable.getDocumentName(), sourceCode ), globalScope );
			if( ( script == null ) || errorManager.hasErrors() )
				throw new ParsingException( executable.getDocumentName() );
			// s = System.currentTimeMillis() - s;
			// System.out.println( "COMPILE: " + s / 1000.0f );

			try
			{
				// s = System.currentTimeMillis();
				ScriptRuntime.apply( script, globalScope );
				// s = System.currentTimeMillis() - s;
				// System.out.println( "RUN: " + s / 1000.0f );
			}
			catch( Throwable x )
			{
				throw NashornAdapter.createExecutionException( x, executable.getDocumentName() );
			}
			finally
			{
				adapter.context.getOut().flush();
				adapter.context.getErr().flush();
			}
		}
		finally
		{
			if( oldGlobal != null )
				Context.setGlobal( oldGlobal );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private
}