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

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.internal.ExecutableSegment;
import com.threecrickets.scripturian.internal.ExposedExecutable;

/**
 * Handles the parsing, compilation, execution and invocation of executable
 * units in any supported languages.
 * <p>
 * Executables can be constructed in three modes:
 * <p>
 * <ul>
 * <li><b>Pure source code.</b> These are constructed when any of the
 * constructors is called with {@code isTextWithScriptlets} as false. The code
 * must be in a single programming language defined by {@code
 * defaultLanguageTag}.</li>
 * <li><b>Text with scriptlets.</b> These are constructed when any of the
 * constructors is called with {@code isTextWithScriptlets} as true. This
 * powerful mode supports executables with scriptlets in various languages,
 * in-flow scriptlets, and other features detailed below.</li>
 * <li><b>Pure text.</b> This is a trivial case of "text with scriptlets" -- no
 * scriptlets are included. If executed, the executable will simply output the
 * text. You can detect plain text executables via {@link #getAsPureText()} to
 * optimize for this edge case.</li>
 * </ul>
 * "Text with scriptlets" executable source code is a mix of text with
 * "scriptlets" -- source code embedded between special delimiters, with an
 * optional specification of which language to use. During construction, the
 * entire document is converted into code and optionally compiled. When the code
 * is executed, the non-scriptlet text segments are sent to output via whatever
 * method is appropriate for the language (see {@link LanguageAdapter}).
 * <p>
 * (The exception to this behavior is when the first segment is text -- in such
 * cases, just that first segment is sent to output from Java. This is just an
 * optimization.)
 * <p>
 * Source code can be provided by an implementation of {@link DocumentSource},
 * though you can use your own system.
 * <p>
 * "Text with scriptlets" is useful for building applications with a lot of
 * textual output -- for example, HTML-based web applications.
 * <p>
 * Executables can have scriptlets in multiple languages within the same source
 * code. You can specify a different language for each scriptlet in its opening
 * delimiter. If the language is not specified, whatever language was previously
 * used in the source code will be used. If no language was previously
 * specified, the {@code defaultLanguageTag} value from the constructor is used.
 * <p>
 * Two scriptlet delimiting styles are supported: JSP/ASP style (using
 * percentage signs), and the PHP style (using question marks). However, each
 * document must adhere to only one style throughout.
 * <p>
 * In addition to regular scriptlets, Scripturian supports a few shorthand
 * scriptlets for common tasks:
 * <p>
 * The expression scriptlet (with an equals sign) causes the expression to be
 * sent to standard output. It can allow for more compact and cleaner code.
 * <p>
 * The include scriptlet (with an ampersand) invokes the
 * <code>executable.container.include(name)</code> command as appropriate for
 * the language. Note that you need the "include" command to be supported by
 * your container environment.
 * <p>
 * Finally, the in-flow scriptlet (with a colon) works like a combination of
 * regular scriptlets with include scriptlets. Read the FAQ for more
 * information.
 * <p>
 * Examples:
 * <ul>
 * <li><b>JSP/ASP-style delimiters</b>: <code>&lt;% print('Hello World'); %&gt;</code></li>
 * <li><b>PHP-style delimiters</b>: <code>&lt;? document.cacheDuration.set 5000
 * ?&gt;</code></li>
 * <li><b>Specifying engine name</b>:
 * <code>&lt;%groovy print myVariable %&gt; &lt;?php $document->container->include(lib_name); ?&gt;</code>
 * </li>
 * <li><b>Output expression</b>: <code>&lt;?= 15 * 6 ?&gt;</code></li>
 * <li><b>Output expression with specifying engine name</b>:
 * <code>&lt;?=js sqrt(myVariable) ?&gt;</code></li>
 * <li><b>Include</b>: <code>&lt;%& 'library.js' %&gt; &lt;?& 'language-' + myObject.getLang + '-support.py' ?&gt;</code></li>
 * <li><b>In-flow</b>:
 * <code>&lt;%js if(isDebug) { %&gt; &lt;%:python dumpStack(); %&gt; &lt;% } %&gt;</code>
 * </li>
 * </ul>
 * <p>
 * A special container environment is exposed to your executable, with some
 * useful services. It is exposed to your executable as a global variable named
 * <code>executable</code> (this name can be changed via the
 * {@link #Executable(String, String, boolean, LanguageManager, String, DocumentSource, boolean, String, String, String, String, String, String, String, String)}
 * constructor).
 * <p>
 * Read-only attributes:
 * <ul>
 * <li><code>executable.container</code>: This is an arbitrary object set by the
 * executable's container environment for access to container-specific services.
 * It might be null if none was provided.</li>
 * <li><code>executable.context</code>: This is the {@link ExecutionContext}
 * used by the executable. Your code may use it to get access to the
 * {@link Writer} objects used for standard output and standard error.</li>
 * <li><code>executable.meta</code>: This {@link ConcurrentMap} provides a
 * convenient location for global values shared by all executables.</li>
 * </ul>
 * 
 * @author Tal Liron
 */
