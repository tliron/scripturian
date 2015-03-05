/**
 * Copyright 2009-2015 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.threecrickets.scripturian.document.DocumentDescriptor;
import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.exception.DocumentException;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.service.ExecutableService;

/**
 * Executables are general-purpose operational units that are manifestations of
 * textual "source code" in any supported "language" (see
 * {@link LanguageAdapter} ). Outside of this definition, there is no real limit
 * to how executables are executed or what the language is. Execution can happen
 * in-process, out-of-process, or on a device somewhere in the network. A common
 * use case is to support various programming and templating languages that run
 * in the JVM, adapters for which are included in Scripturian. Another common
 * use case is for executing non-JVM services.
 * <p>
 * The primary design goal is to decouple the code asking for execution from the
 * execution's implementation, while providing clear, predictable concurrent
 * behavior. This abstraction thus lets you 1) plug in diverse execution
 * technologies into your code, 2) dynamically load and execute source code in
 * runtime.
 * <p>
 * Exact performance characteristics are left up to language implementations,
 * but the hope is that this architecture will allow for very high performance,
 * reusability of operational units, and scalability.
 * <p>
 * Source code can be conveniently provided by an implementation of
 * Scripturian's {@link DocumentSource}, which is designed for concurrent use,
 * though you can use any system you like.
 * <p>
 * Usage is divided into three phases: creation, execution and entry.
 * <p>
 * <b>1. Creation.</b> In this phase, the source code is parsed and possibly
 * otherwise analyzed for errors by the language implementation. The intent is
 * for the implementation to perform the bare minimum required for detecting
 * errors in the source code.
 * <p>
 * This phase supports an optional "preparation" sub-phase, with the intent of
 * speeding up usage of later phases at the expense of higher cost during
 * creation. It would be most useful if the executable is intended to be reused.
 * In many implementations, "preparation" would involve compiling the code, and
 * possibly caching the results on disk.
 * <p>
 * The creation phase supports a powerful "text-with-scriptlets" mode, in which
 * source code, wrapped in special delimiters, can be inserted into plain text.
 * "Scriptlets" written in several languages can be mixed into a single
 * executable. The plain text outside of the scriptlets is sent directly to the
 * {@link ExecutionContext} writer.
 * <p>
 * The "text-with-scriptlets" functionality is implemented entirely in this
 * class, and does not have to explicitly supported by language implementations.
 * <p>
 * <b>2. Execution.</b> This phase uses an {@link ExecutionContext} for passing
 * state between the user and the executable, as well as maintaining
 * implementation-specific state. Concurrent reuse is allowed as long as each
 * calling thread uses its own context.
 * <p>
 * <b>3. Entry.</b> This phase allows fine-grained execution via well-defined
 * "entry points" created by the executable during its execution phase.
 * Depending on the language implementation, entry can mean calling a function,
 * method, lambda, closure or macro, or even sending a network request. This
 * phase follows a special execution phase via a call to
 * {@link #makeEnterable(Object, ExecutionContext, Object, ExecutionController)}
 * , after which all entries use the same {@link ExecutionContext}. Passing
 * state is handled differently in entry vs. execution: in entry, support is for
 * sending a list of "argument" states and returning a single state value.
 * <p>
 * Depending on the language implementation, entry can involve better
 * performance than execution due to the use of a single execution context.
 * <h3>Parsing and Segments</h3>
 * <p>
 * During the creation phase, the entire source code document is converted into
 * pure source code. When the code is executed, the non-scriptlet text segments
 * are sent to output via whatever method is appropriate for the language (see
 * {@link LanguageAdapter}).
 * <p>
 * The exception to this behavior is when the first segment is text -- in such
 * cases, just that first segment is sent to output from Java. This is just an
 * optimization.
 * <p>
 * You can detect the trivial case of "text-with-scriptlets" in which no
 * scriptlets are used at all via {@link #getAsPureLiteral()}, and optimize
 * accordingly.
 * <p>
 * Executables can have scriptlets in multiple languages within the same source
 * code. You can specify a different language for each scriptlet in its opening
 * delimiter. If the language is not specified, whatever language was previously
 * used in the source code will be used. If no language was previously
 * specified, the {@code defaultLanguageTag} value from the constructor is used.
 * <p>
 * An <code>executable</code> service is exposed to executables for access to
 * this container environment. See {@link ExecutableService}.
 * 
 * @author Tal Liron
 */
public class Executable
{
	//
	// Constants
	//

