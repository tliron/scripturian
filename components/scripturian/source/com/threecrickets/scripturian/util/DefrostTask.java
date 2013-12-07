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

package com.threecrickets.scripturian.util;

import java.util.Collection;
import java.util.concurrent.Callable;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.ParserManager;
import com.threecrickets.scripturian.ParsingContext;
import com.threecrickets.scripturian.document.DocumentDescriptor;
import com.threecrickets.scripturian.document.DocumentSource;

/**
 * A {@link Callable} that makes sure that a {@link Executable} is loaded and
 * possibly prepared, making it ready to execute without delay.
 * <p>
 * It may be easier to use a {@link Defroster}, which is at a higher level than
 * this class.
 * 
 * @author Tal Liron
 * @see Defroster
 */
public class DefrostTask implements Callable<Executable>
{
	//
	// Static operations
	//

	/**
	 * Creates a defrost task for each document descriptor in a document source.
	 * 
	 * @param documentSource
	 *        The document source for executables
	 * @param languageManager
	 *        The language manager for executable initialization
	 * @param parserManager
	 *        The parser manager for executable initialization
	 * @param defaultLanguageTag
	 *        The language tag to used if none is specified
	 * @param parserName
	 *        The parser to use, or null for the default parser
	 * @param prepare
	 *        Whether to prepare executables
	 * @param debug
	 *        Whether to debug the source code parsing
	 * @return An array of tasks
	 * @see DocumentSource#getDocument(String)
	 */
	public static DefrostTask[] forDocumentSource( DocumentSource<Executable> documentSource, LanguageManager languageManager, ParserManager parserManager, String defaultLanguageTag, String parserName, boolean prepare,
		boolean debug )
	{
		Collection<DocumentDescriptor<Executable>> documentDescriptors = documentSource.getDocuments();
		DefrostTask[] defrostTasks = new DefrostTask[documentDescriptors.size()];
		int i = 0;
		for( DocumentDescriptor<Executable> documentDescriptor : documentDescriptors )
			defrostTasks[i++] = new DefrostTask( documentDescriptor, documentSource, languageManager, parserManager, defaultLanguageTag, parserName, prepare, debug );

		return defrostTasks;
	}

	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @param documentDescriptor
	 *        The document descriptor for the executable
	 * @param documentSource
	 *        The document source for the executables, required for processing
	 *        in-flow tags during executable initialization
	 * @param languageManager
	 *        The language manager for executable initialization
	 * @param parserManager
	 *        The parser manager for executable initialization
	 * @param defaultLanguageTag
	 *        The language tag to used if none is specified
	 * @param parserName
	 *        The parser to use, or null for the default parser
	 * @param prepare
	 *        Whether to prepare executables
	 * @param debug
	 *        Whether to debug the source code parsing
	 */
	public DefrostTask( DocumentDescriptor<Executable> documentDescriptor, DocumentSource<Executable> documentSource, LanguageManager languageManager, ParserManager parserManager, String defaultLanguageTag,
		String parserName, boolean prepare, boolean debug )
	{
		this.documentDescriptor = documentDescriptor;
		this.documentSource = documentSource;
		this.languageManager = languageManager;
		this.parserManager = parserManager;
		this.defaultLanguageTag = defaultLanguageTag;
		this.parserName = parserName;
		this.prepare = prepare;
		this.debug = debug;
	}

	//
	// Callable
	//

	public Executable call() throws Exception
	{
		Executable executable = documentDescriptor.getDocument();

		if( executable == null )
		{
			LanguageAdapter adapter = languageManager.getAdapterByExtension( documentDescriptor.getDefaultName(), documentDescriptor.getTag() );
			ParsingContext parsingContext = new ParsingContext();
			parsingContext.setLanguageManager( languageManager );
			parsingContext.setParserManager( parserManager );
			parsingContext.setDefaultLanguageTag( adapter != null ? (String) adapter.getAttributes().get( LanguageAdapter.DEFAULT_TAG ) : defaultLanguageTag );
			parsingContext.setPrepare( prepare );
			parsingContext.setDebug( debug );
			parsingContext.setDocumentSource( documentSource );
			executable = Executable.createOnce( documentDescriptor, parserName, parsingContext );
		}

		return executable;
	}

	//
	// Object
	//

	@Override
	public String toString()
	{
		return "DefrosterTask: " + documentDescriptor;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The document descriptor for the executable.
	 */
	private final DocumentDescriptor<Executable> documentDescriptor;

	/**
	 * The document source for the executables, required for processing in-flow
	 * tags during executable initialization.
	 */
	private final DocumentSource<Executable> documentSource;

	/**
	 * The language manager for executable initialization.
	 */
	private final LanguageManager languageManager;

	/**
	 * The parser manager for executable initialization.
	 */
	private final ParserManager parserManager;

	/**
	 * The language tag to used if none is specified.
	 */
	private final String defaultLanguageTag;

	/**
	 * The parser to use, or null for the default parser.
	 */
	private final String parserName;

	/**
	 * Whether to prepare executables.
	 */
	private final boolean prepare;

	/**
	 * Whether to debug the source code parsing.
	 */
	private final boolean debug;
}