public class Executable
{
	//
	// Constants
	//

	/**
	 * The default start delimiter (first option): &lt;%
	 */
	public static final String DEFAULT_DELIMITER1_START = "<%";

	/**
	 * The default end delimiter (first option): %&gt;
	 */
	public static final String DEFAULT_DELIMITER1_END = "%>";

	/**
	 * The default start delimiter (second option): &lt;?
	 */
	public static final String DEFAULT_DELIMITER2_START = "<?";

	/**
	 * The default end delimiter (second option): ?&gt;
	 */
	public static final String DEFAULT_DELIMITER2_END = "?>";

	/**
	 * The default addition to the start delimiter to specify an expression tag:
	 * =
	 */
	public static final String DEFAULT_DELIMITER_EXPRESSION = "=";

	/**
	 * The default addition to the start delimiter to specify an include tag: &
	 */
	public static final String DEFAULT_DELIMITER_INCLUDE = "&";

	/**
	 * The default addition to the start delimiter to specify an in-flow tag: :
	 */
	public static final String DEFAULT_DELIMITER_IN_FLOW = ":";

	/**
	 * The default executable variable name: "executable"
	 */
	public static final String DEFAULT_EXECUTABLE_VARIABLE_NAME = "executable";

	//
	// Construction
	//

	/**
	 * Parses a text stream containing plain text and scriptlets into a compact,
	 * optimized document. Parsing requires the appropriate
	 * {@link LanguageAdapter} implementations to be installed for the script
	 * engines.
	 * 
	 * @param name
	 *        Name used for error messages
	 * @param sourceCode
	 *        The source code -- when {@code isTextWithScriptlets} is false,
	 *        it's considered as pure source code in the language defined by
	 *        {@code defaultLanguageTag}, otherwise it's considered as text with
	 *        embedded scriptlets
	 * @param isTextWithScriptlets
	 *        See {@code sourceCode} and {@code defaultLanguageTag}
	 * @param languageManager
	 *        The language manager used to parse and compile the executable
	 * @param defaultLanguageTag
	 *        When {@code isTextWithScriptlets} is true, this is the language
	 *        used for scriptlets if none is specified
	 * @param documentSource
	 *        A document source used to store in-flow scriptlets; can be null if
	 *        in-flow scriptlets are not used
	 * @param prepare
	 *        Whether to prepare the source code: preparation increases
	 *        initialization time and reduces execution time; note that not all
	 *        languages support preparation as a separate operation
	 * @throws ParsingException
	 *         In case of a parsing error
	 */
	public Executable( String name, String sourceCode, boolean isTextWithScriptlets, LanguageManager languageManager, String defaultLanguageTag, DocumentSource<Executable> documentSource, boolean prepare )
		throws ParsingException
	{
		this( name, sourceCode, isTextWithScriptlets, languageManager, defaultLanguageTag, documentSource, prepare, DEFAULT_EXECUTABLE_VARIABLE_NAME, DEFAULT_DELIMITER1_START, DEFAULT_DELIMITER1_END,
			DEFAULT_DELIMITER2_START, DEFAULT_DELIMITER2_END, DEFAULT_DELIMITER_EXPRESSION, DEFAULT_DELIMITER_INCLUDE, DEFAULT_DELIMITER_IN_FLOW );
	}

