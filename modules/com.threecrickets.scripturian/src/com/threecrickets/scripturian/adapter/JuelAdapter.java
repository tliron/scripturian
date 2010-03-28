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

package com.threecrickets.scripturian.adapter;

import javax.script.ScriptEngine;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.exception.LanguageInitializationException;

/**
 * An {@link LanguageAdapter} that supports the <a
 * href="http://juel.sourceforge.net/">JUEL</a> expression language.
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
	// ScriptletHelper
	//

	public JuelAdapter() throws LanguageInitializationException
	{
		super();
	}

	@Override
	public String getTextAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		return content;
	}

	@Override
	public String getExpressionAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		return "${" + content.trim() + "}";
	}

	@Override
	public String getExpressionAsInclude( Executable document, ScriptEngine scriptEngine, String content )
	{
		return null;
	}

	@Override
	public String getInvocationAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		return null;
	}
}
