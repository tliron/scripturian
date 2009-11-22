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
 * An {@link ScriptletParsingHelper} that supports the Python scripting language
 * as implemented by <a href="http://www.jython.org/">Jython</a>.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"python", "jython"
})
public class JythonScriptletParsingHelper implements ScriptletParsingHelper
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
		String version = scriptEngine.getFactory().getEngineVersion();
		String[] split = version.split( "\\." );
		int major = Integer.parseInt( split[0] );
		int minor = Integer.parseInt( split[1] );
		if( ( major >= 2 ) && ( minor >= 5 ) )
			return "import sys;";
		else
			// Apparently the Java Scripting support for Jython (version 2.2.1)
			// does not correctly redirect stdout and stderr. Luckily, the
			// Python interface is compatible with Java's Writer interface, so
			// we can redirect them explicitly.
			return "import sys;sys.stdout=context.writer;sys.stderr=context.errorWriter;";
	}

	public String getScriptletFooter( Document document, ScriptEngine scriptEngine )
	{
		return null;
	}

	public String getTextAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "sys.stdout.write(\"" + content + "\"),;";
	}

	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "sys.stdout.write(" + content + ");";
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
