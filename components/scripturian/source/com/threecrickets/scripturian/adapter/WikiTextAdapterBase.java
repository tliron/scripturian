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
