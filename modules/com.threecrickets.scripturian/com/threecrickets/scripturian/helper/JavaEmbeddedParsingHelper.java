/**
 * Copyright 2009 Three Crickets.
 * <p>
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * <p>
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * <p>
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * <p>
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * <p>
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://www.threecrickets.com/
 */

package com.threecrickets.scripturian.helper;

import javax.script.ScriptEngine;

import com.threecrickets.scripturian.EmbeddedScript;
import com.threecrickets.scripturian.EmbeddedScriptParsingHelper;
import com.threecrickets.scripturian.ScriptEngines;

/**
 * An {@link EmbeddedScriptParsingHelper} that supports Java as if it were a
 * scripting language.
 * 
 * @author Tal Liron
 */
@ScriptEngines("java")
public class JavaEmbeddedParsingHelper implements EmbeddedScriptParsingHelper
{
	//
	// EmbeddedScriptParsingHelper
	//

	public String getScriptHeader( EmbeddedScript embeddedScript, ScriptEngine scriptEngine )
	{
		return "class Text { private static javax.script.ScriptContext scriptContext; public static void setScriptContext(javax.script.ScriptContext sc) { scriptContext = sc; } public static void main(String arguments[]) throws Exception {";
	}

	public String getScriptFooter( EmbeddedScript embeddedScript, ScriptEngine scriptEngine )
	{
		return "}}";
	}

	public String getTextAsProgram( EmbeddedScript embeddedScript, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\"", "\\\\\"" );
		return "scriptContext.getWriter().write(\"" + content + "\");";
	}

	public String getExpressionAsProgram( EmbeddedScript embeddedScript, ScriptEngine scriptEngine, String content )
	{
		return "scriptContext.getWriter().write((" + content + ").toString());";
	}

	public String getExpressionAsInclude( EmbeddedScript embeddedScript, ScriptEngine scriptEngine, String content )
	{
		return embeddedScript.getScriptVariableName() + ".container.include((" + content + ").toString());";
	}

	public String getInvocationAsProgram( EmbeddedScript embeddedScript, ScriptEngine scriptEngine, String content )
	{
		return content + "();";
	}
}
