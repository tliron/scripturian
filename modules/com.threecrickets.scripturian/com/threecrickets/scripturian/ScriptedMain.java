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

package com.threecrickets.scripturian;

import java.io.IOException;
import java.io.Writer;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.threecrickets.scripturian.internal.ScriptedMainContainer;

/**
 * Delegates the main() call to an {@link EmbeddedScript}, in effect using a
 * script as the entry point to a Java platform application. The path to the
 * script file can be supplied as the first argument. If it's not supplied,
 * {@link #defaultPath} is used instead.
 * <p>
 * The scripting engine's standard output is directed to the system standard
 * output. Note that this output is not captured or buffered, and sent directly
 * as the script runs.
 * <p>
 * A special container environment is created for scripts, with some useful
 * services. It is available to the script as a global variable named
 * "container" (or anything else returned by
 * {@link EmbeddedScript#getContainerVariableName()}). For some other global
 * variables available to scripts, see {@link EmbeddedScript}.
 * <p>
 * Operations:
 * <ul>
 * <li><code>container.include(name)</code>: This powerful method allows scripts
 * to execute other scripts in place, and is useful for creating large,
 * maintainable applications based on scripts. Included scripts can act as a
 * library or toolkit and can even be shared among many applications. The
 * included script does not have to be in the same language or use the same
 * engine as the calling script. However, if they do use the same engine, then
 * methods, functions, modules, etc., could be shared. It is important to note
 * that how this works varies a lot per scripting platform. For example, in
 * JRuby, every script is run in its own scope, so that sharing would have to be
 * done explicitly in the global scope. See the included embedded Ruby script
 * example for a discussion of various ways to do this.</li>
 * <li><code>container.include(name, engineName)</code>: As the above, except
 * that the script is not embedded. As such, you must explicitly specify the
 * name of the scripting engine that should evaluate it.</li>
 * </ul>
 * Read-only attributes:
 * <ul>
 * <li><code>container.arguments</code>: An array of the string arguments sent
 * to {@link #main(String[])}</li>
 * <li><code>container.defaultScriptEngineName</code>: The default script engine
 * name to be used if the script doesn't specify one. Defaults to "js". Scripts
 * can change this value.</li>
 * <li><code>container.writer</code>: Allows the script direct access to the
 * {@link Writer}. This should rarely be necessary, because by default the
 * standard output for your scripting engine would be directed to it, and the
 * scripting platform's native method for printing should be preferred. However,
 * some scripting platforms may not provide adequate access or may otherwise be
 * broken.</li>
 * <li><code>container.errorWriter</code>: Same as above, for standard error.</li>
 * <li><code>container.scriptEngineManager</code>: This is the
 * {@link ScriptEngineManager} used to create the script engine. Scripts may use
 * it to get information about what other engines are available.</li>
 * </ul>
 * <p>
 * In addition to the above, a {@link #scriptContextController} can be set to
 * add your own global variables to each embedded script.
 * 
 * @author Tal Liron
 */
public class ScriptedMain implements Runnable
{
	//
	// Static operations
	//

	/**
	 * Delegates to an {@link EmbeddedScript} file specified by the first
	 * argument, or to {@link ScriptedMain#defaultPath} if not specified.
	 * 
	 * @param arguments
	 *        Supplied arguments (usually from a command line)
	 */
	public static void main( String[] arguments )
	{
		new ScriptedMain( arguments ).run();
	}

	//
	// Construction
	//

	public ScriptedMain( String[] arguments )
	{
		this.arguments = arguments;
		scriptEngineManager = new ScriptEngineManager();
		allowCompilation = false;
		defaultPath = "main.script";
		containerVariableName = "container";
	}

	//
	// Attributes
	//

	/**
	 * The arguments sent to {@link ScriptedMain#main(String[])}.
	 * 
	 * @return The arguments
	 */
	public String[] getArguments()
	{
		return arguments;
	}

	/**
	 * The {@link ScriptEngineManager} used to create the script engines for the
	 * scripts. Uses a default instance, but can be set to something else.
	 * 
	 * @return The script engine manager
	 */
	public ScriptEngineManager getScriptEngineManager()
	{
		return scriptEngineManager;
	}

	/**
	 * Whether or not compilation is attempted for script engines that support
	 * it. Defaults to false.
	 * 
	 * @return Whether to allow compilation
	 */
	public boolean isAllowCompilation()
	{
		return allowCompilation;
	}

	/**
	 * If the path to the script to run is not supplied as the first argument to
	 * {@link #main(String[])}, this is used instead, Defaults to "main.script".
	 * 
	 * @return The default path
	 */
	public String getDefaultPath()
	{
		return defaultPath;
	}

	/**
	 * The default variable name for the container instance. Defaults to
	 * "container".
	 * 
	 * @return The default container variable name
	 */
	public String getContainerVariableName()
	{
		return containerVariableName;
	}

	/**
	 * An optional {@link ScriptContextController} to be used with the scripts.
	 * Useful for adding your own global variables to the script.
	 * 
	 * @return The script context controller
	 * @see #setScriptContextController(ScriptContextController)
	 */
	public ScriptContextController getScriptContextController()
	{
		return scriptContextController;
	}

	/**
	 * @param scriptContextController
	 * @see #getScriptContextController()
	 */
	public void setScriptContextController( ScriptContextController scriptContextController )
	{
		this.scriptContextController = scriptContextController;
	}

	//
	// Runnable
	//

	public void run()
	{
		String name;
		if( arguments.length > 0 )
			name = arguments[0];
		else
			name = defaultPath;

		try
		{
			ScriptedMainContainer container = new ScriptedMainContainer( this );
			container.include( name );
			container.getWriter().flush();
			container.getErrorWriter().flush();
		}
		catch( IOException x )
		{
			System.err.println( "Error reading script file \"" + name + "\", error: " + x.getMessage() );
		}
		catch( ScriptException x )
		{
			System.err.println( "Error in script \"" + name + "\", error: " + x.getMessage() );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final String[] arguments;

	private final ScriptEngineManager scriptEngineManager;

	private final boolean allowCompilation;

	private final String defaultPath;

	private final String containerVariableName;

	private ScriptContextController scriptContextController;
}
