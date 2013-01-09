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

package com.threecrickets.scripturian.adapter;

import java.util.Arrays;

import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.exception.LanguageAdapterException;

/**
 * A {@link LanguageAdapter} that supports the <a
 * href="http://trac.edgewall.org/wiki/WikiFormatting">TracWiki</a> HTML markup
 * language as implemented by <a
 * href="http://wiki.eclipse.org/Mylyn/Incubator/WikiText">Mylyn WikiText</a>.
 * 
 * @author Tal Liron
 */
public class WikiTextTracWikiAdapter extends WikiTextAdapterBase
{
	//
	// Construction
	//

	public WikiTextTracWikiAdapter() throws LanguageAdapterException
	{
		super( "TracWiki", "", Arrays.asList( "trac" ), "trac", Arrays.asList( "trac", "wikitext-trac" ), "wikitext-trac" );
	}
}
