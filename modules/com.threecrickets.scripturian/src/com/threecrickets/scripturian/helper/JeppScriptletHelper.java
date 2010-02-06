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
import com.threecrickets.scripturian.ScriptletHelper;
import com.threecrickets.scripturian.annotation.ScriptEngines;

/**
 * An {@link ScriptletHelper} that supports the Python scripting language
 * as implemented by <a href="http://jepp.sourceforge.net/">Jepp</a>.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"jepp", "jep"
})
public class JeppScriptletHelper extends ScriptletHelper
{
	//
	// ScriptletHelper
	//

	@Override
	public String getScriptletHeader( Document document, ScriptEngine scriptEngine )
	{
		// Apparently the Java Scripting support for Jepp does not correctly
		// set global variables, not redirect stdout and stderr. Luckily, the
		// Python interface is compatible with Java's Writer interface, so we
		// can redirect them explicitly.
		return document.getDocumentVariableName() + "=context.getAttribute('" + document.getDocumentVariableName() + "');import sys;sys.stdout=context.getWriter();sys.stderr=context.getErrorWriter();";
	}

	@Override
	public String getTextAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "sys.stdout.write(\"" + content + "\"),;";
	}

	@Override
	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "sys.stdout.write(" + content + ");";
	}

	@Override
	public String getExpressionAsInclude( Document document, ScriptEngine scriptEngine, String content )
	{
		return document.getDocumentVariableName() + ".getContainer().includeDocument(" + content + ");";
	}
}
