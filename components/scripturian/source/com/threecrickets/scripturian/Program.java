/**
 * Copyright 2009-2016 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
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
 * Operational units within executables. Programs are completely executable
 * units: from the perspective of source code, it means that programs are not
 * fragments or snippets of code. For example, a for-loop in Java without an
 * ending curly bracket is un-compilable as a whole and would thus not
 * constitute a valid program.
 * <p>
 * More power, flexibility and abstraction can be obtained by working with
 * higher-level {@link Executable} instances, which may internally incorporate
 * one or more programs in various languages, rather than working with programs
 * directly. Programs should be seen as an implementation-specific aspects of
 * executables.
 * 
 * @author Tal Liron
 * @see Executable
 * @see LanguageAdapter
 */
public interface Program
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
	 *         In case of a preparation error
	 */
	public void prepare() throws PreparationException;

	/**
	 * Executes the program.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws ExecutionException
	 *         In case of an execution error
	 */
	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException;
}
