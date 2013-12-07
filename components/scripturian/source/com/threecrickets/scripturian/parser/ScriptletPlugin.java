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

package com.threecrickets.scripturian.parser;

import com.threecrickets.scripturian.LanguageAdapter;

/**
 * Allows the creation of custom scriptlets for {@link ScriptletsParser}.
 * 
 * @author Tal Liron
 */
public interface ScriptletPlugin
{
	/**
	 * Generates an executable segment for a custom scriptlet.
	 * 
	 * @param code
	 *        The scriptlet code
	 * @param languageAdapter
	 *        The last language adapter used
	 * @param content
	 *        The scriptlet content
	 * @return The segment
	 */
	public String getScriptlet( String code, LanguageAdapter languageAdapter, String content );
}
