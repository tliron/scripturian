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

package com.threecrickets.scripturian.adapter;

import java.util.Collection;

import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.core.util.ServiceLocator;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * A {@link LanguageAdapter} that supports various HTML markup languages as
 * implemented by <a
 * href="http://wiki.eclipse.org/Mylyn/Incubator/WikiText">Mylyn WikiText</a>.
 * 
 * @author Tal Liron
 */
public abstract class WikiTextAdapterBase extends LanguageAdapterBase
{
	//
	// Constructor
	//

	/**
	 * Constructor.
	 * 
	 * @param name
	 *        The name of the language adapter implementation
	 * @param version
	 *        The version of the language adapter implementation
	 * @param extensions
	 *        Standard source code filename extensions
	 * @param defaultExtension
	 *        Default source code filename extension
	 * @param tags
	 *        Language tags supported for scriptlets
	 * @param defaultTag
	 *        Default language tag used for scriptlets
	 * @throws LanguageAdapterException
	 *         In case of an initialization error
	 */
	public WikiTextAdapterBase( String name, String version, Collection<String> extensions, String defaultExtension, Collection<String> tags, String defaultTag ) throws LanguageAdapterException
	{
		super( "WikiText " + name, version, name, version, extensions, defaultExtension, tags, defaultTag );

		markupLanguage = ServiceLocator.getInstance().getMarkupLanguage( name );
	}

	//
	// LanguageAdapter
	//

	@Override
	public boolean isEphemeral()
	{
		return true;
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new WikiTextProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	protected final MarkupLanguage markupLanguage;
}
