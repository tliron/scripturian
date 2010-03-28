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

import javax.script.ScriptEngine;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.exception.LanguageInitializationException;

/**
 * An {@link LanguageAdapter} that supports Java as if it were a scripting
 * language.
 * 
 * @author Tal Liron
 */
@ScriptEngines("java")
public class JavaAdapter extends Jsr223LanguageAdapter
{
	//
	// ScriptletHelper
	//

	public JavaAdapter() throws LanguageInitializationException
	{
		super();
	}

	@Override
	public String getScriptletHeader( Executable document, ScriptEngine scriptEngine )
	{
		return "class Text { private static javax.script.ScriptContext scriptContext; public static void setScriptContext(javax.script.ScriptContext sc) { scriptContext = sc; } public static void main(String arguments[]) throws Exception {";
	}

	@Override
	public String getScriptletFooter( Executable document, ScriptEngine scriptEngine )
	{
		return "}}";
	}

	@Override
	public String getTextAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\"", "\\\\\"" );
		return "scriptContext.getWriter().write(\"" + content + "\");";
	}

	@Override
	public String getExpressionAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		return "scriptContext.getWriter().write((" + content + ").toString());";
	}

	@Override
	public String getExpressionAsInclude( Executable document, ScriptEngine scriptEngine, String content )
	{
		return document.getExecutableVariableName() + ".container.includeDocument((" + content + ").toString());";
	}

	@Override
	public String getInvocationAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		return content + "();";
	}
}
