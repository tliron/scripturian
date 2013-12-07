/**
 * Copyright 2009-2013 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.parser;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutableSegment;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.ParsingContext;
import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.exception.DocumentException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * By default, two scriptlet delimiting styles are supported: JSP/ASP style
 * (using percentage signs), and the PHP style (using question marks). However,
 * each document must adhere to only one style throughout.
 * <p>
 * In addition to regular scriptlets, Scripturian supports a few shorthand
 * scriptlets for common tasks:
 * <p>
 * The "comment scriptlet" (with a pound sign) is ignored.
 * <p>
 * The "expression scriptlet" (with an equals sign) causes the expression to be
 * sent to standard output. It allows for more readable templates.
 * <p>
 * The "include scriptlet" (with an ampersand) invokes the
 * <code>executable.container.include(name)</code> command as appropriate for
 * the language. Again, it allows for more readable templates.
 * <p>
 * Finally, the "in-flow scriptlet" (with a colon) works like a combination of
 * regular scriptlets with include scriptlets. "In-flow" scriptlets require the
 * use of a {@link DocumentSource}. Read the FAQ for more information.
 * <p>
 * Examples:
 * <ul>
 * <li><b>JSP/ASP-style delimiters</b>:
 * <code>&lt;% print('Hello World'); %&gt;</code></li>
 * <li><b>PHP-style delimiters</b>: <code>&lt;? document.cacheDuration.set 5000
 * ?&gt;</code></li>
 * <li><b>Specifying a language tag</b>:
 * <code>&lt;%groovy print myVariable %&gt; &lt;?php $executable-&gt;container-&gt;include(lib_name); ?&gt;</code>
 * </li>
 * <li><b>Output expression</b>: <code>&lt;?= 15 * 6 ?&gt;</code></li>
 * <li><b>Output expression with specifying a language tag</b>:
 * <code>&lt;?=js sqrt(myVariable) ?&gt;</code></li>
 * <li><b>Include</b>:
 * <code>&lt;%&amp; 'library.js' %&gt; &lt;?&amp; 'language-' + myObject.getLang + '-support.py' ?&gt;</code>
 * </li>
 * <li><b>Comment</b>: <code>&lt;%# This is ignored. %&gt;</code></li>
 * <li><b>In-flow</b>:
 * <code>&lt;%js if(isDebug) { %&gt; &lt;%:python dumpStack(); %&gt; &lt;% } %&gt;</code>
 * </li>
 * </ul>
 * 
 * @author Tal Liron
 * @see ScriptletPlugin
 */
public class ScriptletsParser extends MixedParser
{
	//
	// Constants
	//

	public final static String NAME = "scriptlets";

	/**
	 * The delimiters attribute for {@link ParsingContext}.
	 */
	public static final String DELIMITERS_ATTRIBUTE = ScriptletsParser.class.getCanonicalName() + ".delimiters";

	/**
	 * The delimiter comment attribute for {@link ParsingContext}.
	 */
	public static final String DELIMITER_COMMENT_ATTRIBUTE = ScriptletsParser.class.getCanonicalName() + ".delimiterComment";

	/**
	 * The delimiter expression attribute for {@link ParsingContext}.
	 */
	public static final String DELIMITER_EXPRESSION_ATTRIBUTE = ScriptletsParser.class.getCanonicalName() + ".delimiterExpression";

	/**
	 * The delimiter include attribute for {@link ParsingContext}.
	 */
	public static final String DELIMITER_INCLUDE_ATTRIBUTE = ScriptletsParser.class.getCanonicalName() + ".delimiterInclude";

	/**
	 * The delimiter in-flow attribute for {@link ParsingContext}.
	 */
	public static final String DELIMITER_IN_FLOW_ATTRIBUTE = ScriptletsParser.class.getCanonicalName() + ".delimiterInFlow";

	/**
	 * The plugins attribute for {@link ParsingContext}.
	 */
	public static final String PLUGINS_ATTRIBUTE = ScriptletsParser.class.getCanonicalName() + ".plugins";

	/**
	 * The default delimiters.
	 */
	public static final String[][] DEFAULT_DELIMITERS = new String[][]
	{
		{
			"<%", "%>"
		},
		{
			"<?", "?>"
		}
	};

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
	 * scriptlet: &amp;
	 */
	public static final String DEFAULT_DELIMITER_INCLUDE = "&";

	/**
	 * The default addition to the start delimiter to specify an in-flow
	 * scriptlet: :
	 */
	public static final String DEFAULT_DELIMITER_IN_FLOW = ":";

	//
	// Static operations
	//

	/**
	 * Checks if the source code is an entire scriptlet, according to the
	 * {@link ScriptletsParser#DEFAULT_DELIMITERS}.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @return True is the source code is a scriptlet
	 */
	public static boolean isScriptlet( String sourceCode )
	{
		return isScriptlet( sourceCode, DEFAULT_DELIMITERS );
	}

