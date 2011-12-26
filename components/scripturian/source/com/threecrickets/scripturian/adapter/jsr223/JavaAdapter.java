/**
 * Copyright 2009-2011 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.adapter.jsr223;

import javax.script.ScriptEngine;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.exception.LanguageAdapterException;

/**
 * A {@link LanguageAdapter} that supports the Java language. Experimental code!
 * Doesn't work!
 * 
 * @author Tal Liron
 */
@ScriptEngines("java")
public class JavaAdapter extends Jsr223LanguageAdapter
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 */
	public JavaAdapter() throws LanguageAdapterException
	{
		super();
	}

	//
	// Jsr223LanguageAdapter
	//

	@Override
	public String getScriptletHeader( Executable executable, ScriptEngine scriptEngine )
	{
		return "class Text { private static javax.script.ScriptContext scriptContext; public static void setScriptContext(javax.script.ScriptContext sc) { scriptContext = sc; } public static void main(String arguments[]) throws Exception {";
	}

	@Override
	public String getScriptletFooter( Executable executable, ScriptEngine scriptEngine )
	{
		return "}}";
	}

	@Override
	public String getSourceCodeForLiteralOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\"", "\\\\\"" );
		return "scriptContext.getWriter().write(\"" + content + "\");";
	}

	@Override
	public String getSourceCodeForExpressionOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		return "scriptContext.getWriter().write((" + content + ").toString());";
	}

	@Override
	public String getSourceCodeForExpressionInclude( Executable executable, ScriptEngine scriptEngine, String content )
	{
		return executable.getExecutableServiceName() + ".container.includeDocument((" + content + ").toString());";
	}

	@Override
	public String getInvocationAsProgram( Executable executable, ScriptEngine scriptEngine, String content, Object... arguments )
	{
		return content + "();";
	}
}