	/**
	 * Prefix prepended to on-the-fly scriptlets stored in the document source.
	 */
	public static final String ON_THE_FLY_PREFIX = "_ON_THE_FLY_";

	//
	// Static operations
	//

	/**
	 * If the executable does not yet exist in the document source, retrieves
	 * the source code and parses it into a compact, optimized, executable.
	 * Parsing requires the appropriate {@link LanguageAdapter} implementations
	 * to be available in the language manager.
	 * 
	 * @param documentName
	 *        The document name
	 * @param parserName
	 *        The parser to use, or null for the default parser
	 * @param parsingContext
	 *        The parsing context
	 * @return A document descriptor with a valid executable as its document
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws DocumentException
	 *         In case of a document retrieval error
	 */
	public static DocumentDescriptor<Executable> createOnce( String documentName, String parserName, ParsingContext parsingContext ) throws ParsingException, DocumentException
	{
		DocumentDescriptor<Executable> documentDescriptor = parsingContext.getDocumentSource().getDocument( documentName );
		createOnce( documentDescriptor, parserName, parsingContext );
		return documentDescriptor;
	}

	/**
	 * If the executable does not yet exist in the document descriptor,
	 * retrieves the source code and parses it into a compact, optimized,
	 * executable. Parsing requires the appropriate {@link LanguageAdapter}
	 * implementations to be available in the language manager.
	 * 
	 * @param documentDescriptor
	 *        The document descriptor
	 * @param parserName
	 *        The parser to use, or null for the default parser
	 * @param parsingContext
	 *        The parsing context
	 * @return A new executable or the existing one
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws DocumentException
	 *         In case of a document retrieval error
	 */
	public static Executable createOnce( DocumentDescriptor<Executable> documentDescriptor, String parserName, ParsingContext parsingContext ) throws ParsingException, DocumentException
	{
		Executable executable = documentDescriptor.getDocument();
		if( executable == null )
		{
			String defaultLanguageTag = parsingContext.getLanguageManager().getLanguageTagByExtension( documentDescriptor.getDefaultName(), documentDescriptor.getTag(), parsingContext.getDefaultLanguageTag() );
			if( ( defaultLanguageTag != null ) && !defaultLanguageTag.equals( parsingContext.getDefaultLanguageTag() ) )
			{
				parsingContext = new ParsingContext( parsingContext );
				parsingContext.setDefaultLanguageTag( defaultLanguageTag );
			}

			executable = new Executable( documentDescriptor.getDefaultName(), documentDescriptor.getTimestamp(), documentDescriptor.getSourceCode(), parserName, parsingContext );
			Executable existing = documentDescriptor.setDocumentIfAbsent( executable );
			if( existing != null )
				executable = existing;
		}
		return executable;
	}

	/**
	 * Atomically creates a unique on-the-fly document name.
	 * 
	 * @return An on-the-fly document name
	 */
	public static String createOnTheFlyDocumentName()
	{
		return ON_THE_FLY_PREFIX + onTheFlyCounter.getAndIncrement();
	}

	//
	// Construction
	//

	/**
	 * Parses source code into a compact, optimized, executable. Parsing
	 * requires the appropriate {@link LanguageAdapter} implementations to be
	 * available in the language manager.
	 * 
	 * @param documentName
	 *        The document name
	 * @param documentTimestamp
	 *        The executable's document timestamp
	 * @param sourceCode
	 *        The source code
	 * @param parserName
	 *        The parser to use, or null for the default parser
	 * @param parsingContext
	 *        The parsing context
	 * @throws ParsingException
	 *         In case of a parsing error In case of a parsing or compilation
	 *         error
	 * @throws DocumentException
	 *         In case of a document retrieval error
	 * @see LanguageAdapter
	 */
	public Executable( String documentName, long documentTimestamp, String sourceCode, String parserName, ParsingContext parsingContext ) throws ParsingException, DocumentException
	{
		String partition = parsingContext.getPartition();
		if( ( partition == null ) && ( parsingContext.getDocumentSource() != null ) )
			partition = parsingContext.getDocumentSource().getIdentifier();

		this.documentName = documentName;
		this.partition = partition;
		this.documentTimestamp = documentTimestamp;
		this.executableServiceName = parsingContext.getExposedExecutableName();
		this.languageManager = parsingContext.getLanguageManager();

		if( parserName == null )
			parserName = parsingContext.getDefaultParser();

		// Find parser manager
		ParserManager parserManager = parsingContext.getParserManager();
		if( parserManager == null )
		{
			if( commonParserManager == null )
				commonParserManager = new ParserManager( Executable.class.getClassLoader() );
			parserManager = commonParserManager;
		}
		this.parserManager = parserManager;

		// Find parser
		Parser parser = parserManager.getParser( parserName );

		if( parser == null )
			throw new ParsingException( documentName, "Parser not found: " + parserName );

		Collection<ExecutableSegment> segments = parser.parse( sourceCode, parsingContext, this );

		// Flatten list into array
		this.segments = new ExecutableSegment[segments.size()];
		segments.toArray( this.segments );
	}

