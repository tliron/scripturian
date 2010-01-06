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
import com.threecrickets.scripturian.annotation.ScriptEngines;

/**
 * An {@link ScriptletParsingHelper} that supports the <a
 * href="http://clojure.org/">Clojure</a> scripting language.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"Clojure"
})
public class ClojureScriptletParsingHelper implements ScriptletParsingHelper
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
		// The developers of Clojure's JSR-223 support seem to have
		// misunderstood the use of the Compilable interface,
		// and have implemented it so that it expects a library name, rather
		// than plain Clojure code. We will have to disable support for it.
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
		return "(print \"" + content + "\")";
	}

	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "(print " + content + ")";
	}

	public String getExpressionAsInclude( Document document, ScriptEngine scriptEngine, String content )
	{
		return "(.. " + document.getDocumentVariableName() + " getContainer (includeDocument " + content + "))";
	}

	public String getInvocationAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return null;
	}
}
