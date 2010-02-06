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
 * An {@link ScriptletHelper} that supports the <a
 * href="http://velocity.apache.org/">Velocity</a> templating language.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"velocity", "Velocity"
})
public class VelocityScriptletHelper extends ScriptletHelper
{
	//
	// ScriptletHelper
	//

	@Override
	public String getScriptletHeader( Document document, ScriptEngine scriptEngine )
	{
		return "#set($_d='$')#set($_h='#')";
	}

	@Override
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

	@Override
	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "${" + content.trim() + "}";
	}

	@Override
	public String getExpressionAsInclude( Document document, ScriptEngine scriptEngine, String content )
	{
		return "#if($" + document.getDocumentVariableName() + ".container.includeDocument(" + content + "))#end ";
	}
}
