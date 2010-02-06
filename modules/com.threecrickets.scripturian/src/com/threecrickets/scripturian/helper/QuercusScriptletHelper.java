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
 * An {@link ScriptletHelper} that supports the PHP scripting language as
 * implemented by <a href="http://quercus.caucho.com/">Quercus</a>.
 * <p>
 * Note the peculiarity of the "include" implementation -- due to limitations of
 * the Quercus engine, it must use the internal PHP include. For this to work,
 * it is expected that a variable under
 * <code>documentnt.container.source.basePath</code> be set to the base path for
 * all includes.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"quercus", "php"
})
public class QuercusScriptletHelper extends ScriptletHelper
{
	//
	// ScriptletHelper
	//

	@Override
	public String getScriptletHeader( Document document, ScriptEngine scriptEngine )
	{
		return "<?php";
	}

	@Override
	public String getScriptletFooter( Document document, ScriptEngine scriptEngine )
	{
		return "?>";
	}

	@Override
	public String getTextAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "print(\"" + content + "\");";
	}

	@Override
	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	@Override
	public String getExpressionAsInclude( Document document, ScriptEngine scriptEngine, String content )
	{
		return "include $" + document.getDocumentVariableName() + "->container->source->basePath . '/' . " + content + ";";
	}

	@Override
	public String getInvocationAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "<?php " + content + "(); ?>";
	}
}
