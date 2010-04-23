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
import com.threecrickets.scripturian.Main;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * This is the <code>executable.container</code> exposed by {@link Main}.
 * 
 * @author Tal Liron
 * @see Main
 */
public class ExposedContainerForMain
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
	public ExposedContainerForMain( Main main, ExecutionContext executionContext )
	{
		this.main = main;
		this.executionContext = executionContext;
	}

	//
	// Attributes
	//

	//
	// Attributes
	//

	/**
	 * An array of the string arguments sent to {@link Main#main(String[])}.
	 * 
	 * @return The arguments
	 */
	public String[] getArguments()
	{
		return main.getArguments();
	}

	/**
	 * For use with {@link #includeDocument(String)}, this is the default
	 * language tag used for scriptlets in case none is specified. Defaults to
	 * "js".
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
	 * Executes another executable, with the language determined according to
	 * the document's extension. Note that the executed executable does not have
	 * to be in same language as the executing executable.
	 * 
	 * @param documentName
	 *        The document name
	 * @throws IOException
	 * @throws ParsingException
	 * @throws ExecutionException
	 */
	public void include( String documentName ) throws IOException, ParsingException, ExecutionException
	{
		Executable executable = Executable.createOnce( documentName, main.getSource(), false, main.getLanguageManager(), defaultLanguageTag, false ).getDocument();
		executable.execute( executionContext, this, main.getExecutionController() );
	}

	/**
	 * As {@link #include(String)}, except that the included source code is
	 * parsed as a "text-with-scriptlets" executable.
	 * 
	 * @param documentName
	 *        The document name
	 * @throws IOException
	 * @throws ParsingException
	 * @throws ExecutionException
	 */
	public void includeDocument( String documentName ) throws IOException, ParsingException, ExecutionException
	{
		Executable executable = Executable.createOnce( documentName, main.getSource(), true, main.getLanguageManager(), defaultLanguageTag, false ).getDocument();
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

	private String defaultLanguageTag = "js";
}
