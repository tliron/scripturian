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
 * An {@link ScriptletParsingHelper} that supports the Ruby scripting language
 * as implemented by <a href="http://jruby.codehaus.org/">JRuby</a>.
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

	public String getScriptletHeader( Document document, ScriptEngine scriptEngine )
	{
		return null;
	}

	public String getScriptletFooter( Document document, ScriptEngine scriptEngine )
	{
		return null;
	}

	public String getTextAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "print(\"" + content + "\");";
	}

	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	public String getExpressionAsInclude( Document document, ScriptEngine scriptEngine, String content )
	{
		return "$" + document.getDocumentVariableName() + ".container.include(" + content + ");";
	}

	public String getInvocationAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		// return "$invocable.send(:" + content + ");";
		return "$" + content + ".call;";
	}
}
