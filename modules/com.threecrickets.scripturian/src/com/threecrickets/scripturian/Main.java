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

import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.StackFrame;
import com.threecrickets.scripturian.file.DocumentFileSource;
import com.threecrickets.scripturian.internal.ExposedContainerForMain;

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
	 * Construction.
	 * 
	 * @param arguments
	 *        Supplied arguments (usually from a command line)
	 */
	public Main( String[] arguments )
	{
		this.arguments = arguments;

		manager = new LanguageManager();
		prepare = true;
		defaultDocumentName = "default";
		writer = new OutputStreamWriter( System.out );
		errorWriter = new OutputStreamWriter( System.err );
		documentSource = new DocumentFileSource<Executable>( new File( "." ), defaultDocumentName, -1 );
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

	//
	// Runnable
	//

	public void run()
	{
		String name;
		if( arguments.length > 0 )
			name = arguments[0];
		else
			name = defaultDocumentName;

		ExecutionContext executionContext = new ExecutionContext( manager, getWriter(), getErrorWriter() );
		try
		{
			ExposedContainerForMain container = new ExposedContainerForMain( this, executionContext );
			container.include( name );
			writer.flush();
			errorWriter.flush();
		}
		catch( IOException x )
		{
			System.err.print( "Error reading file \"" + name + "\": " );
			System.err.println( x.getMessage() );
		}
		catch( ParsingException x )
		{
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
	 * If the path to the document to run if not supplied as the first argument
	 * to {@link #main(String[])}, this is used instead.
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
}
