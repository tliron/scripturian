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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import com.threecrickets.scripturian.internal.ExposedEmbeddedScript;

/**
 * Handles the parsing, optional compilation and running of embedded scripts.
 * <p>
 * Embedded scripts are text streams containing a mix of script code, which is
 * embedded between special delimiters, and plain text. During the parsing
 * stage, which happens only once in the constructor, the entire text stream is
 * converted into script code. When the script is run, non-delimited plain text
 * being sent to output via whatever method is appropriate for the scripting
 * engine (see {@link EmbeddedScriptParsingHelper}). The exception to this is
 * text streams beginning with plain text -- in such cases, just that first
 * section is sent to your specified output in pure Java. The reasons for this
 * are two: first, that no script engine has been specified yet, so we can't be
 * sure which to use, and second, that sending in pure Java is probably a bit
 * faster than sending in script. If the entire text stream is just plain text,
 * it is simply sent to output without invoking any script engine. In such
 * cases, {@link #getTrivial()} would return the content of the text stream.
 * <p>
 * Text streams are often provided by an implementation of {@link ScriptSource},
 * though your environment can use its own method.
 * <p>
 * Embedded scripts can easily be abused, becoming hard to read and hard to
 * maintain, since both code and plain text are mixed together. However, they
 * can boost productivity in environments which mostly just output plain text.
 * For example, in an application that dynamically generates HTML web pages, it
 * is likely that most files will be in HTML with only some parts of some files
 * containing embedded script. In such cases, it is easy to have a professional
 * web designer work on the HTML parts while the embedded scripts are reserved
 * for the programmer. Web design software can often recognize embedded script
 * and take care to keep it safe while the web designer makes changes to the
 * file.
 * <p>
 * This class can support multiple script languages and engines within the same
 * text. Each delimited script segment can specify which engine it uses
 * explicitly at the opening delimiter. If the engine is not specified, whatever
 * engine was previously used in the file will be used. If no engine was
 * previously specified, a default value is used (supplied in the constructor).
 * <p>
 * Two delimiting styles are supported: JSP/ASP style (using percentage signs),
 * and the PHP style (using question marks). However, each text stream must
 * adhere to only one style throughout.
 * <p>
 * In addition to embedded scripts, this class supports a shorthand for embedded
 * script expressions. These are internally just sent to standard output.
 * However, they can allow for more compact and cleaner code.
 * <p>
 * Another shorthand exists for including other script files. However, for it to
 * work, you must make sure that a <code>script.container.include(name)</code>
 * method is available to the script, which would then process the include as is
 * appropriate to your environment.
 * <p>
 * Finally, the in-flow shorthand works exactly like an include, but lets you
 * place the included script into the flow of the current script. The inclusion
 * is applied to the previously used script engine, which is then restored to
 * being the default engine after the in-flow tag. This construct allows for
 * very powerful and clean mixing of scripting engines, without cumbersome
 * creation of separate scripts for inclusion.
 * <p>
 * Examples:
 * <ul>
 * <li><b>JSP/ASP-style delimiters</b>: <code>&lt;% print('Hello World'); %&gt;</code></li>
 * <li><b>PHP-style delimiters</b>: <code>&lt;? script.cacheDuration.set 5000
 * ?&gt;</code></li>
 * <li><b>Specifying engine name</b>:
 * <code>&lt;%groovy print myVariable %&gt; &lt;?php $script->container->include(lib_name); ?&gt;</code>
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
 * <code>script</code> (this name can be changed via the
 * {@link #EmbeddedScript(String, ScriptEngineManager, String, boolean, String, String, String, String, String, String, String)}
 * constructor).
 * <p>
 * Read-only attributes:
 * <ul>
 * <li><code>script.container</code>: This is an arbitrary object set by the
 * script's container environment for access to container-specific services. It
 * might be null if none was provided.</li>
 * <li><code>script.context</code>: This is the {@link ScriptContext} used by
 * the script. Scripts may use it to get access to the {@link Writer} objects
 * used for standard output and standard error.</li>
 * <li><code>script.engine</code>: This is the {@link ScriptEngine} used by the
 * script. Scripts may use it to get information about the engine's
 * capabilities.</li>
 * <li><code>script.engineManager</code>: This is the
 * {@link ScriptEngineManager} used to create the script engine. Scripts may use
 * it to get information about what other engines are available.</li>
 * <li><code>script.meta</code>: This {@link ConcurrentMap} provides a
 * convenient location for global values shared by all scripts, run by all
 * engines.</li>
 * </ul>
 * Modifiable attributes:
 * <ul>
 * <li><code>script.cacheDuration</code>: Setting this to something greater than
 * 0 enables caching of the script results for a maximum number of milliseconds.
 * By default cacheDuration is 0, so that each request causes the script to be
 * evaluated. This class does not handle caching itself. Caching can be provided
 * by your environment if appropriate.</li>
 * </ul>
 * 
 * @author Tal Liron
 */
