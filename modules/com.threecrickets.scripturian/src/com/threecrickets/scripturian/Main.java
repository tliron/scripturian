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

package com.threecrickets.scripturian;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.script.ScriptEngineManager;

import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.exception.ExecutableInitializationException;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.StackFrame;
import com.threecrickets.scripturian.file.DocumentFileSource;
import com.threecrickets.scripturian.internal.ExposedContainerForMain;

/**
 * Delegates the main() call to a {@link Executable}, in effect using it as the
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
 * {@link Executable}.
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
 * In addition to the above, a {@link #executionController} can be set to add
 * your own global variables to scriptlets.
 * 
 * @author Tal Liron
 */
public class Main implements Runnable
{
	//
	// Static operations
	//

	/**
	 * Delegates to an {@link Executable} file specified by the first argument,
	 * or to {@link Main#defaultName} if not specified.
	 * 
	 * @param arguments
	 *        Supplied arguments (usually from a command line)
	 */
	public static void main( String[] arguments )
	{
		new Main( arguments ).run();
	}

	//
	// Construction
	//

	public Main( String[] arguments )
	{
		this.arguments = arguments;

		// Fixes problems with JRuby
		// System.setProperty( "org.jruby.embed.localcontext.scope",
		// "threadsafe" );
		//System.setProperty( "org.jruby.embed.localcontext.scope", "singlethread" );

		manager = new LanguageManager();
		allowCompilation = false;
		defaultName = "default";
		writer = new OutputStreamWriter( System.out );
		errorWriter = new OutputStreamWriter( System.err );
		documentSource = new DocumentFileSource<Executable>( new File( "." ), defaultName, -1 );
	}

	//
	// Attributes
	//

	/**
	 * The arguments sent to {@link Main#main(String[])}.
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
	public LanguageManager getManager()
	{
		return manager;
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
	 * The writer to use for {@link Executable}.
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
	 * An optional {@link ExecutionController} to be used with scriptlets.
	 * Useful for adding your own global variables to scriptlets.
	 * 
	 * @return The scriptlet controller
	 * @see #setScriptletController(ExecutionController)
	 */
	public ExecutionController getScriptletController()
	{
		return executionController;
	}

	/**
	 * @param executionController
	 * @see #getScriptletController()
	 */
	public void setScriptletController( ExecutionController executionController )
	{
		this.executionController = executionController;
	}

	/**
	 * Used to load the documents, Defaults to a {@link DocumentFileSource} set
	 * for the current directory, with no validity checking.
	 * 
	 * @return The document source
	 * @see #setDocumentSource(DocumentSource)
	 */
	public DocumentSource<Executable> getDocumentSource()
	{
		return documentSource;
	}

	/**
	 * @param documentSource
	 *        The document source
	 * @see #getDocumentSource()
	 */
	public void setDocumentSource( DocumentSource<Executable> documentSource )
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
			ExposedContainerForMain container = new ExposedContainerForMain( this );
			container.include( name );
			writer.flush();
			errorWriter.flush();
		}
		catch( IOException x )
		{
			System.err.print( "Error reading document file \"" + name + "\": " );
			System.err.println( x.getMessage() );
		}
		catch( ExecutableInitializationException x )
		{
			System.err.print( "Init error: " );
			System.err.println( x.getMessage() );
		}
		catch( ExecutionException x )
		{
			System.err.print( "Run error: " );
			System.err.println( " " + x.getMessage() );
			for( StackFrame stackFrame : x.getStack() )
			{
				System.err.println( "  Document: " + stackFrame.getName() );
				if( stackFrame.getLineNumber() >= 0 )
					System.err.println( "   Line: " + stackFrame.getLineNumber() );
				if( stackFrame.getColumnNumber() >= 0 )
					System.err.println( "   Column: " + stackFrame.getColumnNumber() );
			}
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final String[] arguments;

	private final LanguageManager manager;

	private final boolean allowCompilation;

	private final String defaultName;

	private final Writer writer;

	private final Writer errorWriter;

	private volatile ExecutionController executionController;

	private volatile DocumentSource<Executable> documentSource;
}
