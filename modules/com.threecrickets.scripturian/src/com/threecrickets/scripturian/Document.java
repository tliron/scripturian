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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.threecrickets.scripturian.exception.DocumentInitializationException;
import com.threecrickets.scripturian.exception.DocumentRunException;
import com.threecrickets.scripturian.internal.DocumentSegment;
import com.threecrickets.scripturian.internal.ExposedDocument;

/**
 * Handles the parsing, optional compilation and running of Scripturian
 * documents.
 * <p>
 * Scripturian documents are text streams containing a mix of plain text and
 * "scriptlets" -- script code embedded between special delimiters with an
 * optional specification of which engine to use. During the parsing stage,
 * which happens only once in the constructor, the entire document is converted
 * into code and optionally compiled. When the code is run, the non-delimited
 * plain text segments are sent to output via whatever method is appropriate for
 * the scripting engine (see {@link ScriptletHelper}).
 * <p>
 * The exception to this is documents beginning with plain text -- in such
 * cases, just that first section is sent directly to your specified output. The
 * reasons for this are two: first, that no script engine has been specified
 * yet, so we can't be sure which to use, and second, that sending in directly
 * is probably a bit faster than sending via a scriptlet. If the entire text
 * stream is just plain text, it is simply sent directly to output without
 * invoking any scriptlet. In such cases, {@link #getTrivial()} would return the
 * content of the text stream.
 * <p>
 * Documents are often provided by an implementation of {@link DocumentSource},
 * though your environment can use its own system.
 * <p>
 * Scripturian documents can be abused, becoming hard to read and hard to
 * maintain, since both code and plain text are mixed together. However, they
 * can boost productivity in environments which mostly just output plain text.
 * For example, in an application that dynamically generates HTML web pages, it
 * is likely that most files will be in HTML with only some parts of some files
 * containing scriptlets. In such cases, it is easy to have a professional web
 * designer work on the HTML parts while the scriptlets are reserved for the
 * programmer. Web design software can often recognize scriptlets and take care
 * to keep it safe while the web designer makes changes to the file.
 * <p>
 * Scripturian documents can support scriptlets in multiple languages and
 * engines within the same text. Each scriptlet can specify which engine it uses
 * explicitly at the opening delimiter. If the engine is not specified, whatever
 * engine was previously used in the file will be used. If no engine was
 * previously specified, a default value is used (supplied in the constructor).
 * <p>
 * Two scriptlet delimiting styles are supported: JSP/ASP style (using
 * percentage signs), and the PHP style (using question marks). However, each
 * document must adhere to only one style throughout.
 * <p>
 * In addition to scriptlets, which are regular code, Scriptuarian documents
 * support a few shorthands for common scriptlet tasks.
 * <p>
 * The expression tag causes the expression to be sent to standard output. It
 * can allow for more compact and cleaner code.
 * <p>
 * The include tag invokes the <code>document.container.include(name)</code>
 * command as appropriate for the engine. Note that you need this command to be
 * supported by your container environment.
 * <p>
 * Finally, the in-flow tag works exactly like an include, but lets you place
 * the included script into the flow of the current script. The inclusion is
 * applied to the previously used script engine, which is then restored to being
 * the default engine after the in-flow tag. This construct allows for very
 * powerful and clean mixing of scripting engines, without cumbersome creation
 * of separate scripts for inclusion.
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
 * A special container environment is created for scripts, with some useful
 * services. It is available to the script as a global variable named
 * <code>document</code> (this name can be changed via the
 * {@link #Document(String, String, boolean, ScriptEngineManager, String, DocumentSource, boolean)}
 * constructor).
 * <p>
 * Read-only attributes:
 * <ul>
 * <li><code>document.container</code>: This is an arbitrary object set by the
 * document's container environment for access to container-specific services.
 * It might be null if none was provided.</li>
 * <li><code>document.context</code>: This is the {@link ScriptContext} used by
 * the document. Scriptlets may use it to get access to the {@link Writer}
 * objects used for standard output and standard error.</li>
 * <li><code>document.engine</code>: This is the {@link ScriptEngine} used by
 * the scriptlet. Scriptlets may use it to get information about the engine's
 * capabilities.</li>
 * <li><code>document.engineManager</code>: This is the
 * {@link ScriptEngineManager} used to create all script engines in the
 * document. Scriptlets may use it to get information about what other engines
 * are available.</li>
 * <li><code>document.meta</code>: This {@link ConcurrentMap} provides a
 * convenient location for global values shared by all scriptlets in all
 * documents.</li>
 * </ul>
 * Modifiable attributes:
 * <ul>
 * <li><code>document.cacheDuration</code>: Setting this to something greater
 * than 0 enables caching of the document's output for a maximum number of
 * milliseconds. By default cacheDuration is 0, so that each request causes the
 * document to be run. This class does not handle caching itself. Caching can be
 * provided by your environment if appropriate.</li>
 * </ul>
 * 
 * @author Tal Liron
 */
