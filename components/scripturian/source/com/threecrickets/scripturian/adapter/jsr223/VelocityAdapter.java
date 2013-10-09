/**
 * Copyright 2009-2013 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.adapter.jsr223;

import javax.script.ScriptEngine;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.exception.LanguageAdapterException;

/**
 * A {@link LanguageAdapter} that supports the <a
 * href="http://velocity.apache.org/">Velocity</a> language via its JSR-223
 * scripting engine.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"velocity"
})
public class VelocityAdapter extends Jsr223LanguageAdapter
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 */
	public VelocityAdapter() throws LanguageAdapterException
	{
		super();
	}

	//
	// Jsr223LanguageAdapter
	//

	@Override
	public String getScriptletHeader( Executable executable, ScriptEngine scriptEngine )
	{
		return "#set($_d='$')#set($_h='#')";
	}

	@Override
	public String getSourceCodeForLiteralOutput( Executable executable, ScriptEngine scriptEngine, String content )
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
	public String getSourceCodeForExpressionOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		return "${" + content.trim() + "}";
	}

	@Override
	public String getSourceCodeForExpressionInclude( Executable executable, ScriptEngine scriptEngine, String content )
	{
		String containerIncludeExpressionCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_EXPRESSION_COMMAND_ATTRIBUTE );
		return "#if($" + executable.getExecutableServiceName() + ".container." + containerIncludeExpressionCommand + "(" + content + "))#end ";
	}
}
