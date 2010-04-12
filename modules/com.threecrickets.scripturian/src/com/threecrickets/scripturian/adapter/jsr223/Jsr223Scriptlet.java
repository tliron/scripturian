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
import com.threecrickets.scripturian.Scriptlet;
import com.threecrickets.scripturian.exception.PreparationException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.ExecutionException;

/**
 * @author Tal Liron
 */
public class Jsr223Scriptlet implements Scriptlet
{
	//
	// Construction
	//

	public Jsr223Scriptlet( String sourceCode, int startLineNumber, int startColumnNumber, Jsr223LanguageAdapter languageAdapter, Executable executable )
	{
		this.sourceCode = sourceCode;
		this.startLineNumber = startLineNumber;
		this.startColumnNumber = startColumnNumber;
		this.languageAdapter = languageAdapter;
		this.executable = executable;
	}

	//
	// Scriptlet
	//

	public String getSourceCode()
	{
		return sourceCode;
	}

	public void prepare() throws PreparationException
	{
		if( languageAdapter.isCompilable() )
		{
			ScriptEngine scriptEngine = languageAdapter.getStaticScriptEngine();
			if( scriptEngine instanceof Compilable )
			{
				try
				{
					compiledScript = ( (Compilable) scriptEngine ).compile( sourceCode );
				}
				catch( ScriptException x )
				{
					String scriptEngineName = (String) languageAdapter.getAttributes().get( Jsr223LanguageAdapter.JSR223_SCRIPT_ENGINE_NAME );
					throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, "Compilation error in " + scriptEngineName, x );
				}
			}
		}
	}

	public Object execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		ScriptEngine scriptEngine = languageAdapter.getScriptEngine( executable, executionContext );
		ScriptContext scriptContext = Jsr223LanguageAdapter.getScriptContext( executionContext );

		Object value;
		try
		{
			languageAdapter.beforeCall( scriptEngine, executionContext );

			if( compiledScript != null )
				value = compiledScript.eval( scriptContext );
			else
				// Note that we are wrapping our text with a
				// StringReader. Why? Because some
				// implementations of javax.script (notably
				// Jepp) interpret the String version of eval to
				// mean only one line of code.
				value = scriptEngine.eval( new StringReader( sourceCode ), scriptContext );

			if( ( value != null ) && languageAdapter.isPrintOnEval() )
				executionContext.getWriter().write( value.toString() );
		}
		catch( ScriptException x )
		{
			throw ExecutionException.create( executable.getDocumentName(), executionContext.getManager(), x );
		}
		catch( Exception x )
		{
			// Some script engines (notably Quercus) throw their
			// own special exceptions
			throw ExecutionException.create( executable.getDocumentName(), executionContext.getManager(), x );
		}
		finally
		{
			languageAdapter.afterCall( scriptEngine, executionContext );
		}

		return value;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final String sourceCode;

	private final int startLineNumber;

	private final int startColumnNumber;

	private final Jsr223LanguageAdapter languageAdapter;

	private final Executable executable;

	private CompiledScript compiledScript;
}
