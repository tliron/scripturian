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

import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;

/**
 * Executable segments within executables. Scriptlets are used internally
 * between executables and languages adapters, and you would rarely need to
 * access them directly.
 * 
 * @author Tal Liron
 * @see Executable
 * @see LanguageAdapter
 */
public interface Scriptlet
{
	//
	// Attributes
	//

	/**
	 * The source code.
	 * 
	 * @return The source code
	 */
	public String getSourceCode();

	//
	// Operations
	//

	/**
	 * The optional "preparation" sub-phase is intended to speed up usage of
	 * later phases at the expense of higher cost during creation. It would be
	 * most useful if the executable is intended to be reused. In many
	 * implementations, "preparation" would involve compiling the code, and
	 * possibly caching the results on disk.
	 * 
	 * @throws PreparationException
	 */
	public void prepare() throws PreparationException;

	/**
	 * Executes the scriptlet.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @throws ParsingException
	 * @throws ExecutionException
	 */
	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException;
}
