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
 * href="http://clojure.org/">Clojure</a> scripting language.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"Clojure"
})
public class ClojureScriptletHelper extends ScriptletHelper
{
	//
	// ScriptletHelper
	//

	@Override
	public boolean isMultiThreaded()
	{
		// Unfortunately, Clojure's JSR-223 support puts all global vars in the
		// same namespace. This means that multiple calling threads will
		// override each other's vars.
		return false;
	}

	@Override
	public boolean isCompilable()
	{
		// The developers of Clojure's JSR-223 support seem to have
		// misunderstood the use of the Compilable interface,
		// and have implemented it so that it expects a library name, rather
		// than plain Clojure code. We will have to disable support for it.
		return false;
	}

	@Override
	public String getTextAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "(print \"" + content + "\")";
	}

	@Override
	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "(print " + content + ")";
	}

	@Override
	public String getExpressionAsInclude( Document document, ScriptEngine scriptEngine, String content )
	{
		return "(.. " + document.getDocumentVariableName() + " getContainer (includeDocument " + content + "))";
	}
}
