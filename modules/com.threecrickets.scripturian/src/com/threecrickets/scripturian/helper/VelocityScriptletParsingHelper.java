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

import com.threecrickets.scripturian.CompositeScript;
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

	public String getScriptletHeader( CompositeScript compositeScript, ScriptEngine scriptEngine )
	{
		return "#set($_d='$')#set($_h='#')";
	}

	public String getScriptletFooter( CompositeScript compositeScript, ScriptEngine scriptEngine )
	{
		return null;
	}

	public String getTextAsProgram( CompositeScript compositeScript, ScriptEngine scriptEngine, String content )
	{
		// 
		//content = content.replaceAll( "\\#", "\\\\#" );
		//content = content.replaceAll( "\\$", "\\\\\\$" );
		
		content = content.replaceAll( "\\$", "\\${_d}" );
		content = content.replaceAll( "\\#", "\\${_h}" );
		return content;
		
		//content = content.replaceAll( "\\#end", "\n#end\n#literal()\n" );
		//content = content.replaceAll( "\\#\\#", "FIUSH" );
		//return "\n#literal()\n" + content + "\n#end\n";
	}

	public String getExpressionAsProgram( CompositeScript compositeScript, ScriptEngine scriptEngine, String content )
	{
		return "${" + content.trim() + "}";
	}

	public String getExpressionAsInclude( CompositeScript compositeScript, ScriptEngine scriptEngine, String content )
	{
		return "#if($" + compositeScript.getScriptVariableName() + ".container.include(" + content + "))#end ";
	}

	public String getInvocationAsProgram( CompositeScript compositeScript, ScriptEngine scriptEngine, String content )
	{
		return null;
	}
}
