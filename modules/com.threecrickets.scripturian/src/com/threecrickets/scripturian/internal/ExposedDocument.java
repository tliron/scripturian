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

package com.threecrickets.scripturian.internal;

import java.io.IOException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Main;
import com.threecrickets.scripturian.exception.DocumentException;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * This is the <code>document</code> exposed by {@link Main}.
 * 
 * @author Tal Liron
 * @see Main
 */
public class ExposedDocument
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param main
	 *        The main instance
	 * @param executionContext
	 *        The execution context
	 */
	public ExposedDocument( Main main, ExecutionContext executionContext )
	{
		this.main = main;
		this.executionContext = executionContext;
	}

	//
	// Attributes
	//

	/**
	 * For use with {@link #include(String)}, this is the default language tag
	 * used for scriptlets in case none is specified. Defaults to "js".
	 * 
	 * @return The default script language tag
	 * @see #setDefaultLanguageTag(String)
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

	//
	// Operations
	//

	/**
	 * Executes a source code document. The language of the source code will be
	 * determined by the document tag, which is usually the filename extension.
	 * 
	 * @param documentName
	 *        The document name
	 * @throws ParsingException
	 * @throws ExecutionException
	 * @throws DocumentException
	 * @throws IOException
	 * @see LanguageManager#getAdapterByExtension(String, String)
	 */
	public void execute( String documentName ) throws ParsingException, ExecutionException, DocumentException, IOException
	{
		Executable executable = Executable.createOnce( documentName, main.getSource(), false, main.getManager(), defaultLanguageTag, main.isPrepare() ).getDocument();
		executable.execute( executionContext, this, main.getExecutionController() );
	}

	/**
	 * Includes a text document into the current location. The document may be a
	 * "text-with-scriptlets" executable, in which case its output could be
	 * dynamically generated.
	 * 
	 * @param documentName
	 *        The document name
	 * @throws ParsingException
	 * @throws ExecutionException
	 * @throws DocumentException
	 * @throws IOException
	 */
	public void include( String documentName ) throws ParsingException, ExecutionException, DocumentException, IOException
	{
		Executable executable = Executable.createOnce( documentName, main.getSource(), true, main.getManager(), defaultLanguageTag, main.isPrepare() ).getDocument();
		executable.execute( executionContext, this, main.getExecutionController() );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The main instance.
	 */
	private final Main main;

	/**
	 * The execution context.
	 */
	private final ExecutionContext executionContext;

	/**
	 * The default language tag.
	 */
	private String defaultLanguageTag = "js";
}
