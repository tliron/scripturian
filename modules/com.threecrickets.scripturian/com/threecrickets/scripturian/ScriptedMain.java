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

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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
 * "container". This name can be changed via {@link #containerVariableName},
 * though if you want the embedded script include tag to work, you must also set
 * {@link EmbeddedScript#containerVariableName} to be the same. For some other
 * global variables available to scripts, see {@link EmbeddedScript}.
 * <p>
 * Operations:
 * <ul>
 * <li><b>container.include(name)</b>: This powerful method allows scripts to
 * execute other scripts in place, and is useful for creating large,
 * maintainable applications based on scripts. Included scripts can act as a
 * library or toolkit and can even be shared among many applications. The
 * included script does not have to be in the same language or use the same
 * engine as the calling script. However, if they do use the same engine, then
 * methods, functions, modules, etc., could be shared. It is important to note
 * that how this works varies a lot per scripting platform. For example, in
 * JRuby, every script is run in its own scope, so that sharing would have to be
 * done explicitly in the global scope. See the included embedded Ruby script
 * example for a discussion of various ways to do this.</li>
 * <li><b>container.include(name, engineName)</b>: As the above, except that the
 * script is not embedded. As such, you must explicitly specify the name of the
 * scripting engine that should evaluate it.</li>
 * </ul>
 * Read-only attributes:
 * <ul>
 * <li><b>container.arguments</b>: The arguments sent to main().</li>
 * <li><b>container.writer</b>: Allows the script direct access to the
 * {@link Writer}. This should rarely be necessary, because by default the
 * standard output for your scripting engine would be directed to it, and the
 * scripting platform's native method for printing should be preferred. However,
 * some scripting platforms may not provide adequate access or may otherwise be
 * broken.</li>
 * <li><b>container.errorWriter</b>: Same as above, for standard error.</li>
 * <li><b>container.scriptEngineManager</b>: This is the
 * {@link ScriptEngineManager} used to create the script engine. Scripts may use
 * it to get information about what other engines are available.</li>
 * </ul>
 * <p>
 * In addition to the above, a {@link #scriptContextController} can be set to
 * add your own global variables to each embedded script.
 * 
 * @author Tal Liron
 */
public class ScriptedMain
{
	//
	// Static attributes
	//

	/**
	 * The {@link ScriptEngineManager} used to create the script engines for the
	 * scripts. Uses a default instance, but can be set to something else.
	 */
	public static ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

	/**
	 * Whether or not compilation is attempted for script engines that support
	 * it. Defaults to true.
	 */
	public static boolean allowCompilation = true;

	/**
	 * If the path to the script to run is not supplied as the first argument to
	 * {@link #main(String[])}, this is used instead, Defaults to "main.script".
	 */
	public static String defaultPath = "main.script";

	/**
	 * The default script engine name to be used if the script doesn't specify
	 * one. Defaults to "js".
	 */
	public static String defaultEngineName = "js";

	/**
	 * The default variable name for the {@link Container} instance. Defaults to
	 * "container".
	 */
	public static String containerVariableName = "container";

	/**
	 * An optional {@link ScriptContextController} to be used with the scripts.
	 * Useful for adding your own global variables to the script.
	 */
	public static ScriptContextController scriptContextController;

	//
	// Types
	//

	/**
	 * This is the type of the "container" variable exposed to the script. The
	 * name is set according to {@link ScriptedMain#containerVariableName}.
	 */
	public static class Container
	{
		//
		// Operations
		//

		/**
		 * This powerful method allows scripts to execute other scripts in
		 * place, and is useful for creating large, maintainable applications
		 * based on scripts. Included scripts can act as a library or toolkit
		 * and can even be shared among many applications. The included script
		 * does not have to be in the same language or use the same engine as
		 * the calling script. However, if they do use the same engine, then
		 * methods, functions, modules, etc., could be shared. It is important
		 * to note that how this works varies a lot per scripting platform. For
		 * example, in JRuby, every script is run in its own scope, so that
		 * sharing would have to be done explicitly in the global scope. See the
		 * included embedded Ruby script example for a discussion of various
		 * ways to do this.
		 * 
		 * @param name
		 *        The script name
		 * @throws IOException
		 * @throws ScriptException
		 */
		public void include( String name ) throws IOException, ScriptException
		{
			include( name, null );
		}

		/**
		 * As {@link #include(String)}, except that the script is not embedded.
		 * As such, you must explicitly specify the name of the scripting engine
		 * that should evaluate it.
		 * 
		 * @param name
		 *        The script name
		 * @param scriptEngineName
		 *        The script engine name (if null, behaves identically to
		 *        {@link #include(String)}
		 * @throws IOException
		 * @throws ScriptException
		 */
		public void include( String name, String scriptEngineName ) throws IOException, ScriptException
		{
			String text = EmbeddedScriptUtil.getString( new File( name ) );
			if( scriptEngineName != null )
				text = EmbeddedScript.delimiter1Start + scriptEngineName + " " + text + EmbeddedScript.delimiter1End;

			EmbeddedScript script = new EmbeddedScript( text, scriptEngineManager, defaultEngineName, allowCompilation );

			script.run( writer, errorWriter, scriptEngines, controller, false );
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
		 * Allows the script direct access to the {@link Writer}. This should
		 * rarely be necessary, because by default the standard output for your
		 * scripting engine would be directed to it, and the scripting
		 * platform's native method for printing should be preferred. However,
		 * some scripting platforms may not provide adequate access or may
		 * otherwise be broken.
		 * 
		 * @return The writer
		 */
		public Writer getWriter()
		{
			return writer;
		}

		/**
		 * Same as {@link #getWriter()}, for standard error.
		 * 
		 * @return The error writer
		 */
		public Writer getErrorWriter()
		{
			return errorWriter;
		}

		/**
		 * This is the {@link ScriptEngineManager} used to create the script
		 * engine. Scripts may use it to get information about what other
		 * engines are available.
		 * 
		 * @return The script engine manager
		 */
		public ScriptEngineManager getScriptEngineManager()
		{
			return scriptEngineManager;
		}

		// //////////////////////////////////////////////////////////////////////////
		// Private

		private final String[] arguments;

		private final Map<String, ScriptEngine> scriptEngines = new HashMap<String, ScriptEngine>();

		private final Writer writer = new OutputStreamWriter( System.out );

		private final Writer errorWriter = new OutputStreamWriter( System.err );

		private final Controller controller = new Controller();

		private Container( String[] arguments )
		{
			this.arguments = arguments;
		}

		private class Controller implements ScriptContextController
		{
			public void initialize( ScriptContext scriptContext ) throws ScriptException
			{
				scriptContext.setAttribute( containerVariableName, Container.this, ScriptContext.ENGINE_SCOPE );

				if( scriptContextController != null )
					scriptContextController.initialize( scriptContext );
			}

			public void finalize( ScriptContext scriptContext )
			{
				if( scriptContextController != null )
					scriptContextController.finalize( scriptContext );
			}
		}
	}

	//
	// Main
	//

	/**
	 * Delegates to an {@link EmbeddedScript} file specified by the first
	 * argument, or to {@link ScriptedMain#defaultPath} if not specified.
	 * 
	 * @param arguments
	 */
	public static void main( String[] arguments )
	{
		String name;
		if( arguments.length > 0 )
			name = arguments[0];
		else
			name = defaultPath;

		try
		{
			Container container = new Container( arguments );
			container.include( name );
			container.writer.flush();
			container.errorWriter.flush();
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
}
