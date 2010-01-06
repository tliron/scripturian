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

package com.threecrickets.scripturian.helper;

import javax.script.ScriptEngine;

import com.threecrickets.scripturian.Document;
import com.threecrickets.scripturian.ScriptletParsingHelper;
import com.threecrickets.scripturian.annotation.ScriptEnginePriorityExtensions;
import com.threecrickets.scripturian.annotation.ScriptEngines;

/**
 * An {@link ScriptletParsingHelper} that supports the <a
 * href="http://groovy.codehaus.org/">Groovy</a> scripting language.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"groovy", "Groovy"
})
@ScriptEnginePriorityExtensions(
{
	"gv"
})
public class GroovyScriptletParsingHelper implements ScriptletParsingHelper
{
	//
	// ScriptletParsingHelper
	//

	public boolean isPrintOnEval()
	{
		return false;
	}

	public boolean isCompilable()
	{
		return true;
	}

	public String getScriptletHeader( Document document, ScriptEngine scriptEngine )
	{
		// There's a bug in Groovy's script engine implementation (as of version
		// 1.6) that makes it lose the connection between the script's output
		// and our script context writer in some cases. This makes sure that
		// they are connected.
		return "out=context.writer;";
	}

	public String getScriptletFooter( Document document, ScriptEngine scriptEngine )
	{
		return null;
	}

	public String getTextAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\'", "\\\\'" );
		return "print('" + content + "');";
	}

	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	public String getExpressionAsInclude( Document document, ScriptEngine scriptEngine, String content )
	{
		return document.getDocumentVariableName() + ".container.includeDocument(" + content + ");";
	}

	public String getInvocationAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return null;
	}
}
