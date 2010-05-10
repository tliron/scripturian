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

import com.threecrickets.scripturian.document.DocumentFileSource;
import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.StackFrame;
import com.threecrickets.scripturian.internal.ExposedApplication;
import com.threecrickets.scripturian.internal.ExposedDocument;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * Delegates the main() call to an {@link Executable}, in effect using it as the
 * entry point of a Java platform application. The path to the document file can
 * be supplied as the first argument. If it's not supplied,
 * {@link #defaultDocumentName} is used instead.
 * <p>
 * The executable's standard output is directed to the system standard output.
 * Note that this output is not captured or buffered, and sent directly as the
 * executable runs.
 * <p>
 * A special container environment is exposed to the executable, with some
 * useful services. It is available to code as a global variable named
 * <code>executable.container</code>.
 * <p>
 * Operations:
 * <ul>
 * <li><code>executable.container.include(documentName)</code>: Executes another
 * executable, with the language determined according to the document's
 * extension. Note that the executed executable does not have to be in same
 * language as the executing executable.</li>
 * <li><code>executable.container.includeDocument(documentName)</code>: As
 * above, except that the included source code is parsed as a
 * "text-with-scriptlets" executable.</li>
 * </ul>
 * Read-only attributes:
 * <ul>
 * <li><code>executable.container.arguments</code>: An array of the string
 * arguments sent to {@link #main(String[])}</li>
 * </ul>
 * Modifiable attributes:
 * <ul>
 * <li><code>executable.container.defaultLanguageTag</code>: For use with
 * <code>executable.container.includeDocument(documentName)</code>, this is the
 * default language tag used for scriptlets in case none is specified. Defaults
 * to "js".</li>
 * </ul>
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
	 * or to {@link Main#defaultDocumentName} if not specified.
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

	/**
	 * Construction. Interprets initial values from the arguments or falls back
	 * to defaults.
	 * 
	 * @param arguments
	 *        Supplied arguments (usually from a command line)
	 */
	public Main( String[] arguments )
	{
		this.arguments = arguments;
		manager = new LanguageManager();
		prepare = ScripturianUtil.getSwitchArgument( "prepare", arguments, "false" ).equals( "true" );
		initialDocumentName = ScripturianUtil.getNonSwitchArgument( 0, arguments, "default" );
		defaultDocumentName = ScripturianUtil.getSwitchArgument( "default-document-name", arguments, "default" );
		exposedDocumentName = ScripturianUtil.getSwitchArgument( "exposed-document-name", arguments, "document" );
		exposedApplicationName = ScripturianUtil.getSwitchArgument( "exposed-application-name", arguments, "application" );
		writer = new OutputStreamWriter( System.out );
		errorWriter = new OutputStreamWriter( System.err );
		documentSource = new DocumentFileSource<Executable>( new File( ScripturianUtil.getSwitchArgument( "base-path", arguments, "." ) ), defaultDocumentName, -1 );
	}

	/**
	 * Construction.
	 * 
	 * @param manager
	 *        The language manager
	 * @param prepare
	 *        Whether to prepare the executables
	 * @param initialDocumentName
	 *        The name of the document to run
	 * @param defaultDocumentName
	 *        The name to use for document names that point to a directory
	 *        rather than a file.
	 * @param basePath
	 *        The base path for finding executable documents
	 * @param arguments
	 *        Supplied arguments (usually from a command line)
	 */
	public Main( LanguageManager manager, boolean prepare, String initialDocumentName, String defaultDocumentName, String basePath, String[] arguments )
	{
		this.arguments = arguments;
		this.manager = manager;
		this.prepare = prepare;
		this.initialDocumentName = initialDocumentName;
		this.defaultDocumentName = defaultDocumentName;
		writer = new OutputStreamWriter( System.out );
		errorWriter = new OutputStreamWriter( System.err );
		documentSource = new DocumentFileSource<Executable>( new File( basePath ), defaultDocumentName, -1 );
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
	 * The {@link LanguageManager} used to get language adapters for
	 * executables.
	 * 
	 * @return The language manager
	 */
	public LanguageManager getManager()
	{
		return manager;
	}

	/**
	 * Whether or not executables are prepared.
	 * 
	 * @return Whether to prepare executables.
	 */
	public boolean isPrepare()
	{
		return prepare;
	}

	/**
	 * If the path to the document to run if not supplied as the first argument
	 * to {@link #main(String[])}, this is used instead. Defaults to "instance".
	 * 
	 * @return The default document name
	 */
	public String getDefaultDocumentName()
	{
		return defaultDocumentName;
	}

	/**
	 * The writer to use for {@link ExecutionContext} instances.
	 * 
	 * @return The writer
	 */
	public Writer getWriter()
	{
		return writer;
	}

	/**
	 * The error writer to use for {@link ExecutionContext} instances.
	 * 
	 * @return The error writer
	 */
	public Writer getErrorWriter()
	{
		return errorWriter;
	}

	/**
	 * An optional {@link ExecutionController} to be used with executables.
	 * Useful for exposing your own global variables to executables.
	 * 
	 * @return The execution controller
	 * @see #setExecutionController(ExecutionController)
	 */
	public ExecutionController getExecutionController()
	{
		return executionController;
	}

	/**
	 * @param executionController
	 *        The execution controller
	 * @see #getExecutionController()
	 */
	public void setExecutionController( ExecutionController executionController )
	{
		this.executionController = executionController;
	}

	/**
	 * Used to load the executables. Defaults to a {@link DocumentFileSource}
	 * set for the current directory, with no validity checking.
	 * 
	 * @return The document source
	 * @see #setSource(DocumentSource)
	 */
	public DocumentSource<Executable> getSource()
	{
		return documentSource;
	}

	/**
	 * @param documentSource
	 *        The document source
	 * @see #getSource()
	 */
	public void setSource( DocumentSource<Executable> documentSource )
	{
		this.documentSource = documentSource;
	}

	/**
	 * The name of the document exposed to the executable.
	 * 
	 * @return The exposed name
	 */
	public String getExposedDocumentName()
	{
		return exposedDocumentName;
	}

	/**
	 * The name of the application exposed to the executable.
	 * 
	 * @return The exposed name
	 */
	public String getExposedApplicationName()
	{
		return exposedApplicationName;
	}

	//
	// Operations
	//

	/**
	 * Tries to flush writers.
	 * 
	 * @see #getWriter()
	 * @see #getErrorWriter()
	 */
	public void flushWriters()
	{
		try
		{
			getWriter().flush();
			getErrorWriter().flush();
		}
		catch( IOException x )
		{
		}
	}

	//
	// Runnable
	//

	public void run()
	{
		ExecutionContext executionContext = new ExecutionContext( getWriter(), getErrorWriter() );
		try
		{
			ExposedDocument exposedDocument = new ExposedDocument( this, executionContext );
			executionContext.getExposedVariables().put( getExposedDocumentName(), exposedDocument );
			executionContext.getExposedVariables().put( getExposedApplicationName(), new ExposedApplication( this ) );
			exposedDocument.execute( initialDocumentName );
			flushWriters();
		}
		catch( IOException x )
		{
			flushWriters();
			System.err.print( "Error reading file for \"" + initialDocumentName + "\": " );
			System.err.println( x.getMessage() );
		}
		catch( ParsingException x )
		{
			flushWriters();
			System.err.println( "Initialization error:" );
			System.err.println( " " + x.getMessage() );
			for( StackFrame stackFrame : x.getStack() )
			{
				System.err.println( "  Document: " + stackFrame.getDocumentName() );
				if( stackFrame.getLineNumber() >= 0 )
					System.err.println( "   Line: " + stackFrame.getLineNumber() );
				if( stackFrame.getColumnNumber() >= 0 )
					System.err.println( "   Column: " + stackFrame.getColumnNumber() );
			}
		}
		catch( ExecutionException x )
		{
			flushWriters();
			System.err.println( "Execution error:" );
			System.err.println( " " + x.getMessage() );
			for( StackFrame stackFrame : x.getStack() )
			{
				System.err.println( "  Document: " + stackFrame.getDocumentName() );
				if( stackFrame.getLineNumber() >= 0 )
					System.err.println( "   Line: " + stackFrame.getLineNumber() );
				if( stackFrame.getColumnNumber() >= 0 )
					System.err.println( "   Column: " + stackFrame.getColumnNumber() );
			}
		}
		finally
		{
			executionContext.release();
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * Supplied arguments (usually from a command line).
	 */
	private final String[] arguments;

	/**
	 * The {@link LanguageManager} used to get language adapters for
	 * executables.
	 */
	private final LanguageManager manager;

	/**
	 * Whether to prepare executables.
	 */
	private final boolean prepare;

	/**
	 * The name of the document to run.
	 */
	private final String initialDocumentName;

	/**
	 * The name to use for document names that point to a directory rather than
	 * a file.
	 */
	private final String defaultDocumentName;

	/**
	 * The writer to use for {@link ExecutionContext} instances.
	 */
	private final Writer writer;

	/**
	 * The error writer to use for {@link ExecutionContext} instances.
	 */
	private final Writer errorWriter;

	/**
	 * An optional {@link ExecutionController} to be used with executables.
	 */
	private volatile ExecutionController executionController;

	/**
	 * Used to load the executables.
	 */
	private volatile DocumentSource<Executable> documentSource;

	/**
	 * The name of the document exposed to the executable.
	 */
	private volatile String exposedDocumentName;

	/**
	 * The name of the application exposed to the executable.
	 */
	private volatile String exposedApplicationName;
}
