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

import com.threecrickets.scripturian.EmbeddedScriptParsingHelper;
import com.threecrickets.scripturian.ScriptEngines;
import com.threecrickets.scripturian.file.ScriptFileSource;

/**
 * An {@link EmbeddedScriptParsingHelper} that supports the PHP scripting
 * language as implemented by <a href="http://quercus.caucho.com/">Quercus</a>.
 * <p>
 * Note the peculiarity of the "include" implementation -- due to limitations of
 * the Quercus engine, it must use the internal PHP include. For this to work,
 * it is expected that a global variable named
 * {@link ScriptFileSource#basePathVariableName} be set to be the base path for
 * all includes.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"php", "quercus"
})
public class QuercusEmbeddedParsingHelper implements EmbeddedScriptParsingHelper
{
	//
	// EmbeddedParsingHelper
	//

	public String getScriptHeader( ScriptEngine scriptEngine )
	{
		return "<?php";
	}

	public String getScriptFooter( ScriptEngine scriptEngine )
	{
		return "?>";
	}

	public String getTextAsProgram( ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "print(\"" + content + "\");";
	}

	public String getExpressionAsProgram( ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	public String getExpressionAsInclude( ScriptEngine scriptEngine, String content )
	{
		return "include $" + ScriptFileSource.basePathVariableName + " . '/' . " + content + ";";
	}

	public String getInvocationAsProgram( ScriptEngine scriptEngine, String content )
	{
		return "<?php " + content + "(); ?>";
	}
}
