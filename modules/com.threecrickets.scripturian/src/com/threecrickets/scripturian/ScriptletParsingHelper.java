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

import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptEngine;

/**
 * This interface handles additional tasks specific to parsing scriptlets, which
 * are not covered by standard Java scripting support. It must be implemented
 * for every script engine used by {@link Document}.
 * 
 * @author Tal Liron
 * @see Document
 */
public interface ScriptletParsingHelper
{
	/**
	 * Some languages support their own printing facilities, while others don't.
	 * Returning true here will delegate print handling to {@link Document}.
	 * 
	 * @return True if should print on eval
	 */
	public boolean isPrintOnEval();

	/**
	 * Though some scripting engines support {@link Compilable}, their
	 * implementation is broken for our purposes. This allows us to bypass
	 * compilation.
	 * 
	 * @return True if compilation is allowed
	 */
	public boolean isCompilable();

	/**
	 * The header is inserted at the beginning of every script. It is useful for
	 * appropriately setting up the script's environment.
	 * 
	 * @param document
	 *        The composite script instance
	 * @param scriptEngine
	 *        The script engine
	 * @return The header or null
	 */
	public String getScriptletHeader( Document document, ScriptEngine scriptEngine );

	/**
	 * The footer is appended to the end of every script. It is useful for
	 * cleaning up resources created in the header.
	 * 
	 * @param document
	 *        The composite script instance
	 * @param scriptEngine
	 *        The script engine
	 * @return The footer or null
	 * @see #getScriptletHeader(Document, ScriptEngine)
	 */
	public String getScriptletFooter( Document document, ScriptEngine scriptEngine );

	/**
	 * Turns text into a command or series of commands to print the text to
	 * standard output. Note that the text is of arbitrary length, can span
	 * multiple lines, and include arbitrary characters. The parsing helper
	 * makes sure to escape special characters, partition long text, etc.
	 * 
	 * @param document
	 *        The composite script instance
	 * @param scriptEngine
	 *        The script engine
	 * @param content
	 *        The content
	 * @return A command or series of commands to print the content
	 */
	public String getTextAsProgram( Document document, ScriptEngine scriptEngine, String content );

	/**
	 * Turns an expression into a command or series of commands to print the
	 * evaluation of the expression to standard output. In most cases this
	 * simply involves wrapping the expression in something like a print
	 * command.
	 * 
	 * @param document
	 *        The composite script instance
	 * @param scriptEngine
	 *        The script engine
	 * @param content
	 *        The content
	 * @return A command or series of commands to print the expression
	 */
	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content );

	/**
	 * Turns an expression into a command or series of commands to include the
	 * script named for the result of the evaluation of the expression. Note
	 * that this requires the script to have access to a global method named
	 * <code>documentnt.container.includeDocument</code>.
	 * 
	 * @param document
	 *        The composite script instance
	 * @param scriptEngine
	 *        The script engine
	 * @param content
	 *        The content
	 * @return A command or series of commands to include the script named for
	 *         the expression
	 * @see Document#getDocumentVariableName()
	 */
	public String getExpressionAsInclude( Document document, ScriptEngine scriptEngine, String content );

	/**
	 * Creates a command or series of commands to invoke an entry point
	 * (function, method, closure, etc.) in the script. Note that for engines
	 * that implement {@link Invocable} you must return null.
	 * 
	 * @param document
	 *        The composite script instance
	 * @param scriptEngine
	 *        The script engine
	 * @param content
	 *        The content
	 * @return A command or series of commands to call the entry point, or null
	 *         to signify that {@link Invocable} should be used
	 */
	public String getInvocationAsProgram( Document document, ScriptEngine scriptEngine, String content );
}
