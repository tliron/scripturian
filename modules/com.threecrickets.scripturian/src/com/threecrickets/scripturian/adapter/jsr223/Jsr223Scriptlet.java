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

package com.threecrickets.scripturian.adapter.jsr223;

import java.io.StringReader;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.adapter.ProgramBase;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;

/**
 * Common implementation base for language adapters over JSR-223.
 * 
 * @author Tal Liron
 */
public class Jsr223Scriptlet extends ProgramBase<Jsr223LanguageAdapter>
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
	public Jsr223Scriptlet( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, Jsr223LanguageAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	@Override
	public void prepare() throws PreparationException
	{
		if( adapter.isCompilable() )
		{
			ScriptEngine scriptEngine = adapter.getStaticScriptEngine();
			if( scriptEngine instanceof Compilable )
			{
				try
				{
					compiledScript = ( (Compilable) scriptEngine ).compile( sourceCode );
				}
				catch( ScriptException x )
				{
					String scriptEngineName = (String) adapter.getAttributes().get( Jsr223LanguageAdapter.JSR223_SCRIPT_ENGINE_NAME );
					throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, "Compilation error in " + scriptEngineName, x );
				}
			}
		}
	}

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		ScriptEngine scriptEngine = adapter.getScriptEngine( executable, executionContext );
		ScriptContext scriptContext = Jsr223LanguageAdapter.getScriptContext( executionContext );

		Object value;
		try
		{
			adapter.beforeCall( scriptEngine, executionContext );

			if( compiledScript != null )
				value = compiledScript.eval( scriptContext );
			else
				// Note that we are wrapping our text with a
				// StringReader. Why? Because some
				// implementations of javax.script (notably
				// Jepp) interpret the String version of eval to
				// mean only one line of code.
				value = scriptEngine.eval( new StringReader( sourceCode ), scriptContext );

			if( ( value != null ) && adapter.isPrintOnEval() )
				executionContext.getWriter().write( value.toString() );
		}
		catch( ScriptException x )
		{
			throw Jsr223LanguageAdapter.createExecutionException( executable.getDocumentName(), x );
		}
		catch( Exception x )
		{
			// Some script engines (notably Quercus) throw their
			// own special exceptions
			throw adapter.createExecutionException( executable.getDocumentName(), x );
		}
		finally
		{
			adapter.afterCall( scriptEngine, executionContext );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The compiled script.
	 */
	private CompiledScript compiledScript;
}