	/**
	 * Checks if the source code is an entire scriptlet, according to the
	 * parsing context.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param parsingContext
	 *        The parsing context
	 * @return True is the source code is a scriptlet
	 */
	public static boolean isScriptlet( String sourceCode, ParsingContext parsingContext )
	{
		Map<String, Object> attributes = parsingContext.getAttributes();
		String[][] delimiters = (String[][]) attributes.get( DELIMITERS_ATTRIBUTE );
		if( delimiters == null )
			delimiters = DEFAULT_DELIMITERS;
		return isScriptlet( sourceCode, delimiters );
	}

	/**
	 * Checks if the source code is an entire scriptlet, according to the
	 * provided delimiters.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param delimiters
	 *        The delimiters
	 * @return True is the source code is a scriptlet
	 */
	public static boolean isScriptlet( String sourceCode, String[][] delimiters )
	{
		for( String[] delimiterPair : delimiters )
		{
			if( sourceCode.startsWith( delimiterPair[0] ) && sourceCode.endsWith( delimiterPair[1] ) )
				return true;
		}
		return false;
	}

	//
	// Parser
	//

	public String getName()
	{
		return NAME;
	}

	public Collection<ExecutableSegment> parse( String sourceCode, ParsingContext parsingContext, Executable executable ) throws ParsingException, DocumentException
	{
		String documentName = executable.getDocumentName();
		Map<String, Object> attributes = parsingContext.getAttributes();

		String[][] delimiters = (String[][]) attributes.get( DELIMITERS_ATTRIBUTE );
		if( delimiters == null )
			delimiters = DEFAULT_DELIMITERS;

		String delimiterStart = null;
		String delimiterEnd = null;
		int delimiterStartLength = 0;
		int delimiterEndLength = 0;

		int start = -1;
		int startLineNumber = 1;
		int startColumnNumber = 1;
		int lastLineNumber = 1;
		int lastColumnNumber = 1;

		// Detect type of delimiter
		for( String[] delimiterPair : delimiters )
		{
			if( delimiterPair.length != 2 )
				throw new ParsingException( documentName );

			start = sourceCode.indexOf( delimiterPair[0] );
			if( start != -1 )
			{
				delimiterStart = delimiterPair[0];
				delimiterEnd = delimiterPair[1];
				delimiterStartLength = delimiterStart.length();
				delimiterEndLength = delimiterEnd.length();

				// Start at first delimiter
				for( int i = sourceCode.indexOf( '\n' ); i >= 0 && i < start; i = sourceCode.indexOf( '\n', i + 1 ) )
					startLineNumber++;
				break;
			}
		}

		@SuppressWarnings("unchecked")
		Map<String, ScriptletPlugin> plugins = (Map<String, ScriptletPlugin>) attributes.get( PLUGINS_ATTRIBUTE );

		String delimiterComment = (String) attributes.get( DELIMITER_COMMENT_ATTRIBUTE );
		if( delimiterComment == null )
			delimiterComment = DEFAULT_DELIMITER_COMMENT;
		int commentLength = delimiterComment.length();

		String delimiterExpression = (String) attributes.get( DELIMITER_EXPRESSION_ATTRIBUTE );
		if( delimiterExpression == null )
			delimiterExpression = DEFAULT_DELIMITER_EXPRESSION;
		int expressionLength = delimiterExpression.length();

		String delimiterInclude = (String) attributes.get( DELIMITER_INCLUDE_ATTRIBUTE );
		if( delimiterInclude == null )
			delimiterInclude = DEFAULT_DELIMITER_INCLUDE;
		int includeLength = delimiterInclude.length();

		String delimiterInFlow = (String) attributes.get( DELIMITER_IN_FLOW_ATTRIBUTE );
		if( delimiterInFlow == null )
			delimiterInFlow = DEFAULT_DELIMITER_IN_FLOW;
		int inFlowLength = delimiterInFlow.length();

		int length = sourceCode.length();

		LanguageManager languageManager = parsingContext.getLanguageManager();
		String lastLanguageTag = parsingContext.getDefaultLanguageTag();
		LanguageAdapter lastAdapter = languageManager.getAdapterByTag( lastLanguageTag );
		DocumentSource<Executable> documentSource = parsingContext.getDocumentSource();

		List<ExecutableSegment> segments = new LinkedList<ExecutableSegment>();

		// Parse segments
		if( start != -1 )
		{
			int last = 0;

			while( start != -1 )
			{
				// Add previous literal segment
				if( start != last )
					segments.add( new ExecutableSegment( sourceCode.substring( last, start ), lastLineNumber, lastColumnNumber, false, false, lastLanguageTag ) );

				start += delimiterStartLength;

				int end = sourceCode.indexOf( delimiterEnd, start );
				if( end == -1 )
					throw new ParsingException( documentName, startLineNumber, startColumnNumber, "Scriptlet does not have an ending delimiter" );

				if( start != end )
				{
					String languageTag = lastLanguageTag;
					LanguageAdapter adapter = lastAdapter;

					boolean isComment = false;
					boolean isExpression = false;
					boolean isInclude = false;
					boolean isInFlow = false;
					boolean isEphemeral = false;
					String pluginCode = null;
					ScriptletPlugin plugin = null;

					// Check if this is a plugin
					if( plugins != null )
					{
						for( Map.Entry<String, ScriptletPlugin> scriptletPlugin : plugins.entrySet() )
						{
							pluginCode = scriptletPlugin.getKey();
							int codeLength = pluginCode.length();
							if( ( start + codeLength <= end ) && sourceCode.substring( start, start + codeLength ).equals( pluginCode ) )
							{
								plugin = scriptletPlugin.getValue();
								start += codeLength;
								break;
							}
						}
					}

					if( plugin == null )
					{
						// Check if this is a comment
						if( ( start + commentLength <= end ) && sourceCode.substring( start, start + commentLength ).equals( delimiterComment ) )
						{
							start += commentLength;
							isComment = true;
						}
						// Check if this is an expression
						else if( ( start + expressionLength <= end ) && sourceCode.substring( start, start + expressionLength ).equals( delimiterExpression ) )
						{
							start += expressionLength;
							isExpression = true;
						}
						// Check if this is an include
						else if( ( start + includeLength <= end ) && sourceCode.substring( start, start + includeLength ).equals( delimiterInclude ) )
						{
							start += includeLength;
							isInclude = true;
						}
						// Check if this is an in-flow
						else if( ( start + inFlowLength <= end ) && sourceCode.substring( start, start + inFlowLength ).equals( delimiterInFlow ) )
						{
							start += inFlowLength;
							isInFlow = true;
						}
					}

					// Get language tag if available (ends in whitespace or end
					// delimiter)
					int endLanguageTag = start;
					while( endLanguageTag < end )
					{
						if( Character.isWhitespace( sourceCode.charAt( endLanguageTag ) ) )
							break;

						endLanguageTag++;
					}
					if( endLanguageTag > start + 1 )
					{
						languageTag = sourceCode.substring( start, endLanguageTag );

						// Optimization: in-flow is unnecessary if we are in the
						// same language
						if( isInFlow && lastLanguageTag.equals( languageTag ) )
							isInFlow = false;

						start = endLanguageTag + 1;
					}

					if( !isComment )
					{
						String segment = end > start + 1 ? sourceCode.substring( start, end ) : "";

						if( plugin != null )
						{
							// Our plugin scriptlet is in the last language
							languageTag = lastLanguageTag;

							adapter = languageManager.getAdapterByTag( languageTag );
							if( adapter == null )
								throw ParsingException.adapterNotFound( documentName, startLineNumber, startColumnNumber, languageTag );

							segment = plugin.getScriptlet( pluginCode, adapter, segment );
						}
						else
						{
							adapter = languageManager.getAdapterByTag( languageTag );
							if( adapter == null )
								throw ParsingException.adapterNotFound( documentName, startLineNumber, startColumnNumber, languageTag );

							if( isExpression )
								segment = adapter.getSourceCodeForExpressionOutput( segment, executable );
							else if( isInclude )
								segment = adapter.getSourceCodeForExpressionInclude( segment, executable );
							else if( isInFlow && ( documentSource != null ) )
							{
								String inFlowCode = delimiterStart + languageTag + " " + segment + delimiterEnd;
								String inFlowName = Executable.createOnTheFlyDocumentName();

								// Note that the in-flow executable is a single
								// segment, so we can optimize parsing a bit
								Executable inFlowExecutable = new Executable( documentName + "/" + inFlowName, executable.getDocumentTimestamp(), inFlowCode, NAME, parsingContext );
								documentSource.setDocument( inFlowName, inFlowCode, "", inFlowExecutable );

								// TODO: would it ever be possible to remove the
								// dependent in-flow instances?

								// Our include scriptlet is in the last language
								languageTag = lastLanguageTag;
								segment = lastAdapter.getSourceCodeForExpressionInclude( "\"" + inFlowName + "\"", executable );
							}

							isEphemeral = adapter.isEphemeral();
						}

						if( segment != null )
							segments.add( new ExecutableSegment( segment, startLineNumber, startColumnNumber, true, true, languageTag ) );
					}

					if( !isInFlow && !isEphemeral )
					{
						lastLanguageTag = languageTag;
						lastAdapter = adapter;
					}
				}

				last = end + delimiterEndLength;
				lastLineNumber = startLineNumber;
				lastColumnNumber = startColumnNumber;
				start = sourceCode.indexOf( delimiterStart, last );
				if( start != -1 )
					for( int i = sourceCode.indexOf( '\n', last ); i >= 0 && i < start; i = sourceCode.indexOf( '\n', i + 1 ) )
						startLineNumber++;
			}

			// Add remaining literal segment
			if( last < length )
				segments.add( new ExecutableSegment( sourceCode.substring( last ), lastLineNumber, lastColumnNumber, false, false, lastLanguageTag ) );
		}
		else
		{
			// Trivial executable: does not contain scriptlets
			ExecutableSegment segment = new ExecutableSegment( sourceCode, 1, 1, false, false, null );
			return Collections.singleton( segment );
		}

		return optimize( segments, parsingContext, executable );
	}
}
