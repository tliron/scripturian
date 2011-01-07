/**
 * Copyright 2009-2011 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.adapter.jsr223;

import javax.script.ScriptEngine;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.exception.LanguageAdapterException;

/**
 * A {@link LanguageAdapter} that supports the <a
 * href="http://juel.sourceforge.net/">JUEL</a> language via its JSR-223
 * scripting engine.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"juel"
})
public class JuelAdapter extends Jsr223LanguageAdapter
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @throws LanguageAdapterException
	 */
	public JuelAdapter() throws LanguageAdapterException
	{
		super();
	}

	//
	// Jsr223LanguageAdapter
	//

	@Override
	public String getSourceCodeForLiteralOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		return content;
	}

	@Override
	public String getSourceCodeForExpressionOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		return "${" + content.trim() + "}";
	}

	@Override
	public String getSourceCodeForExpressionInclude( Executable executable, ScriptEngine scriptEngine, String content )
	{
		return null;
	}

	@Override
	public String getInvocationAsProgram( Executable executable, ScriptEngine scriptEngine, String content, Object... arguments )
	{
		return null;
	}
}
