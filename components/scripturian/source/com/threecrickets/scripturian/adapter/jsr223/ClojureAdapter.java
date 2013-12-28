/**
 * Copyright 2009-2014 Three Crickets LLC.
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
 * href="http://clojure.org/">Clojure</a> language via its JSR-223 scripting
 * engine.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"Clojure", "clojure"
})
public class ClojureAdapter extends Jsr223LanguageAdapter
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 */
	public ClojureAdapter() throws LanguageAdapterException
	{
		super();
	}

	//
	// Jsr223LanguageAdapter
	//

	@Override
	public boolean isThreadSafe()
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
	public String getSourceCodeForLiteralOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "(print \"" + content + "\")";
	}

	@Override
	public String getSourceCodeForExpressionOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		return "(print " + content + ")";
	}

	@Override
	public String getSourceCodeForExpressionInclude( Executable executable, ScriptEngine scriptEngine, String content )
	{
		String containerIncludeCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_COMMAND_ATTRIBUTE );
		return "(.. " + executable.getExecutableServiceName() + " getContainer (" + containerIncludeCommand + " " + content + "))";
	}
}
