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

import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * Allows execution of source code in a supported language.
 * <p>
 * Language adapters are usually accessed via a {@link LanguageManager}
 * instance.
 * 
 * @author Tal Liron
 * @see LanguageManager
 */
public interface LanguageAdapter
{
	//
	// Constants
	//

	/**
	 * Attribute for the name of the language adapter implementation.
	 */
	public static final String NAME = "name";

	/**
	 * Attribute for the version of the language adapter implementation.
	 */
	public static final String VERSION = "version";

	/**
	 * Attribute for the name of the implemented language.
	 */
	public static final String LANGUAGE_NAME = "language.name";

	/**
	 * Attribute for the version of the implemented language.
	 */
	public static final String LANGUAGE_VERSION = "language.version";

	/**
	 * Attribute for standard source code filename extensions.
	 */
	public static final String EXTENSIONS = "extensions";

	/**
	 * Attribute for default source code filename extension.
	 */
	public static final String DEFAULT_EXTENSION = "extension.default";

	/**
	 * Attribute for language tags supported for scriptlets.
	 */
	public static final String TAGS = "tags";

	/**
	 * Default language tag used for scriptlets.
	 */
	public static final String DEFAULT_TAG = "tag.default";

	//
	// Attributes
	//

	/**
	 * The language manager.
	 * 
	 * @return The language manager
	 */
	public LanguageManager getManager();

	/**
	 * The language manager.
	 * 
	 * @param manager
	 *        The manager
	 */
	public void setManager( LanguageManager manager );

	/**
	 * Adapter attributes. Adapter must at least support the keys listed in this
	 * interface, but may support their own special keys.
	 * 
	 * @return The attributes
	 */
	public Map<String, Object> getAttributes();

	/**
	 * Some languages or their script engines are inherently broken when called
	 * by multiple threads. Returning false here makes sure Scripturian respects
	 * this and blocks.
	 * 
	 * @return True if we support being run by concurrent threads
	 */
	public boolean isThreadSafe();

	/**
	 * Some languages are meant for use in one segment only. Return true here
	 * makes sure that Scripturian will not change the current language tag
	 * after each segment parsed by this adapter.
	 * 
	 * @return True if the current language tag should not change
	 */
	public boolean isEphemeral();

	/**
	 * Used when {@link #isThreadSafe()} returns false.
	 * 
	 * @return The lock
	 */
	public Lock getLock();

	/**
	 * Creates source code for outputting literal text to standard output.
	 * 
	 * @param literal
	 *        The literal text
	 * @param executable
	 *        The executable
	 * @return Source code
	 * @throws ParsingException
	 *         In case of a parsing error
	 */
	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException;

	/**
	 * Creates source code for outputting a source code expression to standard
	 * output.
	 * 
	 * @param expression
	 *        The source code expression
	 * @param executable
	 *        The executable
	 * @return Source code
	 * @throws ParsingException
	 *         In case of a parsing error
	 */
	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException;

	/**
	 * Creates source code for including the output of a text-with-scriptlets
	 * document.
	 * <p>
	 * For this to work, the executable must have been created with a container
	 * that supports an inclusion command named according to the language
	 * manager attribute
	 * {@link LanguageManager#CONTAINER_INCLUDE_COMMAND_ATTRIBUTE}.
	 * 
	 * @param expression
	 *        The source code expression
	 * @param executable
	 *        The executable
	 * @return Source code
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @see LanguageManager#getAttributes()
	 */
	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException;

	//
	// Operations
	//

	/**
	 * Turns source code into a program. The intent is for the implementation to
	 * perform the bare minimum required for detecting errors in the source
	 * code. For better initialization, likely at the cost of extra processing
	 * up front, call {@link Program#prepare()} on the program.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param isScriptlet
	 *        Whether the source code is a scriptlet
	 * @param position
	 *        The program's position in the executable
	 * @param startLineNumber
	 *        The line number in the document for where the program's source
	 *        code begins
	 * @param startColumnNumber
	 *        The column number in the document for where the program's source
	 *        code begins
	 * @param executable
	 *        The executable
	 * @return A program
	 * @throws ParsingException
	 *         In case of a parsing error
	 */
	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException;

	/**
	 * Enters the executable at a stored, named location. According to the
	 * language, the entry point can be a function, method, lambda, closure,
	 * etc.
	 * 
	 * @param entryPointName
	 *        The entry point name
	 * @param executable
	 *        The executable
	 * @param executionContext
	 *        The executable context
	 * @param arguments
	 *        Optional state to pass to the entry point
	 * @return State returned from to the entry point or null
	 * @throws NoSuchMethodException
	 *         In case the entry point does not exist
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws ExecutionException
	 *         In case of an execution error
	 */
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException;

	/**
	 * Cleans up any resources used for an execution context.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @see ExecutionContext#getAttributes()
	 */
	public void releaseContext( ExecutionContext executionContext );
}