public class EmbeddedScript
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
	 * The default script variable name: "script"
	 */
	public static final String DEFAULT_SCRIPT_VARIABLE_NAME = "script";

	//
	// Static attributes
	//

	/**
	 * A map of script engine names to their {@link EmbeddedScriptParsingHelper}
	 * . Note that embedded scripts will not work without the appropriate
	 * parsing helpers being installed.
	 * <p>
	 * This map is automatically initialized when this class loads according to
	 * resources named
	 * <code>META-INF/services/com.threecrickets.scripturian.EmbeddedScriptParsingHelper</code>
	 * . Each resource is a simple text file with class names, one per line.
	 * Each class listed must implement the {@link EmbeddedScriptParsingHelper}
	 * interface and specify which engine names it supports via the
	 * {@link ScriptEngines} annotation.
	 * <p>
	 * You may also manipulate this map yourself, adding and removing helpers as
	 * necessary.
	 * <p>
	 * The default implementation of this library already contains a few useful
	 * parsing helpers, under the com.threecrickets.scripturian.helper package.
	 */
	public static ConcurrentMap<String, EmbeddedScriptParsingHelper> embeddedScriptParsingHelpers = new ConcurrentHashMap<String, EmbeddedScriptParsingHelper>();

	{
		// Initialize embeddedScriptParsingHelpers (look for them in META-INF)

		// For Java 6

		/*
		 * ServiceLoader<EmbeddedScriptParsingHelper> serviceLoader =
		 * ServiceLoader.load( EmbeddedScriptParsingHelper.class ); for(
		 * EmbeddedScriptParsingHelper embeddedScriptParsingHelper :
		 * serviceLoader ) { ScriptEngines scriptEngines =
		 * embeddedScriptParsingHelper.getClass().getAnnotation(
		 * ScriptEngines.class ); if( scriptEngines != null ) for( String
		 * scriptEngine : scriptEngines.value() )
		 * embeddedScriptParsingHelpers.put( scriptEngine,
		 * embeddedScriptParsingHelper ); }
		 */

		// For Java 5
		String resourceName = "META-INF/services/" + EmbeddedScriptParsingHelper.class.getCanonicalName();
		try
		{
			Enumeration<URL> resources = ClassLoader.getSystemResources( resourceName );
			while( resources.hasMoreElements() )
			{
				InputStream stream = resources.nextElement().openStream();
				BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) );
				String line = reader.readLine();
				while( line != null )
				{
					line = line.trim();
					if( ( line.length() > 0 ) && !line.startsWith( "#" ) )
					{
						EmbeddedScriptParsingHelper embeddedScriptParsingHelper = (EmbeddedScriptParsingHelper) Class.forName( line ).newInstance();
						ScriptEngines scriptEngines = embeddedScriptParsingHelper.getClass().getAnnotation( ScriptEngines.class );
						if( scriptEngines != null )
							for( String scriptEngine : scriptEngines.value() )
								embeddedScriptParsingHelpers.put( scriptEngine, embeddedScriptParsingHelper );
					}
					line = reader.readLine();
				}
				stream.close();
				reader.close();
			}
		}
		catch( IOException x )
		{
			x.printStackTrace();
		}
		catch( InstantiationException x )
		{
			x.printStackTrace();
		}
		catch( IllegalAccessException x )
		{
			x.printStackTrace();
		}
		catch( ClassNotFoundException x )
		{
			x.printStackTrace();
		}
	}

	//
	// Construction
	//

	/**
	 * Parses a text stream containing plan text and embedded script segments
	 * into a compact, optimized script. Parsing requires the appropriate
	 * {@link EmbeddedScriptParsingHelper} implementations to be installed for
	 * the script engines.
	 * 
	 * @param text
	 *        The text stream
	 * @param scriptEngineManager
	 *        The script engine manager used to create script engines
	 * @param defaultEngineName
	 *        If a script engine name isn't explicitly specified in the embedded
	 *        script file, this one will be used
	 * @param scriptSource
	 *        The script source (used for in-flow tags)
	 * @param allowCompilation
	 *        Whether script segments will be compiled (note that compilation
	 *        will only happen if the script engine supports it, and that what
	 *        compilation exactly means is left up to the script engine)
	 * @param scriptSource
	 *        The script source, used for in-flow tags
	 * @throws ScriptException
	 *         In case of a parsing error
	 */
	public EmbeddedScript( String text, ScriptEngineManager scriptEngineManager, String defaultEngineName, ScriptSource<EmbeddedScript> scriptSource, boolean allowCompilation ) throws ScriptException
	{
		this( text, scriptEngineManager, defaultEngineName, scriptSource, allowCompilation, DEFAULT_SCRIPT_VARIABLE_NAME, DEFAULT_DELIMITER1_START, DEFAULT_DELIMITER1_END, DEFAULT_DELIMITER2_START,
			DEFAULT_DELIMITER2_END, DEFAULT_DELIMITER_EXPRESSION, DEFAULT_DELIMITER_INCLUDE, DEFAULT_DELIMITER_IN_FLOW );
	}

	/**
	 * Parses a text stream containing plan text and embedded script segments
	 * into a compact, optimized script. Parsing requires the appropriate
	 * {@link EmbeddedScriptParsingHelper} implementations to be installed for
	 * the script engines.
	 * 
	 * @param text
	 *        The text stream
	 * @param scriptEngineManager
	 *        The script engine manager used to create script engines
	 * @param defaultEngineName
	 *        If a script engine name isn't explicitly specified in the embedded
	 *        script file, this one will be used
	 * @param scriptSource
	 *        The script source (used for in-flow tags)
	 * @param allowCompilation
	 *        Whether script segments will be compiled (note that compilation
	 *        will only happen if the script engine supports it, and that what
	 *        compilation exactly means is left up to the script engine)
	 * @param scriptVariableName
	 *        The script variable name
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
	 * @throws ScriptException
	 *         In case of a parsing error
	 * @see EmbeddedScriptParsingHelper
	 */
	public EmbeddedScript( String text, ScriptEngineManager scriptEngineManager, String defaultEngineName, ScriptSource<EmbeddedScript> scriptSource, boolean allowCompilation, String scriptVariableName,
		String delimiter1Start, String delimiter1End, String delimiter2Start, String delimiter2End, String delimiterExpression, String delimiterInclude, String delimiterInFlow ) throws ScriptException
	{
		this.scriptEngineManager = scriptEngineManager;
		this.scriptVariableName = scriptVariableName;

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

		// Parse segments
		if( start != -1 )
		{
			int last = 0;

			while( start != -1 )
			{
				// Add previous non-script segment
				if( start != last )
					segments.add( new Segment( text.substring( last, start ), false, lastScriptEngineName ) );

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
								throw new ScriptException( "Unsupported script engine: " + scriptEngineName );

							EmbeddedScriptParsingHelper embeddedScriptParsingHelper = embeddedScriptParsingHelpers.get( scriptEngineName );
							if( embeddedScriptParsingHelper == null )
								throw new ScriptException( "Embedded script parsing helper not available for script engine: " + scriptEngineName );

							if( isExpression )
								segments.add( new Segment( embeddedScriptParsingHelper.getExpressionAsProgram( this, scriptEngine, text.substring( start, end ) ), true, scriptEngineName ) );
							else if( isInclude )
								segments.add( new Segment( embeddedScriptParsingHelper.getExpressionAsInclude( this, scriptEngine, text.substring( start, end ) ), true, scriptEngineName ) );
						}
						else if( isInFlow && ( scriptSource != null ) )
						{
							ScriptEngine lastScriptEngine = scriptEngineManager.getEngineByName( lastScriptEngineName );
							if( lastScriptEngine == null )
								throw new ScriptException( "Unsupported script engine: " + lastScriptEngineName );

							EmbeddedScriptParsingHelper embeddedScriptParsingHelper = embeddedScriptParsingHelpers.get( lastScriptEngineName );
							if( embeddedScriptParsingHelper == null )
								throw new ScriptException( "Embedded script parsing helper not available for script engine: " + lastScriptEngineName );

							String inFlowText = delimiterStart + scriptEngineName + " " + text.substring( start, end ) + delimiterEnd;
							String inFlowName = IN_FLOW_PREFIX + inFlowCounter.getAndIncrement();

							// Note that the in-flow embedded script is a
							// single segment, so we can optimize parsing a
							// bit
							EmbeddedScript inFlowEmbeddedScript = new EmbeddedScript( inFlowText, scriptEngineManager, null, null, allowCompilation, scriptVariableName, delimiterStart, delimiterEnd, delimiterStart,
								delimiterEnd, delimiterExpression, delimiterInclude, delimiterInFlow );
							scriptSource.setScriptDescriptor( inFlowName, inFlowText, "", inFlowEmbeddedScript );

							// TODO: would it ever be possible to remove the
							// dependent in-flow instances?

							// Our include is in the last script engine
							segments.add( new Segment( embeddedScriptParsingHelper.getExpressionAsInclude( this, lastScriptEngine, "'" + inFlowName + "'" ), true, lastScriptEngineName ) );
						}
						else
							segments.add( new Segment( text.substring( start, end ), true, scriptEngineName ) );
					}

					if( !isInFlow )
						lastScriptEngineName = scriptEngineName;
				}

				last = end + delimiterEndLength;
				start = text.indexOf( delimiterStart, last );
			}

			// Add remaining non-script segment
			if( last < text.length() )
				segments.add( new Segment( text.substring( last ), false, lastScriptEngineName ) );
		}
		else
		{
			// Trivial file: does not include script
			segments.add( new Segment( text, false, lastScriptEngineName ) );
		}

		// Collapse segments of same kind
		Segment previous = null;
		Segment current;
		for( Iterator<Segment> i = segments.iterator(); i.hasNext(); )
		{
			current = i.next();

			if( previous != null )
			{
				if( previous.isScript == current.isScript )
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
		for( Iterator<Segment> i = segments.iterator(); i.hasNext(); )
		{
			current = i.next();

			if( ( previous != null ) && previous.isScript )
			{
				if( previous.scriptEngineName.equals( current.scriptEngineName ) )
				{
					// Collapse current into previous
					// (converting to script if necessary)
					i.remove();

					if( current.isScript )
						previous.text += current.text;
					else
					{
						ScriptEngine scriptEngine = scriptEngineManager.getEngineByName( current.scriptEngineName );
						if( scriptEngine == null )
							throw new ScriptException( "Unsupported script engine: " + current.scriptEngineName );

						EmbeddedScriptParsingHelper embeddedScriptParsingHelper = embeddedScriptParsingHelpers.get( current.scriptEngineName );
						if( embeddedScriptParsingHelper == null )
							throw new ScriptException( "Embedded script parsing helper not available for script engine: " + current.scriptEngineName );

						// ScriptEngineFactory factory =
						// scriptEngine.getFactory();
						// previous.text += factory.getProgram(
						// factory.getOutputStatement(
						// embeddedScriptParsingHelper.getTextAsScript(
						// scriptEngine,
						// current.text ) ) );

						previous.text += embeddedScriptParsingHelper.getTextAsProgram( this, scriptEngine, current.text );
					}

					current = previous;
				}
			}

			previous = current;
		}

		// Compiles segments if possible
		if( allowCompilation )
			for( Segment segment : segments )
			{
				if( segment.isScript )
				{
					ScriptEngine scriptEngine = scriptEngineManager.getEngineByName( segment.scriptEngineName );
					if( scriptEngine == null )
						throw new ScriptException( "Unsupported script engine: " + segment.scriptEngineName );

					EmbeddedScriptParsingHelper embeddedScriptParsingHelper = embeddedScriptParsingHelpers.get( segment.scriptEngineName );
					if( embeddedScriptParsingHelper == null )
						throw new ScriptException( "Embedded script parsing helper not available for script engine: " + segment.scriptEngineName );

					// Add header
					String header = embeddedScriptParsingHelper.getScriptHeader( this, scriptEngine );
					if( header != null )
						segment.text = header + segment.text;

					// Add footer
					String footer = embeddedScriptParsingHelper.getScriptFooter( this, scriptEngine );
					if( footer != null )
						segment.text += footer;

					if( scriptEngine instanceof Compilable )
						segment.compiledScript = ( (Compilable) scriptEngine ).compile( segment.text );
				}
			}
	}

	//
	// Attributes
	//

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
	 * The default variable name for the {@link ExposedEmbeddedScript} instance.
	 */
	public String getScriptVariableName()
	{
		return scriptVariableName;
	}

	/**
	 * Timestamp of when the script last finished running successfully.
	 * 
	 * @return The timestamp
	 */
	public long getLastRun()
	{
		return lastRun.get();
	}

	/**
	 * The {@link ScriptEngineManager} used to create the script engines.
	 * 
	 * @return The script engine manager
	 */
	public ScriptEngineManager getScriptEngineManager()
	{
		return scriptEngineManager;
	}

	/**
	 * The last {@link ScriptEngine} used in the last run of the script.
	 * 
	 * @return The script engine
	 */
	public ScriptEngine getLastScriptEngine()
	{
		return scriptEngine;
	}

	/**
	 * Setting this to something greater than 0 enables caching of the script
	 * results for a maximum number of milliseconds. By default cacheDuration is
	 * 0, so that each call to
	 * {@link #run(Writer, Writer, boolean, ConcurrentMap, Object, ScriptContextController, boolean)}
	 * causes the script to be run. This class does not handle caching itself.
	 * Caching can be provided by your environment if appropriate.
	 * <p>
	 * This is the same instance provided for
	 * ExposedEmbeddedScript#getCacheDuration().
	 * 
	 * @return The cache duration in milliseconds
	 * @see #setCacheDuration(long)
	 * @see #getLastRun()
	 */
	public long getCacheDuration()
	{
		return cacheDuration.get();
	}

	/**
	 * @param cacheDuration
	 *        The cache duration in milliseconds
	 * @see #getCacheDuration()
	 */
	public void setCacheDuration( long cacheDuration )
	{
		this.cacheDuration.set( cacheDuration );
	}

	/**
	 * Trivial embedded script objects have no embedded scripts, meaning that
	 * they are pure text. Identifying such scripts can save you from making
	 * unnecessary calls to
	 * {@link #run(Writer, Writer, boolean, ConcurrentMap, Object, ScriptContextController, boolean)}
	 * in some situations.
	 * 
	 * @return The script content if it's trivial, null if not
	 */
	public String getTrivial()
	{
		if( segments.size() == 1 )
		{
			Segment sole = segments.get( 0 );
			if( !sole.isScript )
				return sole.text;
		}
		return null;
	}

	//
	// Operations
	//

	/**
	 * Runs the script. Optionally supports checking for output caching, by
	 * testing {@link #cacheDuration} versus the value of {@link #getLastRun()}.
	 * If checking the cache is enabled, this method will return false to
	 * signify that the script did not run and the cached output should be used
	 * instead. In such cases, it is up to your running environment to interpret
	 * this accordingly if you wish to support caching.
	 * 
	 * @param writer
	 *        Standard output
	 * @param errorWriter
	 *        Standard error output
	 * @param flushLines
	 *        Whether to flush the writers after every line
	 * @param scriptEngines
	 *        A cache of script engines by engine name
	 * @param container
	 *        The container (can be null)
	 * @param scriptContextController
	 *        An optional {@link ScriptContextController} to be applied to the
	 *        script context
	 * @param checkCache
	 *        Whether or not to check for caching versus the value of
	 *        {@link #getLastRun()}
	 * @return True if the script ran, false if it didn't run, because the
	 *         cached output is expected to be used instead
	 * @throws ScriptException
	 */
	public boolean run( Writer writer, Writer errorWriter, boolean flushLines, ConcurrentMap<String, ScriptEngine> scriptEngines, Object container, ScriptContextController scriptContextController, boolean checkCache )
		throws ScriptException, IOException
	{
		long now = System.currentTimeMillis();
		if( checkCache && ( now - lastRun.get() < cacheDuration.get() ) )
		{
			// We didn't run this time
			return false;
		}
		else
		{
			for( Segment segment : segments )
			{
				if( !segment.isScript )
					writer.write( segment.text );
				else
				{
					scriptEngineName = segment.scriptEngineName;
					setScriptEngine( scriptEngineName, scriptEngines );
					ScriptContext scriptContext = scriptEngine.getContext();

					// Note that some script engines (such as Rhino) expect a
					// PrintWriter, even though the spec defines just a Writer
					writer = new PrintWriter( writer, flushLines );
					errorWriter = new PrintWriter( errorWriter, flushLines );

					scriptContext.setWriter( writer );
					scriptContext.setErrorWriter( errorWriter );

					Object oldScript = scriptContext.getAttribute( scriptVariableName, ScriptContext.ENGINE_SCOPE );
					scriptContext.setAttribute( scriptVariableName, new ExposedEmbeddedScript( this, scriptEngine, scriptContext, container ), ScriptContext.ENGINE_SCOPE );

					if( scriptContextController != null )
						scriptContextController.initialize( scriptContext );

					try
					{
						if( segment.compiledScript != null )
							segment.compiledScript.eval( scriptContext );
						else
						{
							// Note that we are wrapping our text with a
							// StringReader. Why? Because some implementations
							// of javax.script (notably Jepp) interpret the
							// String version of eval to mean only one line of
							// code.
							scriptEngine.eval( new StringReader( segment.text ) );
						}
					}
					catch( ScriptException x )
					{
						throw x;
					}
					catch( Exception x )
					{
						// Some script engines (notably Quercus) throw their own
						// special exceptions
						throw new ScriptException( x );
					}
					finally
					{
						if( scriptContextController != null )
							scriptContextController.finalize( scriptContext );

						// Restore old script value (this is desirable for
						// scripts that run other scripts)
						if( oldScript != null )
							scriptContext.setAttribute( scriptVariableName, oldScript, ScriptContext.ENGINE_SCOPE );
					}
				}
			}

			lastRun.set( now );
			return true;
		}
	}

	/**
	 * Calls an entry point in the script: a function, method, closure, etc.,
	 * according to how the scripting engine and its language handles
	 * invocations. If not, then this method requires the appropriate
	 * {@link EmbeddedScriptParsingHelper} implementation to be installed for
	 * the script engine. Most likely, the script engine supports the
	 * {@link Invocable} interface. Running the script first (via
	 * {@link #run(Writer, Writer, boolean, ConcurrentMap, Object, ScriptContextController, boolean)}
	 * ) is not absolutely required, but probably will be necessary in most
	 * useful scenarios, where running the script causes useful entry point to
	 * be defined.
	 * <p>
	 * Note that this call does not support sending arguments. If you need to
	 * pass data to the script, use a global variable, which you can set via the
	 * optional {@link ScriptContextController}.
	 * 
	 * @param entryPointName
	 *        The name of the entry point
	 * @param container
	 *        The container (can be null)
	 * @param scriptContextController
	 *        An optional {@link ScriptContextController} to be applied to the
	 *        script context
	 * @return The value returned by the script call
	 * @throws ScriptException
	 * @throws NoSuchMethodException
	 *         Note that this exception will only be thrown if the script engine
	 *         supports the {@link Invocable} interface; otherwise a
	 *         {@link ScriptException} is thrown if the method is not found
	 */
	public Object invoke( String entryPointName, Object container, ScriptContextController scriptContextController ) throws ScriptException, NoSuchMethodException
	{
		ScriptContext scriptContext = scriptEngine.getContext();

		Object oldScript = scriptContext.getAttribute( scriptVariableName, ScriptContext.ENGINE_SCOPE );
		scriptContext.setAttribute( scriptVariableName, new ExposedEmbeddedScript( this, scriptEngine, scriptContext, container ), ScriptContext.ENGINE_SCOPE );

		if( scriptContextController != null )
			scriptContextController.initialize( scriptContext );

		try
		{
			EmbeddedScriptParsingHelper embeddedScriptParsingHelper = embeddedScriptParsingHelpers.get( scriptEngineName );

			if( embeddedScriptParsingHelper == null )
				throw new ScriptException( "Embedded script parsing helper not available for script engine: " + scriptEngineName );

			String program = embeddedScriptParsingHelper.getInvocationAsProgram( this, scriptEngine, entryPointName );
			if( program == null )
			{
				if( scriptEngine instanceof Invocable )
					return ( (Invocable) scriptEngine ).invokeFunction( entryPointName );
				else
					throw new ScriptException( "Script engine does not support invocations" );
			}
			else
				return scriptEngine.eval( program );
		}
		finally
		{
			if( scriptContextController != null )
				scriptContextController.finalize( scriptContext );

			// Restore old script value (this is desirable for scripts that run
			// other scripts)
			if( oldScript != null )
				scriptContext.setAttribute( scriptVariableName, oldScript, ScriptContext.ENGINE_SCOPE );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final String IN_FLOW_PREFIX = "_IN_FLOW_";

	private static final AtomicInteger inFlowCounter = new AtomicInteger();

	private final List<Segment> segments = new LinkedList<Segment>();

	private final String delimiterStart;

	private final String delimiterEnd;

	private final String scriptVariableName;

	private final ScriptEngineManager scriptEngineManager;

	private final AtomicLong cacheDuration = new AtomicLong();

	private final AtomicLong lastRun = new AtomicLong();

	private ScriptEngine scriptEngine;

	private String scriptEngineName;

	private static class Segment
	{
		public Segment( String text, boolean isScript, String scriptEngineName )
		{
			this.text = text;
			this.isScript = isScript;
			this.scriptEngineName = scriptEngineName;
		}

		public String text;

		public CompiledScript compiledScript;

		public boolean isScript;

		public String scriptEngineName;
	}

	private void setScriptEngine( String scriptEngineName, ConcurrentMap<String, ScriptEngine> scriptEngines ) throws ScriptException
	{
		scriptEngine = scriptEngines.get( scriptEngineName );
		if( scriptEngine == null )
		{
			scriptEngine = scriptEngineManager.getEngineByName( scriptEngineName );
			if( scriptEngine == null )
				throw new ScriptException( "Unsupported script engine: " + scriptEngineName );

			// Note: another thread might have put a ScriptEngine
			// here in the meantime... we'll make sure there is no
			// duplication
			ScriptEngine existing = scriptEngines.putIfAbsent( scriptEngineName, scriptEngine );
			if( existing != null )
				scriptEngine = existing;
			else
			{
				// We absolutely need a new script context here!
				// Otherwise, we might end up using a context
				// already in use by another thread.
				// (Also, note that some script engines do not even
				// provide a default context -- Jepp, for example -- so
				// it's generally a good idea to explicitly create one)
				ScriptContext scriptContext = new SimpleScriptContext();
				scriptContext.setBindings( scriptEngine.createBindings(), ScriptContext.ENGINE_SCOPE );
				scriptContext.setBindings( scriptEngine.createBindings(), ScriptContext.GLOBAL_SCOPE );
				scriptEngine.setContext( scriptContext );
			}
		}
	}
}