	//
	// Attributes
	//

	/**
	 * The executable's document name.
	 * 
	 * @return The document name
	 * @see #getPartition()
	 */
	public String getDocumentName()
	{
		return documentName;
	}

	/**
	 * The executable partition. It used in addition to the document name to
	 * calculate unique IDs for documents. Partitioning allows you have the same
	 * document name on multiple partitions.
	 * 
	 * @return The executable partition
	 * @see #getDocumentName()
	 */
	public String getPartition()
	{
		return partition;
	}

	/**
	 * The language manager used to parse, prepare and execute the executable.
	 * 
	 * @return The language manager
	 */
	public LanguageManager getLanguageManager()
	{
		return languageManager;
	}

	/**
	 * The parser manager used to parse the executable.
	 * 
	 * @return The parser manager
	 */
	public ParserManager getParserManager()
	{
		return parserManager;
	}

	/**
	 * User-defined attributes.
	 * 
	 * @return The attributes
	 */
	public ConcurrentMap<String, Object> getAttributes()
	{
		return attributes;
	}

	/**
	 * The default name for the {@link ExecutableService} instance.
	 * 
	 * @return The default executable service name
	 */
	public String getExecutableServiceName()
	{
		return executableServiceName;
	}

	/**
	 * The executable's document timestamp.
	 * 
	 * @return The timestamp
	 */
	public long getDocumentTimestamp()
	{
		return documentTimestamp;
	}

	/**
	 * Timestamp of when the executable last finished executing or entering
	 * successfully, or 0 if it was never executed or entered.
	 * 
	 * @return The timestamp or 0
	 */
	public long getLastUsedTimestamp()
	{
		return lastUsedTimestamp;
	}

	/**
	 * Returns the source code in the trivial case of a "text-with-scriptlets"
	 * executable that contains no scriptlets. Identifying such executables can
	 * save you from making unnecessary calls to
	 * {@link #execute(ExecutionContext, Object, ExecutionController)} in some
	 * situations.
	 * 
	 * @return The source code if it's pure literal text, null if not
	 */
	public String getAsPureLiteral()
	{
		if( segments.length == 1 )
		{
			ExecutableSegment sole = segments[0];
			if( !sole.isProgram )
				return sole.sourceCode;
		}
		return null;
	}

	/**
	 * The enterable execution context for an entering key.
	 * 
	 * @param enteringKey
	 *        The entering key
	 * @return The execution context
	 * @see #makeEnterable(Object, ExecutionContext, Object,
	 *      ExecutionController)
	 * @see ExecutionContext#enter(String, Object...)
	 */
	public ExecutionContext getEnterableExecutionContext( Object enteringKey )
	{
		return enterableExecutionContexts.get( enteringKey );
	}

	/**
	 * The container service stored in the context, if it was set.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The container service or null
	 * @see #execute(ExecutionContext, Object, ExecutionController)
	 */
	public Object getContainerService( ExecutionContext executionContext )
	{
		ExecutableService executableService = getExecutableService( executionContext );
		if( executableService != null )
			return executableService.getContainer();
		else
			return null;
	}

	//
	// Operations
	//

	/**
	 * Executes the executable.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws ExecutionException
	 *         In case of an execution error
	 * @throws IOException
	 *         In case of a writing error
	 */
	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException, IOException
	{
		execute( executionContext, null, null );
	}

