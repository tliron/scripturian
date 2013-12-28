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

package com.threecrickets.scripturian;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.threecrickets.scripturian.document.DocumentFileSource;
import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.exception.DocumentException;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.StackFrame;
import com.threecrickets.scripturian.internal.ScripturianUtil;
import com.threecrickets.scripturian.service.ApplicationService;
import com.threecrickets.scripturian.service.DocumentService;
import com.threecrickets.scripturian.service.ExecutableService;
import com.threecrickets.scripturian.service.Shell;

/**
 * Delegates the main() call to an {@link Executable}, in effect using it as the
 * entry point of a JVM application. The path to the document file can be
 * supplied as the first argument. If it's not supplied,
 * {@link #defaultDocumentName} is used instead.
 * <p>
 * The executable's standard output is directed to the system standard output.
 * Note that this output is not captured or buffered, and sent directly as the
 * executable runs.
 * <p>
 * <code>executable</code>, <code>document</code>, and <code>application</code>
 * are available as global services to executables. See
 * {@link ExecutableService}, {@link DocumentService} and
 * {@link ApplicationService}.
 * <p>
 * 
 * @author Tal Liron
 */
public class Main implements Shell, Runnable
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
	 * Constructor. Interprets initial values from the arguments or falls back
	 * to defaults.
	 * 
	 * @param arguments
	 *        Supplied arguments (usually from a command line)
	 */
	public Main( String[] arguments )
	{
		languageManager = new LanguageManager();
		parserManager = new ParserManager();
		prepare = ScripturianUtil.getSwitchArgument( "prepare", arguments, "true" ).equals( "true" );
		initialDocumentName = ScripturianUtil.getNonSwitchArgument( 0, arguments, "default" );
		defaultDocumentName = ScripturianUtil.getSwitchArgument( "default-document-name", arguments, "default" );
		documentServiceName = ScripturianUtil.getSwitchArgument( "document-service-name", arguments, "document" );
		applicationServiceName = ScripturianUtil.getSwitchArgument( "application-service-name", arguments, "application" );
		String basePath = ScripturianUtil.getSwitchArgument( "base-path", arguments, "" );
		String preferredExtension = ScripturianUtil.getSwitchArgument( "preferred-extension", arguments, "js" );
		writer = new OutputStreamWriter( System.out );
		errorWriter = new OutputStreamWriter( System.err );
		documentSource = new DocumentFileSource<Executable>( new File( basePath ), defaultDocumentName, preferredExtension, -1 );
		this.arguments = arguments;
	}

	/**
	 * Constructor.
	 * 
	 * @param languageManager
	 *        The language manager
	 * @param parserManager
	 *        The parser manager
	 * @param basePath
	 *        The base path for the document source
	 * @param prepare
	 *        Whether to prepare the executables
	 * @param initialDocumentName
	 *        The name of the document to run
	 * @param defaultDocumentName
	 *        The name to use for document names that point to a directory
	 *        rather than a file.
	 * @param preferredExtension
	 *        An extension to prefer if more than one file with the same name is
	 *        in a directory
	 * @param documentServiceName
	 *        The name of the document service exposed to the executable
	 * @param applicationServiceName
	 *        The name of the application service exposed to the executable
	 * @param writer
	 *        The writer used for {@link ExecutionContext} instances
	 * @param errorWriter
	 *        The error used for {@link ExecutionContext} instances
	 * @param arguments
	 *        Supplied arguments (usually from a command line)
	 */
	public Main( LanguageManager languageManager, ParserManager parserManager, File basePath, boolean prepare, String initialDocumentName, String defaultDocumentName, String preferredExtension,
		String documentServiceName, String applicationServiceName, Writer writer, Writer errorWriter, String[] arguments )
	{
		this.languageManager = languageManager;
		this.parserManager = parserManager;
		this.prepare = prepare;
		this.initialDocumentName = initialDocumentName;
		this.defaultDocumentName = defaultDocumentName;
		this.documentServiceName = documentServiceName;
		this.applicationServiceName = applicationServiceName;
		this.writer = writer;
		this.errorWriter = errorWriter;
		this.arguments = arguments;
		documentSource = new DocumentFileSource<Executable>( basePath, defaultDocumentName, preferredExtension, -1 );
	}

	//
	// Attributes
	//

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
	 * @param executionController
	 *        The execution controller
	 * @see #getExecutionController()
	 */
	public void setExecutionController( ExecutionController executionController )
	{
		this.executionController = executionController;
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
	 * The name of the document service exposed to the executable.
	 * 
	 * @return The exposed service name
	 */
	public String getDocumentServiceName()
	{
		return documentServiceName;
	}

	/**
	 * The name of the application service exposed to the executable.
	 * 
	 * @return The exposed service name
	 */
	public String getApplicationServiceName()
	{
		return applicationServiceName;
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
	// Shell
	//

	public String[] getArguments()
	{
		return arguments;
	}

	public Logger getLogger()
	{
		return logger;
	}

	public void setLogger( Logger logger )
	{
		this.logger = logger;
	}

	public LanguageManager getLanguageManager()
	{
		return languageManager;
	}

	public ParserManager getParserManager()
	{
		return parserManager;
	}

	public boolean isPrepare()
	{
		return prepare;
	}

	public DocumentSource<Executable> getSource()
	{
		return documentSource;
	}

	public CopyOnWriteArrayList<DocumentSource<Executable>> getLibrarySources()
	{
		return librarySources;
	}

	public ExecutionController getExecutionController()
	{
		return executionController;
	}

	//
	// Runnable
	//

	public void run()
	{
		ExecutionContext executionContext = new ExecutionContext( getWriter(), getErrorWriter() );
		try
		{
			DocumentService documentService = new DocumentService( this, executionContext );
			executionContext.getServices().put( getDocumentServiceName(), documentService );
			executionContext.getServices().put( getApplicationServiceName(), new ApplicationService( this ) );
			documentService.execute( initialDocumentName );
			flushWriters();
		}
		catch( DocumentException x )
		{
			flushWriters();
			System.err.print( "Error reading document for \"" + initialDocumentName + "\": " );
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
		catch( IOException x )
		{
			flushWriters();
			System.err.print( "I/O error in \"" + initialDocumentName + "\": " );
			System.err.println( x.getMessage() );
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
	private final LanguageManager languageManager;

	/**
	 * The {@link ParserManager} used to get parsers for executables.
	 */
	private final ParserManager parserManager;

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
	 * The additional document sources to use.
	 */
	private final CopyOnWriteArrayList<DocumentSource<Executable>> librarySources = new CopyOnWriteArrayList<DocumentSource<Executable>>();

	/**
	 * The name of the document service exposed to the executable.
	 */
	private volatile String documentServiceName;

	/**
	 * The name of the application service exposed to the executable.
	 */
	private volatile String applicationServiceName;

	/**
	 * The logger.
	 */
	private volatile Logger logger = Logger.getAnonymousLogger();
}
