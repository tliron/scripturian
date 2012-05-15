/**
 * Copyright 2009-2012 Three Crickets LLC.
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
 * href="http://twiki.org/cgi-bin/view/TWiki/TextFormattingRules">TWiki</a> HTML
 * markup language as implemented by <a
 * href="http://wiki.eclipse.org/Mylyn/Incubator/WikiText">Mylyn WikiText</a>.
 * 
 * @author Tal Liron
 */
public class WikiTextTWikiAdapter extends WikiTextAdapterBase
{
	//
	// Construction
	//

	public WikiTextTWikiAdapter() throws LanguageAdapterException
	{
		super( "TWiki", "", Arrays.asList( "twiki" ), "twiki", Arrays.asList( "twiki", "wikitext-twiki" ), "wikitext-twiki" );
	}
}
