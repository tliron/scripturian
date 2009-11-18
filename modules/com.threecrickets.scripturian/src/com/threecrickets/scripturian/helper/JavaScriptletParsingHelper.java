/**
 * Copyright 2009 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://www.threecrickets.com/
 */

package com.threecrickets.scripturian.helper;

import javax.script.ScriptEngine;

import com.threecrickets.scripturian.Document;
import com.threecrickets.scripturian.ScriptletParsingHelper;
import com.threecrickets.scripturian.ScriptEngines;

/**
 * An {@link ScriptletParsingHelper} that supports Java as if it were a
 * scripting language.
 * 
 * @author Tal Liron
 */
@ScriptEngines("java")
public class JavaScriptletParsingHelper implements ScriptletParsingHelper
{
	//
	// ScriptletParsingHelper
	//

	public boolean isPrintOnEval()
	{
		return false;
	}

	public String getScriptletHeader( Document document, ScriptEngine scriptEngine )
	{
		return "class Text { private static javax.script.ScriptContext scriptContext; public static void setScriptContext(javax.script.ScriptContext sc) { scriptContext = sc; } public static void main(String arguments[]) throws Exception {";
	}

	public String getScriptletFooter( Document document, ScriptEngine scriptEngine )
	{
		return "}}";
	}

	public String getTextAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\"", "\\\\\"" );
		return "scriptContext.getWriter().write(\"" + content + "\");";
	}

	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "scriptContext.getWriter().write((" + content + ").toString());";
	}

	public String getExpressionAsInclude( Document document, ScriptEngine scriptEngine, String content )
	{
		return document.getDocumentVariableName() + ".container.includeDocument((" + content + ").toString());";
	}

	public String getInvocationAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return content + "();";
	}
}