	/**
	 * Executes the executable.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @param containerService
	 *        The optional container service
	 * @param executionController
	 *        The optional {@link ExecutionController} to be applied to the
	 *        execution context
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws ExecutionException
	 *         In case of an execution error
	 * @throws IOException
	 *         In case of a writing error
	 */
	public void execute( ExecutionContext executionContext, Object containerService, ExecutionController executionController ) throws ParsingException, ExecutionException, IOException
	{
		if( executionContext == null )
			throw new ExecutionException( documentName, "Execute does not have an execution context" );

		ExecutionContext oldExecutionContext = executionContext.makeCurrent();

		if( !executionContext.isImmutable() && executionController != null )
			executionController.initialize( executionContext );

		Object oldExecutableService = null;
		if( !executionContext.isImmutable() )
			oldExecutableService = executionContext.getServices().put( executableServiceName, new ExecutableService( executionContext, languageManager, parserManager, containerService ) );

		try
		{
			for( ExecutableSegment segment : segments )
			{
				if( !segment.isProgram )
					// Literal
					executionContext.getWriter().write( segment.sourceCode );
				else
				{
					LanguageAdapter adapter = languageManager.getAdapterByTag( segment.languageTag );
					if( adapter == null )
						throw ParsingException.adapterNotFound( documentName, segment.startLineNumber, segment.startColumnNumber, segment.languageTag );

					if( !executionContext.isImmutable() )
						executionContext.addAdapter( adapter );

					if( !adapter.isThreadSafe() )
						adapter.getLock().lock();

					try
					{
						segment.program.execute( executionContext );
					}
					catch( ParsingException x )
					{
						x.setExectable( this );
						throw x;
					}
					catch( ExecutionException x )
					{
						x.setExectable( this );
						throw x;
					}
					finally
					{
						if( !adapter.isThreadSafe() )
							adapter.getLock().unlock();
					}

					if( !executionContext.isImmutable() )
						executionContext.addAdapter( adapter );
				}
			}
		}
		finally
		{
			if( !executionContext.isImmutable() && oldExecutableService != null )
				executionContext.getServices().put( executableServiceName, oldExecutableService );

			if( !executionContext.isImmutable() && executionController != null )
				executionController.release( executionContext );

			if( oldExecutionContext != null )
				oldExecutionContext.makeCurrent();
		}

		lastUsedTimestamp = System.currentTimeMillis();
	}

	/**
	 * Executes the executable with the current execution context.
	 * 
	 * @param containerService
	 *        The optional container service
	 * @param executionController
	 *        The optional {@link ExecutionController} to be applied to the
	 *        execution context
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws ExecutionException
	 *         In case of an execution error
	 * @throws IOException
	 *         In case of a writing error
	 * @see ExecutionContext#getCurrent()
	 */
	public void executeInThread( Object containerService, ExecutionController executionController ) throws ParsingException, ExecutionException, IOException
	{
		execute( ExecutionContext.getCurrent(), containerService, executionController );
	}

	/**
	 * Makes an execution context enterable, in preparation for calling
	 * {@link ExecutionContext#enter(String, Object...)}.
	 * <p>
	 * Note that this can only be done once per entering key for an executable.
	 * If it succeeds and returns true, the execution context should be
	 * considered "consumed" by this executable. At this point it is immutable,
	 * and can only be released by calling {@link #release()} on the executable.
	 * 
	 * @param enteringKey
	 *        The entering key
	 * @param executionContext
	 *        The execution context
	 * @return False if we're already enterable and the execution context was
	 *         not consumed, true if the operation succeeded and execution
	 *         context was consumed
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws ExecutionException
	 *         In case of an execution error
	 * @throws IOException
	 *         In case of a writing error
	 */
	public boolean makeEnterable( Object enteringKey, ExecutionContext executionContext ) throws ParsingException, ExecutionException, IOException
	{
		return makeEnterable( enteringKey, executionContext, null, null );
	}

	/**
	 * Makes an execution context enterable, in preparation for calling
	 * {@link ExecutionContext#enter(String, Object...)}.
	 * <p>
	 * Note that this can only be done once per entering key for an executable.
	 * If it succeeds and returns true, the execution context should be
	 * considered "consumed" by this executable. At this point it is immutable,
	 * and can only be released by calling {@link #release()} on the executable.
	 * 
	 * @param enteringKey
	 *        The entering key
	 * @param executionContext
	 *        The execution context
	 * @param containerService
	 *        The optional container service
	 * @param executionController
	 *        The optional {@link ExecutionController} to be applied to the
	 *        execution context
	 * @return False if we're already enterable and the execution context was
	 *         not consumed, true if the operation succeeded and execution
	 *         context was consumed
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws ExecutionException
	 *         In case of an execution error
	 * @throws IOException
	 *         In case of a writing error
	 */
	public boolean makeEnterable( Object enteringKey, ExecutionContext executionContext, Object containerService, ExecutionController executionController ) throws ParsingException, ExecutionException, IOException
	{
		if( executionContext.enterableExecutable != null )
			throw new IllegalStateException( "Execution context was already made enterable for another executable" );

		execute( executionContext, containerService, executionController );

		if( enterableExecutionContexts.putIfAbsent( enteringKey, executionContext ) != null )
			return false;

		executionContext.enterableExecutable = this;
		executionContext.enteringKey = enteringKey;
		executionContext.makeImmutable();
		return true;
	}

