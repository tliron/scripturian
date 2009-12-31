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

package com.threecrickets.scripturian;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.threecrickets.scripturian.file.DocumentFileSource;
import com.threecrickets.scripturian.internal.ExposedContainerForMainDocument;

/**
 * Delegates the main() call to a {@link Document}, in effect using it as the
 * entry point of a Java platform application. The path to the document file can
 * be supplied as the first argument. If it's not supplied, {@link #defaultName}
 * is used instead.
 * <p>
 * The scripting engine's standard output is directed to the system standard
 * output. Note that this output is not captured or buffered, and sent directly
 * as the document runs.
 * <p>
 * A special container environment is created for scriptlets, with some useful
 * services. It is available to code as a global variable named
 * <code>document.container</code>. For some other global variables, see
 * {@link Document}.
 * <p>
 * Operations:
 * <ul>
 * <li><code>document.container.includeDocument(name)</code>: This powerful
 * method allows documents to execute other documents in place, and is useful
 * for creating large, maintainable applications based on documents. Included
 * documents can act as a library or toolkit and can even be shared among many
 * applications. The included document does not have to be in the same
 * programming language or use the same engine as the calling code. However, if
 * they do use the same engine, then methods, functions, modules, etc., could be
 * shared.
 * <p>
 * It is important to note that how this works varies a lot per engine. For
 * example, in JRuby, every scriptlet is run in its own scope, so that sharing
 * would have to be done explicitly in the global scope. See the included JRuby
 * examples for a discussion of various ways to do this.
 * </li>
 * <li><code>document.container.include(name)</code>: As above, except that the
 * document is parsed as a single, non-delimited script with the engine name
 * derived from the name's extension.</li>
 * </ul>
 * Read-only attributes:
 * <ul>
 * <li><code>document.container.arguments</code>: An array of the string
 * arguments sent to {@link #main(String[])}</li>
 * </ul>
 * Modifiable attributes:
 * <ul>
 * <li><code>document.container.defaultEngineName</code>: The default script
 * engine name to be used if the first scriptlet doesn't specify one. Defaults
 * to "js". Scriptlets can change this value.</li>
 * </ul>
 * <p>
 * In addition to the above, a {@link #scriptletController} can be set to add
 * your own global variables to scriptlets.
 * 
 * @author Tal Liron
 */
public class MainDocument implements Runnable
{
	//
	// Static operations
	//

	/**
	 * Delegates to an {@link Document} file specified by the first argument, or
	 * to {@link MainDocument#defaultName} if not specified.
	 * 
	 * @param arguments
	 *        Supplied arguments (usually from a command line)
	 */
	public static void main( String[] arguments )
	{
		new MainDocument( arguments ).run();
	}

	//
	// Construction
	//

	public MainDocument( String[] arguments )
	{
		this.arguments = arguments;
		scriptEngineManager = new ScriptEngineManager();
		allowCompilation = false;
		defaultName = "default";
		writer = new OutputStreamWriter( System.out );
		errorWriter = new OutputStreamWriter( System.err );
		documentSource = new DocumentFileSource<Document>( new File( "." ), defaultName, -1 );
	}

	//
	// Attributes
	//

	/**
	 * The arguments sent to {@link MainDocument#main(String[])}.
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
	 * If the path to the document to run if not supplied as the first argument
	 * to {@link #main(String[])}, this is used instead, Defaults to
	 * "main.script".
	 * 
	 * @return The default path
	 */
	public String getDefaultPath()
	{
		return defaultName;
	}

	/**
	 * The writer to use for {@link Document}.
	 * 
	 * @return The writer
	 */
	public Writer getWriter()
	{
		return writer;
	}

	/**
	 * As {@link #getWriter()}, for error.
	 * 
	 * @return The error writer
	 */
	public Writer getErrorWriter()
	{
		return errorWriter;
	}

	/**
	 * An optional {@link ScriptletController} to be used with scriptlets.
	 * Useful for adding your own global variables to scriptlets.
	 * 
	 * @return The scriptlet controller
	 * @see #setScriptletController(ScriptletController)
	 */
	public ScriptletController getScriptletController()
	{
		return scriptletController;
	}

	/**
	 * @param scriptletController
	 * @see #getScriptletController()
	 */
	public void setScriptletController( ScriptletController scriptletController )
	{
		this.scriptletController = scriptletController;
	}

	/**
	 * Used to load the documents, Defaults to a {@link DocumentFileSource} set
	 * for the current directory, with no validity checking.
	 * 
	 * @return The document source
	 * @see #setDocumentSource(DocumentSource)
	 */
	public DocumentSource<Document> getDocumentSource()
	{
		return documentSource;
	}

	/**
	 * @param documentSource
	 *        The document source
	 * @see #getDocumentSource()
	 */
	public void setDocumentSource( DocumentSource<Document> documentSource )
	{
		this.documentSource = documentSource;
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
			name = defaultName;

		try
		{
			ExposedContainerForMainDocument container = new ExposedContainerForMainDocument( this );
			container.include( name );
			writer.flush();
			errorWriter.flush();
		}
		catch( IOException x )
		{
			System.err.println( "Error reading document file \"" + name + "\", error: " + x.getMessage() );
		}
		catch( ScriptException x )
		{
			System.err.println( "Error in scriptlet \"" + name + "\", error: " + x.getMessage() );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final String[] arguments;

	private final ScriptEngineManager scriptEngineManager;

	private final boolean allowCompilation;

	private final String defaultName;

	private final Writer writer;

	private final Writer errorWriter;

	private volatile ScriptletController scriptletController;

	private volatile DocumentSource<Document> documentSource;
}
