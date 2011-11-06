/**
 * Copyright 2009-2011 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian;

/**
 * Allows the creation of custom scriptlets for text-with-scriptlets
 * {@link Executable} construction.
 * 
 * @author Tal Liron
 * @see ParsingContext#getScriptletPlugins()
 */
public interface ScriptletPlugin
{
	/**
	 * Generates an exectuable segment for a custom scriptlet.
	 * 
	 * @param code
	 *        The scriptlet code
	 * @param languageAdapter
	 *        The language adapter
	 * @param content
	 *        The scriptlet content
	 * @return The segment
	 */
	public String getScriptlet( String code, LanguageAdapter languageAdapter, String content );
}
