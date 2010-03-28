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

import java.util.Collection;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.exception.LanguageInitializationException;

/**
 * An {@link LanguageAdapter} that supports the <a
 * href="http://groovy.codehaus.org/">Groovy</a> scripting language.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"groovy", "Groovy"
})
public class GroovyAdapter extends Jsr223LanguageAdapter
{
	//
	// ScriptletHelper
	//

	@SuppressWarnings("unchecked")
	public GroovyAdapter() throws LanguageInitializationException
	{
		super();
		( (Collection<String>) getAttributes().get( EXTENSIONS ) ).add( "gv" );
	}

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
	public String getTextAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\'", "\\\\'" );
		return "print('" + content + "');";
	}

	@Override
	public String getExpressionAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	@Override
	public String getExpressionAsInclude( Executable document, ScriptEngine scriptEngine, String content )
	{
		return document.getExecutableVariableName() + ".container.includeDocument(" + content + ");";
	}

	@Override
	public Throwable getCauseOrDocumentRunException( String documentName, Throwable throwable )
	{
		// Wish there were a way to get line numbers from
		// GroovyRuntimeException!

		return null;
	}
}
