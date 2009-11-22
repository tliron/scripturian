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
 * An {@link ScriptletParsingHelper} that supports the <a
 * href="http://velocity.apache.org/">Velocity</a> templating language.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"velocity", "Velocity"
})
public class VelocityScriptletParsingHelper implements ScriptletParsingHelper
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
		return "#set($_d='$')#set($_h='#')";
	}

	public String getScriptletFooter( Document document, ScriptEngine scriptEngine )
	{
		return null;
	}

	public String getTextAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		// 
		// content = content.replaceAll( "\\#", "\\\\#" );
		// content = content.replaceAll( "\\$", "\\\\\\$" );

		content = content.replaceAll( "\\$", "\\${_d}" );
		content = content.replaceAll( "\\#", "\\${_h}" );
		return content;

		// content = content.replaceAll( "\\#end", "\n#end\n#literal()\n" );
		// content = content.replaceAll( "\\#\\#", "FIUSH" );
		// return "\n#literal()\n" + content + "\n#end\n";
	}

	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "${" + content.trim() + "}";
	}

	public String getExpressionAsInclude( Document document, ScriptEngine scriptEngine, String content )
	{
		return "#if($" + document.getDocumentVariableName() + ".container.includeDocument(" + content + "))#end ";
	}

	public String getInvocationAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return null;
	}
}
