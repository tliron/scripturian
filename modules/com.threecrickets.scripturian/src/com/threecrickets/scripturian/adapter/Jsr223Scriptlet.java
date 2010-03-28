package com.threecrickets.scripturian.adapter;

import java.io.StringReader;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.Scriptlet;
import com.threecrickets.scripturian.exception.CompilationException;
import com.threecrickets.scripturian.exception.ExecutableInitializationException;
import com.threecrickets.scripturian.exception.ExecutionException;

public class Jsr223Scriptlet extends Scriptlet
{
	private final String code;

	private CompiledScript compiledScript;

	private final Jsr223LanguageAdapter adapter;

	private final Executable document;

	@Override
	public String getCode()
	{
		return code;
	}

	public Jsr223Scriptlet( String code, Jsr223LanguageAdapter adapter, Executable document )
	{
		this.code = code;
		this.adapter = adapter;
		this.document = document;
	}

	@Override
	public void compile() throws ExecutableInitializationException
	{
		if( adapter.isCompilable() )
		{
			ScriptEngine scriptEngine = adapter.getStaticScriptEngine();
			if( scriptEngine instanceof Compilable )
			{
				try
				{
					compiledScript = ( (Compilable) scriptEngine ).compile( code );
				}
				catch( ScriptException x )
				{
					String scriptEngineName = (String) adapter.getAttributes().get( "jsr223.scriptEngineName" );
					throw new CompilationException( document.getName(), "Compilation error in " + scriptEngineName, x );
				}
			}
		}
	}

	@Override
	public Object execute( ExecutionContext executionContext ) throws ExecutableInitializationException, ExecutionException
	{
		ScriptEngine scriptEngine = adapter.getScriptEngine( document, executionContext );
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
				value = scriptEngine.eval( new StringReader( code ), scriptContext );

			if( ( value != null ) && adapter.isPrintOnEval() )
				executionContext.getWriter().write( value.toString() );
		}
		catch( ScriptException x )
		{
			throw ExecutionException.create( document.getName(), executionContext.getManager(), x );
		}
		catch( Exception x )
		{
			// Some script engines (notably Quercus) throw their
			// own special exceptions
			throw ExecutionException.create( document.getName(), executionContext.getManager(), x );
		}
		finally
		{
			adapter.afterCall( scriptEngine, executionContext );
		}

		return value;
	}
}