public class Document
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
	 * The default document variable name: "document"
	 */
	public static final String DEFAULT_DOCUMENT_VARIABLE_NAME = "document";

	//
	// Construction
	//

	/**
	 * Parses a text stream containing plain text and scriptlets into a compact,
	 * optimized document. Parsing requires the appropriate
	 * {@link ScriptletHelper} implementations to be installed for the script
	 * engines.
	 * 
	 * @param name
	 *        Name used for error messages
	 * @param text
	 *        The text stream
	 * @param isScript
	 *        If true, the text will be parsed as a single script for
	 *        defaultEngineName
	 * @param scriptEngineManager
	 *        The script engine manager used to create script engines
	 * @param defaultEngineName
	 *        If a script engine name isn't explicitly specified in the first
	 *        scriptlet, this one will be used
	 * @param documentSource
	 *        The document source (used for in-flow tags)
	 * @param allowCompilation
	 *        Whether script segments will be compiled (note that compilation
	 *        will only happen if the script engine supports it, and that what
	 *        compilation exactly means is left up to the script engine)
	 * @throws DocumentInitializationException
	 *         In case of a parsing error
	 */
	public Document( String name, String text, boolean isScript, ScriptEngineManager scriptEngineManager, String defaultEngineName, DocumentSource<Document> documentSource, boolean allowCompilation )
		throws DocumentInitializationException
	{
		this( name, text, isScript, scriptEngineManager, defaultEngineName, documentSource, allowCompilation, DEFAULT_DOCUMENT_VARIABLE_NAME, DEFAULT_DELIMITER1_START, DEFAULT_DELIMITER1_END, DEFAULT_DELIMITER2_START,
			DEFAULT_DELIMITER2_END, DEFAULT_DELIMITER_EXPRESSION, DEFAULT_DELIMITER_INCLUDE, DEFAULT_DELIMITER_IN_FLOW );
	}

	/**
	 * Parses a text stream containing plain text and scriptlets into a compact,
	 * optimized document. Parsing requires the appropriate
	 * {@link ScriptletHelper} implementations to be installed for the script
	 * engines.
	 * 
	 * @param name
	 *        Name used for error messages
	 * @param text
	 *        The text stream
	 * @param isScript
	 *        If true, the text will be parsed as a single script for
	 *        defaultEngineName
	 * @param scriptEngineManager
	 *        The script engine manager used to create script engines
	 * @param defaultEngineName
	 *        If a script engine name isn't explicitly specified in the first
	 *        scriptlet, this one will be used
	 * @param documentSource
	 *        The document source (used for in-flow tags)
	 * @param allowCompilation
	 *        Whether scriptlets will be compiled (note that compilation will
	 *        only happen if the script engine supports it, and that what
	 *        compilation exactly means is left up to the script engine)
	 * @param documentVariableName
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
	 *        tag
	 * @param delimiterInFlow
	 *        The default addition to the start delimiter to specify an in-flow
	 *        tag
	 * @throws DocumentInitializationException
	 *         In case of a parsing error
	 * @see ScriptletHelper
	 */
	public Document( String name, String text, boolean isScript, ScriptEngineManager scriptEngineManager, String defaultEngineName, DocumentSource<Document> documentSource, boolean allowCompilation,
		String documentVariableName, String delimiter1Start, String delimiter1End, String delimiter2Start, String delimiter2End, String delimiterExpression, String delimiterInclude, String delimiterInFlow )
		throws DocumentInitializationException
	{
		this.name = name;
		this.documentVariableName = documentVariableName;

		if( isScript )
		{
			DocumentSegment segment = new DocumentSegment( text, true, defaultEngineName );
			segments = new DocumentSegment[]
			{
				segment
			};
			segment.resolve( this, scriptEngineManager, allowCompilation );
			delimiterStart = null;
			delimiterEnd = null;
			return;
		}

		String lastScriptEngineName = defaultEngineName;

		int delimiterStartLength = 0;
		int delimiterEndLength = 0;
		int expressionLength = delimiterExpression.length();
		int includeLength = delimiterInclude.length();
		int inFlowLength = delimiterInFlow.length();

		// Detect type of delimiter
		int start = text.indexOf( delimiter1Start );
		if( start != -1 )
		{
			delimiterStart = delimiter1Start;
			delimiterEnd = delimiter1End;
			delimiterStartLength = delimiterStart.length();
			delimiterEndLength = delimiterEnd.length();
		}
		else
		{
			start = text.indexOf( delimiter2Start );
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

		List<DocumentSegment> segments = new LinkedList<DocumentSegment>();

		// Parse segments
		if( start != -1 )
		{
			int last = 0;

			while( start != -1 )
			{
				// Add previous non-script segment
				if( start != last )
					segments.add( new DocumentSegment( text.substring( last, start ), false, lastScriptEngineName ) );

				start += delimiterStartLength;

				int end = text.indexOf( delimiterEnd, start );
				if( end == -1 )
					throw new RuntimeException( "Script block does not have an ending delimiter" );

				if( start + 1 != end )
				{
					String scriptEngineName = lastScriptEngineName;

					boolean isExpression = false;
					boolean isInclude = false;
					boolean isInFlow = false;

					// Check if this is an expression
					if( text.substring( start, start + expressionLength ).equals( delimiterExpression ) )
					{
						start += expressionLength;
						isExpression = true;
					}
					// Check if this is an include
					else if( text.substring( start, start + includeLength ).equals( delimiterInclude ) )
					{
						start += includeLength;
						isInclude = true;
					}
					// Check if this is an in-flow
					else if( text.substring( start, start + inFlowLength ).equals( delimiterInFlow ) )
					{
						start += inFlowLength;
						isInFlow = true;
					}

					// Get engine name if available
					if( !Character.isWhitespace( text.charAt( start ) ) )
					{
						int endEngineName = start + 1;
						while( !Character.isWhitespace( text.charAt( endEngineName ) ) )
							endEngineName++;

						scriptEngineName = text.substring( start, endEngineName );

						// Optimization: in-flow is unnecessary if we are in the
						// same script engine
						if( isInFlow && lastScriptEngineName.equals( scriptEngineName ) )
							isInFlow = false;

						start = endEngineName + 1;
					}

					if( start + 1 != end )
					{
						// Add script segment
						if( isExpression || isInclude )
						{
							ScriptEngine scriptEngine = scriptEngineManager.getEngineByName( scriptEngineName );
							if( scriptEngine == null )
								throw DocumentInitializationException.scriptEngineNotFound( name, scriptEngineName );

							ScriptletHelper scriptletHelper = Scripturian.getScriptletHelper( scriptEngineName, name );

							if( isExpression )
								segments.add( new DocumentSegment( scriptletHelper.getExpressionAsProgram( this, scriptEngine, text.substring( start, end ) ), true, scriptEngineName ) );
							else if( isInclude )
								segments.add( new DocumentSegment( scriptletHelper.getExpressionAsInclude( this, scriptEngine, text.substring( start, end ) ), true, scriptEngineName ) );
						}
						else if( isInFlow && ( documentSource != null ) )
						{
							ScriptEngine lastScriptEngine = scriptEngineManager.getEngineByName( lastScriptEngineName );
							if( lastScriptEngine == null )
								throw DocumentInitializationException.scriptEngineNotFound( name, lastScriptEngineName );

							ScriptletHelper scriptletHelper = Scripturian.getScriptletHelper( lastScriptEngineName, name );

							String inFlowText = delimiterStart + scriptEngineName + " " + text.substring( start, end ) + delimiterEnd;
							String inFlowName = IN_FLOW_PREFIX + inFlowCounter.getAndIncrement();

							// Note that the in-flow scriptlet is a
							// single segment, so we can optimize parsing a
							// bit
							Document inFlowScript = new Document( name + "/" + inFlowName, inFlowText, false, scriptEngineManager, null, null, allowCompilation, documentVariableName, delimiterStart, delimiterEnd,
								delimiterStart, delimiterEnd, delimiterExpression, delimiterInclude, delimiterInFlow );
							documentSource.setDocumentDescriptor( inFlowName, inFlowText, "", inFlowScript );

							// TODO: would it ever be possible to remove the
							// dependent in-flow instances?

							// Our include is in the last script engine
							segments.add( new DocumentSegment( scriptletHelper.getExpressionAsInclude( this, lastScriptEngine, "'" + inFlowName + "'" ), true, lastScriptEngineName ) );
						}
						else
							segments.add( new DocumentSegment( text.substring( start, end ), true, scriptEngineName ) );
					}

					if( !isInFlow )
						lastScriptEngineName = scriptEngineName;
				}

				last = end + delimiterEndLength;
				start = text.indexOf( delimiterStart, last );
			}

			// Add remaining non-script segment
			if( last < text.length() )
				segments.add( new DocumentSegment( text.substring( last ), false, lastScriptEngineName ) );
		}
		else
		{
			// Trivial file: does not include script
			this.segments = new DocumentSegment[]
			{
				new DocumentSegment( text, false, lastScriptEngineName )
			};
			return;
		}

		// Collapse segments of same kind
		DocumentSegment previous = null;
		DocumentSegment current;
		for( Iterator<DocumentSegment> i = segments.iterator(); i.hasNext(); )
		{
			current = i.next();

			if( previous != null )
			{
				if( previous.isScriptlet == current.isScriptlet )
				{
					if( current.scriptEngineName.equals( previous.scriptEngineName ) )
					{
						// Collapse current into previous
						i.remove();
						previous.text += current.text;
						current = previous;
					}
				}
			}

			previous = current;
		}

		// Collapse segments of same engine as scripts
		// (does not convert first segment into script if it isn't one -- that's
		// good)
		previous = null;
		for( Iterator<DocumentSegment> i = segments.iterator(); i.hasNext(); )
		{
			current = i.next();

			if( ( previous != null ) && previous.isScriptlet )
			{
				if( previous.scriptEngineName.equals( current.scriptEngineName ) )
				{
					// Collapse current into previous
					// (converting to script if necessary)
					i.remove();

					if( current.isScriptlet )
						previous.text += current.text;
					else
					{
						ScriptEngine scriptEngine = scriptEngineManager.getEngineByName( current.scriptEngineName );
						if( scriptEngine == null )
							throw DocumentInitializationException.scriptEngineNotFound( name, current.scriptEngineName );

						ScriptletHelper scriptletHelper = Scripturian.getScriptletHelper( current.scriptEngineName, name );

						// ScriptEngineFactory factory =
						// scriptEngine.getFactory();
						// previous.text += factory.getProgram(
						// factory.getOutputStatement(
						// scriptletHelper.getTextAsScript(
						// scriptEngine,
						// current.text ) ) );

						previous.text += scriptletHelper.getTextAsProgram( this, scriptEngine, current.text );
					}

					current = previous;
				}
			}

			previous = current;
		}

		// Process scriptlets
		for( DocumentSegment segment : segments )
			if( segment.isScriptlet )
				segment.resolve( this, scriptEngineManager, allowCompilation );

		this.segments = new DocumentSegment[segments.size()];
		segments.toArray( this.segments );
	}

	//
	// Attributes
	//

	/**
	 * Name used for error messages.
	 * 
	 * @return The name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * The start delimiter used.
	 * 
	 * @return The start delimiter, or null if none was used
	 * @see #getDelimiterEnd()
	 */
	public String getDelimiterStart()
	{
		return delimiterStart;
	}

	/**
	 * The end delimiter used.
	 * 
	 * @return The end delimiter, or null if none was used
	 * @see #getDelimiterStart()
	 */
	public String getDelimiterEnd()
	{
		return delimiterEnd;
	}

	/**
	 * The default variable name for the {@link ExposedDocument} instance.
	 */
	public String getDocumentVariableName()
	{
		return documentVariableName;
	}

	/**
	 * Timestamp of when the script last finished running successfully, or 0 if
	 * it was never run.
	 * 
	 * @return The timestamp or 0
	 */
	public long getLastRun()
	{
		return lastRun;
	}

	/**
	 * Setting this to something greater than 0 enables caching of the script
	 * results for a maximum number of milliseconds. By default cacheDuration is
	 * 0, so that each call to
	 * {@link #run(boolean, boolean, Writer, Writer, boolean, DocumentContext, Object, ScriptletController)}
	 * causes the script to be run. This class does not handle caching itself.
	 * Caching can be provided by your environment if appropriate.
	 * <p>
	 * This is the same instance provided for ExposedScript#getCacheDuration().
	 * 
	 * @return The cache duration in milliseconds
	 * @see #setCacheDuration(long)
	 * @see #getLastRun()
	 */
	public long getCacheDuration()
	{
		return cacheDuration;
	}

	/**
	 * @param cacheDuration
	 *        The cache duration in milliseconds
	 * @see #getCacheDuration()
	 */
	public void setCacheDuration( long cacheDuration )
	{
		this.cacheDuration = cacheDuration;
	}

	/**
	 * This is the last run plus the cache duration, or 0 if the cache duration
	 * is 0.
	 * 
	 * @return The expiration timestamp or 0
	 * @see #getLastRun()
	 * @see #getCacheDuration()
	 */
	public long getExpiration()
	{
		// TODO: Should this be more atomic? What are the pitfalls of leaving it
		// like this?
		long cacheDuration = getCacheDuration();
		return cacheDuration > 0 ? getLastRun() + cacheDuration : 0;
	}

	/**
	 * Trivial documents have no scriptlets, meaning that they are pure text.
	 * Identifying such documents can save you from making unnecessary calls to
	 * {@link #run(boolean, boolean, Writer, Writer, boolean, DocumentContext, Object, ScriptletController)}
	 * in some situations.
	 * 
	 * @return The text content if it's trivial, null if not
	 */
	public String getTrivial()
	{
		if( segments.length == 1 )
		{
			DocumentSegment sole = segments[0];
			if( !sole.isScriptlet )
				return sole.text;
		}
		return null;
	}

	/**
	 * The document context to be used for calls to
	 * {@link #invoke(String, Object, ScriptletController)}.
	 * 
	 * @return The document context
	 */
	public DocumentContext getDocumentContextForInvocations()
	{
		return documentContextForInvocations;
	}

	//
	// Operations
	//

	/**
	 * Runs the document. Optionally supports checking for output caching, by
	 * testing {@link #cacheDuration} versus the value of {@link #getLastRun()}.
	 * If checking the cache is enabled, this method will return false to
	 * signify that the script did not run and the cached output should be used
	 * instead. In such cases, it is up to your running environment to interpret
	 * this accordingly if you wish to support caching.
	 * <p>
	 * If you intend to run the document multiple times from the same thread, it
	 * is recommended that you use the same {@link DocumentContext} for each
	 * call for better performance.
	 * 
	 * @param checkRan
	 *        Run only if we've never ran before
	 * @param checkCache
	 *        Whether or not to check for caching versus the value of
	 *        {@link #getLastRun()}
	 * @param writer
	 *        Standard output
	 * @param errorWriter
	 *        Standard error output
	 * @param flushLines
	 *        Whether to flush the writers after every line
	 * @param documentContext
	 *        The context
	 * @param container
	 *        The container (can be null)
	 * @param scriptletController
	 *        An optional {@link ScriptletController} to be applied to the
	 *        script context
	 * @return True if the script ran, false if it didn't run, because the
	 *         cached output is expected to be used instead
	 * @throws DocumentInitializationException
	 * @throws DocumentRunException
	 * @throws IOException
	 */
	public boolean run( boolean checkRan, boolean checkCache, Writer writer, Writer errorWriter, boolean flushLines, DocumentContext documentContext, Object container, ScriptletController scriptletController )
		throws DocumentInitializationException, DocumentRunException, IOException
	{
		if( checkRan )
		{
			// TODO: ughghghghghghgh!!!!
			if( lastRun != 0 )
				return false;
		}

		long now = System.currentTimeMillis();
		if( checkCache && ( now - lastRun < cacheDuration ) )
		{
			// We won't run this time
			return false;
		}
		else
		{
			Writer oldWriter = documentContext.setWriter( writer, flushLines );
			Writer oldErrorWriter = documentContext.setErrorWriter( writer, flushLines );

			if( scriptletController != null )
				scriptletController.initialize( documentContext );

			try
			{
				for( DocumentSegment segment : segments )
				{
					if( !segment.isScriptlet )
						writer.write( segment.text );
					else
					{
						ScriptEngine scriptEngine = documentContext.getScriptEngine( segment.scriptEngineName, name );
						ScriptletHelper scriptletHelper = Scripturian.getScriptletHelper( segment.scriptEngineName, name );

						if( !scriptletHelper.isMultiThreaded() )
							scriptletHelper.lock.lock();

						Object oldExposedDocument = documentContext.setVariable( documentVariableName, new ExposedDocument( this, documentContext, scriptEngine, container ) );

						try
						{
							scriptletHelper.beforeCall( scriptEngine, documentContext );

							Object value = documentContext.call( scriptEngine, segment.compiledScript, name, segment.text );

							if( ( value != null ) && scriptletHelper.isPrintOnEval() )
								writer.write( value.toString() );
						}
						finally
						{
							scriptletHelper.afterCall( scriptEngine, documentContext );

							// Restore old document value (this is desirable for
							// documents that run other documents)
							if( oldExposedDocument != null )
								documentContext.setVariable( documentVariableName, oldExposedDocument );

							if( !scriptletHelper.isMultiThreaded() )
								scriptletHelper.lock.unlock();
						}
					}
				}
			}
			finally
			{
				documentContext.setWriter( oldWriter );
				documentContext.setErrorWriter( oldErrorWriter );

				if( scriptletController != null )
					scriptletController.finalize( documentContext );
			}

			this.documentContextForInvocations = documentContext;
			lastRun = now;
			return true;
		}
	}

	/**
	 * Calls an entry point in the document: a function, method, closure, etc.,
	 * according to how the script engine and its language handles invocations.
	 * If not, then this method requires the appropriate {@link ScriptletHelper}
	 * implementation to be installed for the script engine. Most likely, the
	 * script engine supports the {@link Invocable} interface. Running the
	 * script first (via
	 * {@link #run(boolean, boolean, Writer, Writer, boolean, DocumentContext, Object, ScriptletController)}
	 * ) is not absolutely required, but probably will be necessary in most
	 * useful scenarios, where running the script causes useful entry point to
	 * be defined.
	 * <p>
	 * Note that this call does not support sending arguments. If you need to
	 * pass data to the script, use a global variable, which you can set via the
	 * optional {@link ScriptletController}.
	 * <p>
	 * Concurrency note: The invoke mechanism allows for multi-threaded access,
	 * so it's the responsibility of your script to be thread-safe. Also note
	 * that, internally, invoke relies on the {@link DocumentContext} from
	 * {@link #getDocumentContextForInvocations()}. This is set to be the one
	 * used in the last call to
	 * {@link #run(boolean, boolean, Writer, Writer, boolean, DocumentContext, Object, ScriptletController)}
	 * 
	 * @param entryPointName
	 *        The name of the entry point
	 * @param container
	 *        The container (can be null)
	 * @param scriptletController
	 *        An optional {@link ScriptletController} to be applied to the
	 *        script context
	 * @return The value returned by the invocation
	 * @throws DocumentInitializationException
	 * @throws DocumentRunException
	 * @throws NoSuchMethodException
	 *         Note that this exception will only be thrown if the script engine
	 *         supports the {@link Invocable} interface; otherwise a
	 *         {@link ScriptException} is thrown if the method is not found
	 */
	public Object invoke( String entryPointName, Object container, ScriptletController scriptletController ) throws DocumentInitializationException, DocumentRunException, NoSuchMethodException
	{
		if( documentContextForInvocations == null )
			throw new DocumentRunException( name, "Document must be run at least once before calling invoke" );

		ScriptEngine scriptEngine = documentContextForInvocations.getLastScriptEngine();
		String scriptEngineName = documentContextForInvocations.getLastScriptEngineName();

		ScriptletHelper scriptletHelper = Scripturian.getScriptletHelper( scriptEngineName, name );

		if( !scriptletHelper.isMultiThreaded() )
			scriptletHelper.lock.lock();

		Object oldExposedDocument = documentContextForInvocations.setVariable( documentVariableName, new ExposedDocument( this, documentContextForInvocations, scriptEngine, container ) );

		try
		{
			scriptletHelper.beforeCall( scriptEngine, documentContextForInvocations );

			if( scriptletController != null )
				scriptletController.initialize( documentContextForInvocations );

			String program = scriptletHelper.getInvocationAsProgram( this, scriptEngine, entryPointName );
			return documentContextForInvocations.invoke( scriptEngine, scriptEngineName, entryPointName, name, program );
		}
		finally
		{
			scriptletHelper.afterCall( scriptEngine, documentContextForInvocations );

			if( scriptletController != null )
				scriptletController.finalize( documentContextForInvocations );

			// Restore old script value (this is desirable for scripts that run
			// other scripts)
			if( oldExposedDocument != null )
				documentContextForInvocations.setVariable( documentVariableName, oldExposedDocument );

			if( !scriptletHelper.isMultiThreaded() )
				scriptletHelper.lock.unlock();
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final String IN_FLOW_PREFIX = "_IN_FLOW_";

	private static final AtomicInteger inFlowCounter = new AtomicInteger();

	final String name;

	private final DocumentSegment[] segments;

	private final String delimiterStart;

	private final String delimiterEnd;

	private final String documentVariableName;

	private volatile long cacheDuration = 0;

	private volatile long lastRun = 0;

	private volatile DocumentContext documentContextForInvocations;
}
