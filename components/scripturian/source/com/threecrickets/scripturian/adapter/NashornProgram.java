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

import jdk.nashorn.api.scripting.NashornException;
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
	}

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Context context = adapter.getContext( executionContext );
		ScriptObject globalScope = adapter.getGlobalScope( executionContext, context );
		ErrorManager errorManager = context.getErrorManager();

		ScriptFunction script = context.compileScript( new Source( executable.getDocumentName(), sourceCode ), globalScope );
		if( ( script == null ) || errorManager.hasErrors() )
			throw new ParsingException( executable.getDocumentName() );

		try
		{
			ScriptRuntime.apply( script, globalScope );
		}
		catch( NashornException x )
		{
			throw NashornAdapter.createExecutionException( x );
		}
		finally
		{
			context.getOut().flush();
			context.getErr().flush();
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private
}