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

import java.util.HashMap;
import java.util.Map;

import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.parser.ProgramParser;

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
	 * The default executable service name: "executable"
	 */
	public static final String DEFAULT_EXECUTABLE_SERVICE_NAME = "executable";

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
		attributes.putAll( parsingContext.getAttributes() );
		languageManager = parsingContext.getLanguageManager();
		parserManager = parsingContext.getParserManager();
		partition = parsingContext.getPartition();
		defaultLanguageTag = parsingContext.getDefaultLanguageTag();
		prepare = parsingContext.isPrepare();
		debug = parsingContext.isDebug();
		documentSource = parsingContext.getDocumentSource();
		exposedExecutableName = parsingContext.getExposedExecutableName();
	}

	//
	// Attributes
	//

	/**
	 * General-purpose attributes. Useful for configuring special parser
	 * features not supported by the context.
	 * 
	 * @return The attributes
	 */
	public Map<String, Object> getAttributes()
	{
		return attributes;
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
	 * @param languageManager
	 *        The language manager
	 * @see #getLanguageManager()
	 */
	public void setLanguageManager( LanguageManager languageManager )
	{
		this.languageManager = languageManager;
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
	 * @param parserManager
	 *        The parser manager
	 * @see #getParserManager()
	 */
	public void setParserManager( ParserManager parserManager )
	{
		this.parserManager = parserManager;
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
	 * The language to use if none is specified.
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
	 * The parser to use if none is specified.
	 * 
	 * @return The default language tag
	 */
	public String getDefaultParser()
	{
		return defaultParser;
	}

	/**
	 * @param defaultParser
	 *        The default language tag
	 * @see #getDefaultParser()
	 */
	public void setDefaultParser( String defaultParser )
	{
		this.defaultParser = defaultParser;
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
	 * Whether to debug the source code parsing.
	 * 
	 * @return The debug flag
	 */
	public boolean isDebug()
	{
		return debug;
	}

	/**
	 * @param debug
	 *        The debug flag
	 * @see #isDebug()
	 */
	public void setDebug( boolean debug )
	{
		this.debug = debug;
	}

	/**
	 * A document source used to store on-the-fly documents created during
	 * parsing.
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

	//
	// Object
	//

	@Override
	public String toString()
	{
		return "ParsingContext: " + partition + ", " + defaultLanguageTag + ", " + prepare + ", " + documentSource;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * General-purpose attributes. Useful for configuring special language
	 * features not supported by the context.
	 */
	private final Map<String, Object> attributes = new HashMap<String, Object>();

	/**
	 * The language manager used to parse, prepare and execute the executable.
	 */
	private LanguageManager languageManager;

	/**
	 * The parser manager used to parse the executable.
	 */
	private ParserManager parserManager;

	/**
	 * The executable partition.
	 */
	private String partition;

	/**
	 * The language to use if none is specified.
	 */
	private String defaultLanguageTag;

	/**
	 * The parser to use if none is specified.
	 */
	private String defaultParser = ProgramParser.NAME;

	/**
	 * Whether to prepare the source code: preparation increases initialization
	 * time and reduces execution time; note that not all languages support
	 * preparation as a separate operation.
	 */
	private boolean prepare;

	/**
	 * Whether to debug source code parsing.
	 */
	private boolean debug;

	/**
	 * A document source used to store on-the-fly documents created during
	 * parsing.
	 */
	private DocumentSource<Executable> documentSource;

	/**
	 * The <code>executable</code> service name exposed to executables.
	 */
	private String exposedExecutableName = DEFAULT_EXECUTABLE_SERVICE_NAME;
}
