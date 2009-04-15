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
 * An {@link ScriptletParsingHelper} that supports the Ruby scripting
 * language as implemented by <a href="http://jruby.codehaus.org/">JRuby</a>.
 * <p>
 * Note that JRuby internally embeds each script in a "main" object, so that
 * methods defined therein cannot be accessible to us after the script runs,
 * unless they are explicitly stored in global variables. For this reason, in
 * order to make your methods invocable, they must be stored as closures in
 * global variables of the same name as the entry point. As so:
 * 
 * <pre>
 *  def myentry value
 *  	print value*3
 *  end
 *  $myentry = method :myentry
 * </pre>
 * 
 * Or even store raw lambdas:
 * 
 * <pre>
 *  $myentry = lambda do |value|
 *  	print value*3
 *  end
 * </pre>
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"ruby", "jruby"
})
public class JRubyScriptletParsingHelper implements ScriptletParsingHelper
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
		return null;
	}

	public String getScriptletFooter( CompositeScript compositeScript, ScriptEngine scriptEngine )
	{
		return null;
	}

	public String getTextAsProgram( CompositeScript compositeScript, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "print(\"" + content + "\");";
	}

	public String getExpressionAsProgram( CompositeScript compositeScript, ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	public String getExpressionAsInclude( CompositeScript compositeScript, ScriptEngine scriptEngine, String content )
	{
		return "$" + compositeScript.getScriptVariableName() + ".container.include(" + content + ");";
	}

	public String getInvocationAsProgram( CompositeScript compositeScript, ScriptEngine scriptEngine, String content )
	{
		// return "$invocable.send(:" + content + ");";
		return "$" + content + ".call;";
	}
}
