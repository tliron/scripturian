/**
 * Copyright 2009-2011 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian;

import java.util.HashMap;
import java.util.Map;

import com.threecrickets.scripturian.document.DocumentSource;

/**
 * The parsing context is used to construct {@link Executable} instances.
 * <p>
 * Note that instances are <i>not</i> thread-safe.
 * 
 * @author Tal Liron
 */
public class ParsingContext
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
	 * The default addition to the start delimiter to specify a comment
	 * scriptlet: #
	 */
	public static final String DEFAULT_DELIMITER_COMMENT = "#";

	/**
	 * The default addition to the start delimiter to specify an expression
	 * scriptlet: =
	 */
	public static final String DEFAULT_DELIMITER_EXPRESSION = "=";

	/**
	 * The default addition to the start delimiter to specify an include
	 * scriptlet: &
	 */
	public static final String DEFAULT_DELIMITER_INCLUDE = "&";

	/**
	 * The default addition to the start delimiter to specify an in-flow
	 * scriptlet: :
	 */
	public static final String DEFAULT_DELIMITER_IN_FLOW = ":";

	/**
	 * The default executable service name: "executable"
	 */
	public static final String DEFAULT_EXECUTABLE_SERVICE_NAME = "executable";

	/**
	 * Prefix prepended to in-flow scriptlets stores in the document source.
	 */
	public static final String IN_FLOW_PREFIX = "_IN_FLOW_";

	//
	// Construction
	//

	/**
	 * Constructor.
	 */
	public ParsingContext()
	{
	}

	/**
	 * Copy constructor.
	 * 
	 * @param parsingContext
	 *        The parsing context from which to copy
	 */
	public ParsingContext( ParsingContext parsingContext )
	{
		languageManager = parsingContext.getLanguageManager();
		partition = parsingContext.getPartition();
		defaultLanguageTag = parsingContext.getDefaultLanguageTag();
		prepare = parsingContext.isPrepare();
		delimiter1Start = parsingContext.getDelimiter1Start();
		delimiter1End = parsingContext.getDelimiter1End();
		delimiter2Start = parsingContext.getDelimiter2Start();
		delimiter2End = parsingContext.getDelimiter2End();
		delimiterComment = parsingContext.getDelimiterComment();
		delimiterExpression = parsingContext.getDelimiterExpression();
		delimiterInclude = parsingContext.getDelimiterInclude();
		delimiterInFlow = parsingContext.getDelimiterInFlow();
		documentSource = parsingContext.getDocumentSource();
		exposedExecutableName = parsingContext.getExposedExecutableName();
		scriptletPlugins.clear();
		scriptletPlugins.putAll( parsingContext.getScriptletPlugins() );
	}

	//
	// Attributes
	//

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
	 * @param languageManager
	 *        The language manager
	 * @see #getLanguageManager()
	 */
	public void setLanguageManager( LanguageManager languageManager )
	{
		this.languageManager = languageManager;
	}

	/**
	 * The executable partition.
	 * 
	 * @return The partition
	 */
	public String getPartition()
	{
		return partition;
	}

	/**
	 * @param partition
	 *        The partition
	 * @see #getPartition()
	 */
	public void setPartition( String partition )
	{
		this.partition = partition;
	}

	/**
	 * When {@code isTextWithScriptlets} is true, this is the language used for
	 * scriptlets if none is specified.
	 * 
	 * @return The default language tag
	 */
	public String getDefaultLanguageTag()
	{
		return defaultLanguageTag;
	}

	/**
	 * @param defaultLanguageTag
	 *        The default language tag
	 * @see #getDefaultLanguageTag()
	 */
	public void setDefaultLanguageTag( String defaultLanguageTag )
	{
		this.defaultLanguageTag = defaultLanguageTag;
	}

	/**
	 * Whether to prepare the source code: preparation increases initialization
	 * time and reduces execution time; note that not all languages support
	 * preparation as a separate operation.
	 * 
	 * @return The prepare flag
	 */
	public boolean isPrepare()
	{
		return prepare;
	}

	/**
	 * @param prepare
	 *        The prepare flag
	 * @see #isPrepare()
	 */
	public void setPrepare( boolean prepare )
	{
		this.prepare = prepare;
	}

	/**
	 * The start delimiter (first option).
	 * 
	 * @return The start delimiter
	 */
	public String getDelimiter1Start()
	{
		return delimiter1Start;
	}

	/**
	 * @param delimiter1Start
	 *        The start delimiter
	 * @see #getDelimiter1Start()
	 */
	public void setDelimiter1Start( String delimiter1Start )
	{
		this.delimiter1Start = delimiter1Start;
	}

	/**
	 * The end delimiter (first option).
	 * 
	 * @return The end delimiter
	 */
	public String getDelimiter1End()
	{
		return delimiter1End;
	}

	/**
	 * @param delimiter1End
	 *        The end delimiter
	 * @see #getDelimiter1End()
	 */
	public void setDelimiter1End( String delimiter1End )
	{
		this.delimiter1End = delimiter1End;
	}

	/**
	 * The start delimiter (second option).
	 * 
	 * @return The start delimiter
	 */
	public String getDelimiter2Start()
	{
		return delimiter2Start;
	}

	/**
	 * @param delimiter2Start
	 *        The start delimiter
	 * @see #getDelimiter2Start()
	 */
	public void setDelimiter2Start( String delimiter2Start )
	{
		this.delimiter2Start = delimiter2Start;
	}

	/**
	 * The end delimiter (second option).
	 * 
	 * @return The end delimiter
	 */
	public String getDelimiter2End()
	{
		return delimiter2End;
	}

	/**
	 * @param delimiter2End
	 *        The end delimiter
	 * @see #getDelimiter2End()
	 */
	public void setDelimiter2End( String delimiter2End )
	{
		this.delimiter2End = delimiter2End;
	}

	/**
	 * The addition to the start delimiter to specify a comment scriptlet.
	 * 
	 * @return The comment delimiter
	 */
	public String getDelimiterComment()
	{
		return delimiterComment;
	}

	/**
	 * @param delimiterComment
	 *        The comment delimiter
	 * @see #getDelimiterComment()
	 */
	public void setDelimiterComment( String delimiterComment )
	{
		this.delimiterComment = delimiterComment;
	}

	/**
	 * The addition to the start delimiter to specify an expression scriptlet.
	 * 
	 * @return The expression delimiter
	 */
	public String getDelimiterExpression()
	{
		return delimiterExpression;
	}

	/**
	 * @param delimiterExpression
	 *        The expression delimiter
	 * @see #getDelimiterExpression()
	 */
	public void setDelimiterExpression( String delimiterExpression )
	{
		this.delimiterExpression = delimiterExpression;
	}

	/**
	 * The addition to the start delimiter to specify an include scriptlet.
	 * 
	 * @return The include delimiter
	 */
	public String getDelimiterInclude()
	{
		return delimiterInclude;
	}

	/**
	 * @param delimiterInclude
	 *        The include delimiter
	 * @see #getDelimiterInclude()
	 */
	public void setDelimiterInclude( String delimiterInclude )
	{
		this.delimiterInclude = delimiterInclude;
	}

	/**
	 * The addition to the start delimiter to specify an in-flow scriptlet.
	 * 
	 * @return The in-flow delimiter
	 */
	public String getDelimiterInFlow()
	{
		return delimiterInFlow;
	}

	/**
	 * @param delimiterInFlow
	 *        The in-flow delimiter
	 * @see #getDelimiterInFlow()
	 */
	public void setDelimiterInFlow( String delimiterInFlow )
	{
		this.delimiterInFlow = delimiterInFlow;
	}

	/**
	 * A document source used to store in-flow scriptlets; can be null if
	 * in-flow scriptlets are not used.
	 * 
	 * @return The document source
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

	/**
	 * The <code>executable</code> service name exposed to executables.
	 * 
	 * @return The exposed executable service name
	 */
	public String getExposedExecutableName()
	{
		return exposedExecutableName;
	}

	/**
	 * @param exposedExecutableName
	 *        The exposed executable service name
	 * @see #getExposedExecutableName()
	 */
	public void setExposedExecutableName( String exposedExecutableName )
	{
		this.exposedExecutableName = exposedExecutableName;
	}

	/**
	 * The scriptlet plugins.
	 * 
	 * @return The scriptlet plugins
	 */
	public Map<String, ScriptletPlugin> getScriptletPlugins()
	{
		return scriptletPlugins;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The language manager used to parse, prepare and execute the executable.
	 */
	private LanguageManager languageManager;

	/**
	 * The executable partition.
	 */
	private String partition;

	/**
	 * When {@code isTextWithScriptlets} is true, this is the language used for
	 * scriptlets if none is specified.
	 */
	private String defaultLanguageTag;

	/**
	 * Whether to prepare the source code: preparation increases initialization
	 * time and reduces execution time; note that not all languages support
	 * preparation as a separate operation.
	 */
	private boolean prepare;

	/**
	 * The start delimiter (first option).
	 */
	private String delimiter1Start = DEFAULT_DELIMITER1_START;

	/**
	 * The end delimiter (first option).
	 */
	private String delimiter1End = DEFAULT_DELIMITER1_END;

	/**
	 * The start delimiter (second option).
	 */
	private String delimiter2Start = DEFAULT_DELIMITER2_START;

	/**
	 * The end delimiter (second option).
	 */
	private String delimiter2End = DEFAULT_DELIMITER2_END;

	/**
	 * The addition to the start delimiter to specify a comment scriptlet.
	 */
	private String delimiterComment = DEFAULT_DELIMITER_COMMENT;

	/**
	 * The addition to the start delimiter to specify an expression scriptlet.
	 */
	private String delimiterExpression = DEFAULT_DELIMITER_EXPRESSION;

	/**
	 * The addition to the start delimiter to specify an include scriptlet.
	 */
	private String delimiterInclude = DEFAULT_DELIMITER_INCLUDE;

	/**
	 * The addition to the start delimiter to specify an in-flow scriptlet.
	 */
	private String delimiterInFlow = DEFAULT_DELIMITER_IN_FLOW;

	/**
	 * A document source used to store in-flow scriptlets; can be null if
	 * in-flow scriptlets are not used.
	 */
	private DocumentSource<Executable> documentSource;

	/**
	 * The <code>executable</code> service name exposed to executables.
	 */
	private String exposedExecutableName = DEFAULT_EXECUTABLE_SERVICE_NAME;

	/**
	 * The scriptlet plugins.
	 */
	private final Map<String, ScriptletPlugin> scriptletPlugins = new HashMap<String, ScriptletPlugin>();
}