	/**
	 * Parses a text stream containing plain text and scriptlets into a compact,
	 * optimized document. Parsing requires the appropriate
	 * {@link LanguageAdapter} implementations to be installed for the script
	 * engines.
	 * 
	 * @param name
	 *        Name used for error messages
	 * @param sourceCode
	 *        The source code -- when {@code isTextWithScriptlets} is false,
	 *        it's considered as pure source code in the language defined by
	 *        {@code defaultLanguageTag}, otherwise it's considered as text with
	 *        embedded scriptlets
	 * @param isTextWithScriptlets
	 *        See {@code sourceCode} and {@code defaultLanguageTag}
	 * @param languageManager
	 *        The language manager used to parse and compile the executable
	 * @param defaultLanguageTag
	 *        When {@code isTextWithScriptlets} is true, this is the language
	 *        used for scriptlets if none is specified
	 * @param documentSource
	 *        A document source used to store in-flow scriptlets; can be null if
	 *        in-flow scriptlets are not used
	 * @param prepare
	 *        Whether to prepare the source code: preparation increases
	 *        initialization time and reduces execution time; note that not all
	 *        languages support preparation as a separate operation
	 * @param exposedExecutableName
	 *        The document variable name
	 * @param delimiter1Start
	 *        The start delimiter (first option)
	 * @param delimiter1End
	 *        The end delimiter (first option)
	 * @param delimiter2Start
	 *        The start delimiter (second option)
	 * @param delimiter2End
	 *        The end delimiter (second option)
	 * @param delimiterExpression
	 *        The default addition to the start delimiter to specify an
	 *        expression tag
	 * @param delimiterInclude
	 *        The default addition to the start delimiter to specify an include
	 *        scriptlet
	 * @param delimiterInFlow
	 *        The default addition to the start delimiter to specify an in-flow
	 *        scriptlet
	 * @throws ParsingException
	 *         In case of a parsing or compilation error
	 * @see LanguageAdapter
	 */
	public Executable( String name, String sourceCode, boolean isTextWithScriptlets, LanguageManager languageManager, String defaultLanguageTag, DocumentSource<Executable> documentSource, boolean prepare,
		String exposedExecutableName, String delimiter1Start, String delimiter1End, String delimiter2Start, String delimiter2End, String delimiterExpression, String delimiterInclude, String delimiterInFlow )
		throws ParsingException
	{
		this.documentName = name;
		this.exposedExecutableName = exposedExecutableName;

		if( !isTextWithScriptlets )
		{
			ExecutableSegment segment = new ExecutableSegment( sourceCode, 1, 1, true, defaultLanguageTag );
			segments = new ExecutableSegment[]
			{
				segment
			};
			segment.createScriptlet( this, languageManager, prepare );
			delimiterStart = null;
			delimiterEnd = null;
			return;
		}

		String lastLanguageTag = defaultLanguageTag;

		int delimiterStartLength = 0;
		int delimiterEndLength = 0;
		int expressionLength = delimiterExpression.length();
		int includeLength = delimiterInclude.length();
		int inFlowLength = delimiterInFlow.length();

		// Detect type of delimiter
		int start = sourceCode.indexOf( delimiter1Start );
		if( start != -1 )
		{
			delimiterStart = delimiter1Start;
			delimiterEnd = delimiter1End;
			delimiterStartLength = delimiterStart.length();
			delimiterEndLength = delimiterEnd.length();
		}
		else
		{
			start = sourceCode.indexOf( delimiter2Start );
			if( start != -1 )
			{
				delimiterStart = delimiter2Start;
				delimiterEnd = delimiter2End;
				delimiterStartLength = delimiterStart.length();
				delimiterEndLength = delimiterEnd.length();
			}
			else
			{
				// No delimiters used
				delimiterStart = null;
				delimiterEnd = null;
			}
		}

		int startLineNumber = 1;
		int startColumnNumber = 1;
		int lastLineNumber = 1;
		int lastColumnNumber = 1;

		if( start != -1 )
			for( int i = sourceCode.indexOf( '\n' ); i >= 0 && i < start; i = sourceCode.indexOf( '\n', i + 1 ) )
				startLineNumber++;

		List<ExecutableSegment> segments = new LinkedList<ExecutableSegment>();

		// Parse segments
		if( start != -1 )
		{
			int last = 0;

			while( start != -1 )
			{
				// Add previous non-script segment
				if( start != last )
					segments.add( new ExecutableSegment( sourceCode.substring( last, start ), lastLineNumber, lastColumnNumber, false, lastLanguageTag ) );

				start += delimiterStartLength;

				int end = sourceCode.indexOf( delimiterEnd, start );
				if( end == -1 )
					throw new ParsingException( documentName, startLineNumber, startColumnNumber, "Scriptlet does not have an ending delimiter" );

				if( start + 1 != end )
				{
					String languageTag = lastLanguageTag;

					boolean isExpression = false;
					boolean isInclude = false;
					boolean isInFlow = false;

					// Check if this is an expression
					if( sourceCode.substring( start, start + expressionLength ).equals( delimiterExpression ) )
					{
						start += expressionLength;
						isExpression = true;
					}
					// Check if this is an include
					else if( sourceCode.substring( start, start + includeLength ).equals( delimiterInclude ) )
					{
						start += includeLength;
						isInclude = true;
					}
					// Check if this is an in-flow
					else if( sourceCode.substring( start, start + inFlowLength ).equals( delimiterInFlow ) )
					{
						start += inFlowLength;
						isInFlow = true;
					}

					// Get language tag if available
					if( !Character.isWhitespace( sourceCode.charAt( start ) ) )
					{
						int endEngineName = start + 1;
						while( !Character.isWhitespace( sourceCode.charAt( endEngineName ) ) )
							endEngineName++;

						languageTag = sourceCode.substring( start, endEngineName );

						// Optimization: in-flow is unnecessary if we are in the
						// same language
						if( isInFlow && lastLanguageTag.equals( languageTag ) )
							isInFlow = false;

						start = endEngineName + 1;
					}

					if( start + 1 != end )
					{
						// Add scriptlet segment
						if( isExpression || isInclude )
						{
							LanguageAdapter adapter = languageManager.getAdapterByTag( languageTag );
							if( adapter == null )
								throw ParsingException.adapterNotFound( name, startLineNumber, startColumnNumber, languageTag );

							if( isExpression )
								segments.add( new ExecutableSegment( adapter.getSourceCodeForExpressionOutput( sourceCode.substring( start, end ), this ), startLineNumber, startColumnNumber, true, languageTag ) );
							else if( isInclude )
								segments.add( new ExecutableSegment( adapter.getSourceCodeForExpressionInclude( sourceCode.substring( start, end ), this ), startLineNumber, startColumnNumber, true, languageTag ) );
						}
						else if( isInFlow && ( documentSource != null ) )
						{
							LanguageAdapter adapter = languageManager.getAdapterByTag( languageTag );
							if( adapter == null )
								throw ParsingException.adapterNotFound( name, startLineNumber, startColumnNumber, languageTag );

							String inFlowCode = delimiterStart + languageTag + " " + sourceCode.substring( start, end ) + delimiterEnd;
							String inFlowName = IN_FLOW_PREFIX + inFlowCounter.getAndIncrement();

							// Note that the in-flow executable is a
							// single segment, so we can optimize parsing a
							// bit
							Executable inFlowExecutable = new Executable( name + "/" + inFlowName, inFlowCode, false, languageManager, null, null, prepare, exposedExecutableName, delimiterStart, delimiterEnd,
								delimiterStart, delimiterEnd, delimiterExpression, delimiterInclude, delimiterInFlow );
							documentSource.setDocument( inFlowName, inFlowCode, "", inFlowExecutable );

							// TODO: would it ever be possible to remove the
							// dependent in-flow instances?

							// Our include segment is in the last language
							segments.add( new ExecutableSegment( adapter.getSourceCodeForExpressionInclude( "'" + inFlowName + "'", this ), startLineNumber, startColumnNumber, true, lastLanguageTag ) );
						}
						else
							segments.add( new ExecutableSegment( sourceCode.substring( start, end ), startLineNumber, startColumnNumber, true, languageTag ) );
					}

					if( !isInFlow )
						lastLanguageTag = languageTag;
				}

				last = end + delimiterEndLength;
				lastLineNumber = startLineNumber;
				lastColumnNumber = startColumnNumber;
				start = sourceCode.indexOf( delimiterStart, last );
				if( start != -1 )
					for( int i = sourceCode.indexOf( '\n', last ); i >= 0 && i < start; i = sourceCode.indexOf( '\n', i + 1 ) )
						startLineNumber++;

				/*
				 * if( documentName.equals( "/test/clojure.html" ) ) if( start
				 * != -1 ) System.out.println( "" + startLineNumber + " " +
				 * sourceCode.substring( start, start + 10 ) );
				 * System.out.flush();
				 */
			}

			// Add remaining non-scriptlet segment
			if( last < sourceCode.length() )
				segments.add( new ExecutableSegment( sourceCode.substring( last ), lastLineNumber, lastColumnNumber, false, lastLanguageTag ) );
		}
		else
		{
			// Trivial executable: does not contain scriptlets
			this.segments = new ExecutableSegment[]
			{
				new ExecutableSegment( sourceCode, 1, 1, false, lastLanguageTag )
			};
			return;
		}

		// Collapse segments of same kind
		ExecutableSegment previous = null;
		ExecutableSegment current;
		for( Iterator<ExecutableSegment> i = segments.iterator(); i.hasNext(); )
		{
			current = i.next();

			if( previous != null )
			{
				if( previous.isScriptlet == current.isScriptlet )
				{
					if( current.languageTag.equals( previous.languageTag ) )
					{
						// Collapse current into previous
						i.remove();
						previous.startLineNumber = current.startLineNumber;
						previous.startColumnNumber = current.startColumnNumber;
						previous.sourceCode += current.sourceCode;
						current = previous;
					}
				}
			}

			previous = current;
		}

		// Collapse segments of same language (does not convert first segment
		// into scriptlet)
		previous = null;
		for( Iterator<ExecutableSegment> i = segments.iterator(); i.hasNext(); )
		{
			current = i.next();

			if( ( previous != null ) && previous.isScriptlet )
			{
				if( previous.languageTag.equals( current.languageTag ) )
				{
					// Collapse current into previous
					// (converting to scriptlet if necessary)
					i.remove();

					if( current.isScriptlet )
						previous.sourceCode += current.sourceCode;
					else
					{
						LanguageAdapter adapter = languageManager.getAdapterByTag( current.languageTag );
						if( adapter == null )
							throw ParsingException.adapterNotFound( name, current.startLineNumber, current.startColumnNumber, current.languageTag );

						previous.sourceCode += adapter.getSourceCodeForLiteralOutput( current.sourceCode, this );
					}

					current = previous;
				}
			}

			previous = current;
		}

		// Create scriptlets
		for( ExecutableSegment segment : segments )
			if( segment.isScriptlet )
				segment.createScriptlet( this, languageManager, prepare );

		// Flatten list into array
		this.segments = new ExecutableSegment[segments.size()];
		segments.toArray( this.segments );
	}

