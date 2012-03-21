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

package com.threecrickets.scripturian.adapter.jsr223;

import java.util.Collection;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.exception.LanguageAdapterException;

/**
 * A {@link LanguageAdapter} that supports the <a
 * href="http://groovy.codehaus.org/">Groovy</a> language via its JSR-223
 * scripting engine.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"groovy"
})
public class GroovyAdapter extends Jsr223LanguageAdapter
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 */
	public GroovyAdapter() throws LanguageAdapterException
	{
		super();
		@SuppressWarnings("unchecked")
		Collection<String> extensions = (Collection<String>) getAttributes().get( EXTENSIONS );
		extensions.add( "gv" );
	}

	//
	// Jsr223LanguageAdapter
	//

	@Override
	public void afterCall( ScriptEngine scriptEngine, ExecutionContext executionContext )
	{
		// There's a bug in Groovy's script engine implementation (as of
		// version 1.6) that makes it lose the connection between the
		// script's output and our script context writer in some cases. This
		// makes sure that they are connected.
		scriptEngine.getContext().setAttribute( "out", executionContext.getWriter(), ScriptContext.ENGINE_SCOPE );
	}

	@Override
	public String getSourceCodeForLiteralOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\'", "\\\\'" );
		return "print('" + content + "');";
	}

	@Override
	public String getSourceCodeForExpressionOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	@Override
	public String getSourceCodeForExpressionInclude( Executable executable, ScriptEngine scriptEngine, String content )
	{
		String containerIncludeExpressionCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_EXPRESSION_COMMAND );
		return executable.getExecutableServiceName() + ".container." + containerIncludeExpressionCommand + "(" + content + ");";
	}

	@Override
	public Throwable getCauseOrExecutionException( String documentName, Throwable throwable )
	{
		// Wish there were a way to get line numbers from
		// GroovyRuntimeException!

		return null;
	}
}
