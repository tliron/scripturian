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
import javax.script.ScriptException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.exception.LanguageAdapterException;

/**
 * A {@link LanguageAdapter} that supports the Python language as implemented by
 * <a href="http://jepp.sourceforge.net/">Jepp</a> via its JSR-223 scripting
 * engine.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"jep", "jepp"
})
public class JeppAdapter extends Jsr223LanguageAdapter
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 */
	public JeppAdapter() throws LanguageAdapterException
	{
		super();
	}

	//
	// Jsr223LanguageAdapter
	//

	@Override
	public String getScriptletHeader( Executable executable, ScriptEngine scriptEngine )
	{
		// We're changing the __builtins__.__import__ to re-enable import, which
		// was disabled in Jepp 2.4.

		// Apparently the Java Scripting support for Jepp does not correctly
		// set global variables, not redirect stdout and stderr. Luckily, the
		// Python interface is compatible with Java's Writer interface, so we
		// can redirect them explicitly.
		return "from jep import *;__builtins__.__import__=jep.jimport;import sys;sys.stdout=context.getWriter();sys.stderr=context.getErrorWriter();";
	}

	@Override
	public void beforeCall( ScriptEngine scriptEngine, ExecutionContext executionContext )
	{
		StringBuilder r = new StringBuilder();
		for( String var : executionContext.getServices().keySet() )
		{
			r.append( var );
			r.append( "=context.getAttribute('" );
			r.append( var );
			r.append( "');" );
		}

		try
		{
			scriptEngine.eval( r.toString() );
		}
		catch( ScriptException x )
		{
		}
	}

	@Override
	public String getSourceCodeForLiteralOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "sys.stdout.write(\"" + content + "\"),;";
	}

	@Override
	public String getSourceCodeForExpressionOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		return "sys.stdout.write(str(" + content + "));";
	}

	@Override
	public String getSourceCodeForExpressionInclude( Executable executable, ScriptEngine scriptEngine, String content )
	{
		String containerIncludeCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_COMMAND_ATTRIBUTE );
		return executable.getExecutableServiceName() + ".getContainer()." + containerIncludeCommand + "(" + content + ");";
	}
}