	/**
	 * Enters the executable at a stored, named location, via the last language
	 * adapter that used the enterable context. According to the language, the
	 * entry point can be a function, method, lambda, closure, etc.
	 * <p>
	 * An execution context must have been previously made enterable by a call
	 * to
	 * {@link #makeEnterable(Object, ExecutionContext, Object, ExecutionController)}
	 * .
	 * 
	 * @param enteringKey
	 *        The entering key
	 * @param entryPointName
	 *        The name of the entry point
	 * @param arguments
	 *        Optional state to pass to the entry point
	 * @return State returned from the entry point or null
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws ExecutionException
	 *         In case of an execution error
	 * @throws NoSuchMethodException
	 *         In case the entry point does not exist
	 * @see ExecutionContext#enter(String, Object...)
	 */
	public Object enter( Object enteringKey, String entryPointName, Object... arguments ) throws ParsingException, ExecutionException, NoSuchMethodException
	{
		ExecutionContext enterableExecutionContext = enterableExecutionContexts.get( enteringKey );
		if( enterableExecutionContext == null )
			throw new IllegalStateException( "Executable does not have an enterable execution context for key: " + enteringKey );

		if( enterableExecutionContext.enterableExecutable != this )
			throw new ExecutionException( documentName, "Attempted to enter executable using an uninitialized execution context" );

		Object r = enterableExecutionContext.enter( entryPointName, arguments );
		lastUsedTimestamp = System.currentTimeMillis();
		return r;
	}

	/**
	 * Releases consumed execution contexts.
	 * 
	 * @see #makeEnterable(Object, ExecutionContext, Object,
	 *      ExecutionController)
	 * @see #finalize()
	 */
	public void release()
	{
		for( ExecutionContext enterableExecutionContext : enterableExecutionContexts.values() )
			enterableExecutionContext.release();
	}

	//
	// Object
	//

	@Override
	public String toString()
	{
		return "Executable: " + documentName + ", " + partition + ", " + documentTimestamp;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	/**
	 * The execution contexts to be used for calls to
	 * {@link #enter(Object, String, Object...)}.
	 * 
	 * @see #makeEnterable(Object, ExecutionContext, Object,
	 *      ExecutionController)
	 */
	protected final ConcurrentMap<Object, ExecutionContext> enterableExecutionContexts = new ConcurrentHashMap<Object, ExecutionContext>();

	//
	// Object
	//

	@Override
	protected void finalize()
	{
		release();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * Used to ensure unique names for on-the-fly scriptlets.
	 */
	private static final AtomicInteger onTheFlyCounter = new AtomicInteger();

	/**
	 * A shared parser manager used when none is specified.
	 */
	private static volatile ParserManager commonParserManager;

	/**
	 * The executable's partition.
	 */
	private final String partition;

	/**
	 * The executable's document name.
	 */
	private final String documentName;

	/**
	 * The executable's document timestamp.
	 */
	private final long documentTimestamp;

	/**
	 * The language manager used to parse, prepare and execute the executable.
	 */
	private final LanguageManager languageManager;

	/**
	 * The parser manager used to parse the executable.
	 */
	private final ParserManager parserManager;

	/**
	 * User-defined attributes.
	 */
	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	/**
	 * The segments, which can be programs, scriptlets or plain-text.
	 */
	private final ExecutableSegment[] segments;

	/**
	 * The default name for the {@link ExecutableService} instance.
	 */
	private final String executableServiceName;

	/**
	 * Timestamp of when the executable last finished executing or entering
	 * successfully, or 0 if it was never executed or entered.
	 */
	private volatile long lastUsedTimestamp = 0;

	/**
	 * Get the exposed service for the executable.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The executable service
	 * @see #getExecutableServiceName()
	 */
	private ExecutableService getExecutableService( ExecutionContext executionContext )
	{
		return (ExecutableService) executionContext.getServices().get( executableServiceName );
	}
}