	//
	// Attributes
	//

	/**
	 * The document name, used for debugging and logging.
	 * 
	 * @return The document name
	 */
	public String getDocumentName()
	{
		return documentName;
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
	 * The start delimiter used.
	 * 
	 * @return The start delimiter, or null if none was used
	 * @see #getScriptletEndDelimiter()
	 */
	public String getScriptletStartDelimiter()
	{
		return delimiterStart;
	}

	/**
	 * The end delimiter used.
	 * 
	 * @return The end delimiter, or null if none was used
	 * @see #getScriptletStartDelimiter()
	 */
	public String getScriptletEndDelimiter()
	{
		return delimiterEnd;
	}

	/**
	 * The default variable name for the {@link ExposedExecutable} instance.
	 */
	public String getExposedExecutableName()
	{
		return exposedExecutableName;
	}

	/**
	 * Timestamp of when the executable last finished executing successfully, or
	 * 0 if it was never executed.
	 * 
	 * @return The timestamp or 0
	 */
	public long getLastExecutedTimestamp()
	{
		return lastExecuted;
	}

	/**
	 * Returns the entire source code in the trivial case of a
	 * "text with scriptlets" executable that contains no scriptlets.
	 * Identifying such documents can save you from making unnecessary calls to
	 * {@link #execute(boolean, ExecutionContext, Object, ExecutionController)}
	 * in some situations.
	 * 
	 * @return The soure code if it's pure text, null if not
	 */
	public String getAsPureText()
	{
		if( segments.length == 1 )
		{
			ExecutableSegment sole = segments[0];
			if( !sole.isScriptlet )
				return sole.sourceCode;
		}
		return null;
	}

	/**
	 * The execution context to be used for calls to
	 * {@link #invoke(String, Object, ExecutionController)}.
	 * 
	 * @return The execution context
	 */
	public ExecutionContext getExecutionContextForInvocations()
	{
		return executionContextForInvocationsReference.get();
	}

	//
	// Operations
	//

	/**
	 * Executes the executable.
	 * 
	 * @param checkIfExecutedBefore
	 *        Run only if we've never ran before -- this will affect the return
	 *        value
	 * @param executionContext
	 *        The execution context (can be null, in which case we will try to
	 *        default to the last used context of the executable, or the last
	 *        used context in the thread)
	 * @param container
	 *        The container (can be null)
	 * @param executionController
	 *        An optional {@link ExecutionController} to be applied to the
	 *        execution context
	 * @return True if execution happened, false if it didn't because
	 *         checkIfExecutedBefore was set to true and we've already executed
	 *         once
	 * @throws ParsingException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	public boolean execute( boolean checkIfExecutedBefore, ExecutionContext executionContext, Object container, ExecutionController executionController ) throws ParsingException, ExecutionException, IOException
	{
		if( checkIfExecutedBefore )
		{
			// TODO: ughghghghghghgh!!!!
			if( lastExecuted != 0 )
				return false;
		}

		if( executionContext == null )
			throw new ExecutionException( documentName, "Execute does not have an execution context" );

		if( executionController != null )
			executionController.initialize( executionContext );

		try
		{
			for( ExecutableSegment segment : segments )
			{
				if( !segment.isScriptlet )
					// Plain text
					executionContext.getWriter().write( segment.sourceCode );
				else
				{
					LanguageAdapter languageAdapter = executionContext.getManager().getAdapterByTag( segment.languageTag );
					if( languageAdapter == null )
						throw ParsingException.adapterNotFound( documentName, segment.startLineNumber, segment.startColumnNumber, segment.languageTag );

					executionContext.setAdapter( languageAdapter );

					if( !languageAdapter.isThreadSafe() )
						languageAdapter.getLock().lock();

					Object oldExposedExecutable = executionContext.getExposedVariables().put( exposedExecutableName, new ExposedExecutable( executionContext, container ) );

					try
					{
						segment.scriptlet.execute( executionContext );
					}
					finally
					{
						// Restore old document value (this is desirable for
						// documents that run other documents)
						if( oldExposedExecutable != null )
							executionContext.getExposedVariables().put( exposedExecutableName, oldExposedExecutable );

						if( !languageAdapter.isThreadSafe() )
							languageAdapter.getLock().unlock();
					}
				}
			}
		}
		finally
		{
			if( executionController != null )
				executionController.finalize( executionContext );
		}

		executionContextForInvocationsReference.set( executionContext );
		lastExecuted = System.currentTimeMillis();
		return true;
	}

	/**
	 * Invokes an entry point in the executable: a function, method, closure,
	 * etc., according to how the language handles invocations. Executing the
	 * script first (via
	 * {@link #execute(boolean, ExecutionContext, Object, ExecutionController)}
	 * ) is not absolutely required for this, but probably will be necessary in
	 * most useful scenarios, where running the script causes useful entry point
	 * to be initialized.
	 * <p>
	 * Note that this call does not support sending arguments to the method. If
	 * you need to pass data to the executable, expose it via the optional
	 * {@link ExecutionController}.
	 * <p>
	 * Concurrency note: The invoke mechanism allows for multi-threaded access,
	 * so it's the responsibility of your executable to be thread-safe. Also
	 * note that, internally, invoke relies on the {@link ExecutionContext} from
	 * {@link #getExecutionContextForInvocations()}. This is set to be the one
	 * used in the last call to
	 * {@link #execute(boolean, ExecutionContext, Object, ExecutionController)}
	 * 
	 * @param entryPointName
	 *        The name of the entry point
	 * @param container
	 *        The container (can be null)
	 * @param executionController
	 *        An optional {@link ExecutionController} to be applied to the
	 *        execution context
	 * @return The value returned by the invocation
	 * @throws ParsingException
	 * @throws ExecutionException
	 * @throws NoSuchMethodException
	 *         If the method is not found
	 */
	public Object invoke( String entryPointName, Object container, ExecutionController executionController ) throws ParsingException, ExecutionException, NoSuchMethodException
	{
		ExecutionContext executionContextForInvocations = executionContextForInvocationsReference.get();
		if( executionContextForInvocations == null )
			throw new ExecutionException( documentName, "Executable must be executed at least once before calling invoke" );

		LanguageAdapter languageAdapter = executionContextForInvocations.getAdapter();

		if( !languageAdapter.isThreadSafe() )
			languageAdapter.getLock().lock();

		Object oldExposedExecutable = executionContextForInvocations.getExposedVariables().put( exposedExecutableName, new ExposedExecutable( executionContextForInvocations, container ) );

		try
		{
			if( executionController != null )
				executionController.initialize( executionContextForInvocations );

			return languageAdapter.invoke( entryPointName, this, executionContextForInvocations );
		}
		finally
		{
			if( executionController != null )
				executionController.finalize( executionContextForInvocations );

			// Restore old exposed executable value (this is desirable for
			// executable that run other executables)
			if( oldExposedExecutable != null )
				executionContextForInvocations.getExposedVariables().put( exposedExecutableName, oldExposedExecutable );

			if( !languageAdapter.isThreadSafe() )
				languageAdapter.getLock().unlock();
		}
	}

	/**
	 * 
	 */
	public void release()
	{
		ExecutionContext executionContextForInvocations = executionContextForInvocationsReference.getAndSet( null );
		if( executionContextForInvocations != null )
			executionContextForInvocations.release();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final String IN_FLOW_PREFIX = "_IN_FLOW_";

	private static final AtomicInteger inFlowCounter = new AtomicInteger();

	private final String documentName;

	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	private final ExecutableSegment[] segments;

	private final String delimiterStart;

	private final String delimiterEnd;

	private final String exposedExecutableName;

	private volatile long lastExecuted = 0;

	private final AtomicReference<ExecutionContext> executionContextForInvocationsReference = new AtomicReference<ExecutionContext>();
}
