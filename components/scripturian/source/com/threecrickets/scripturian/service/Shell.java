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

package com.threecrickets.scripturian.service;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionController;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Main;
import com.threecrickets.scripturian.ParserManager;
import com.threecrickets.scripturian.document.DocumentFileSource;
import com.threecrickets.scripturian.document.DocumentSource;

/**
 * Used as a provider for {@link ApplicationService} and {@link DocumentService}
 * instances.
 * 
 * @author Tal Liron
 */
public interface Shell
{
	/**
	 * The logger.
	 * 
	 * @return The logger
	 * @see #setLogger(Logger)
	 */
	public Logger getLogger();

	/**
	 * @param logger
	 *        The logger
	 * @see #getLogger()
	 */
	public void setLogger( Logger logger );

	/**
	 * The arguments sent to {@link Main#main(String[])}.
	 * 
	 * @return The arguments
	 */
	public String[] getArguments();

	/**
	 * The {@link LanguageManager} used to get language adapters for
	 * executables.
	 * 
	 * @return The language manager
	 */
	public LanguageManager getLanguageManager();

	/**
	 * The {@link ParserManager} used to get parsers for executables.
	 * 
	 * @return The parser manager
	 */
	public ParserManager getParserManager();

	/**
	 * Whether or not executables are prepared.
	 * 
	 * @return Whether to prepare executables.
	 */
	public boolean isPrepare();

	/**
	 * Used to load the executables. Defaults to a {@link DocumentFileSource}
	 * set for the current directory, with no validity checking.
	 * 
	 * @return The document source
	 */
	public DocumentSource<Executable> getSource();

	/**
	 * The additional document sources to use.
	 * 
	 * @return The library document sources
	 */
	public CopyOnWriteArrayList<DocumentSource<Executable>> getLibrarySources();

	/**
	 * An optional {@link ExecutionController} to be used with executables.
	 * Useful for exposing your own global variables to executables.
	 * 
	 * @return The execution controller
	 */
	public ExecutionController getExecutionController();
}
