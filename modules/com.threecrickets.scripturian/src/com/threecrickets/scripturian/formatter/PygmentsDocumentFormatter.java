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

package com.threecrickets.scripturian.formatter;

import java.io.StringReader;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.threecrickets.scripturian.DocumentDescriptor;
import com.threecrickets.scripturian.DocumentFormatter;
import com.threecrickets.scripturian.internal.ExposedContainerForPygmentsDocumentFormatter;

/**
 * Use <a href="http://pygments.org/">Pygments</a> over Jython to format source
 * code.
 * 
 * @author Tal Liron
 */
public class PygmentsDocumentFormatter<D> implements DocumentFormatter<D>
{
	//
	// Construction
	//

	public PygmentsDocumentFormatter()
	{
		this( "vs" );
	}

	public PygmentsDocumentFormatter( String theme )
	{
		this.theme = theme;
	}

	//
	// DocumentFormatter
	//

	public String format( DocumentDescriptor<D> documentDescriptor, String title, int highlightLineNumber )
	{
		String tag = documentDescriptor.getTag();
		String language = null;
		if( "py".equals( tag ) )
			language = "python";
		else if( "rb".equals( tag ) )
			language = "ruby";
		else if( "gv".equals( tag ) || "groovy".equals( tag ) )
			language = "groovy";
		else if( "js".equals( tag ) )
			language = "javascript";
		else if( "clj".equals( tag ) )
			language = "clojure";
		else if( "php".equals( tag ) )
			language = "html+php";
		else if( "html".equals( tag ) )
			language = "html";
		else if( "xhtml".equals( tag ) )
			language = "html";
		else if( "xml".equals( tag ) )
			language = "xml";
		else if( "xslt".equals( tag ) )
			language = "xslt";

		if( language == null )
			return documentDescriptor.getSourceCode();

		ExposedContainerForPygmentsDocumentFormatter container = new ExposedContainerForPygmentsDocumentFormatter( documentDescriptor.getSourceCode(), highlightLineNumber, language, title, theme, "#dddddd", "#dddd00" );
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		ScriptEngine scriptEngine = scriptEngineManager.getEngineByName( "python" );
		ScriptContext scriptContext = scriptEngine.getContext();
		scriptContext.setAttribute( "container", container, ScriptContext.ENGINE_SCOPE );

		if( scriptEngine instanceof Compilable )
		{
			synchronized( program )
			{
				if( compiledScript == null )
				{
					try
					{
						compiledScript = ( (Compilable) scriptEngine ).compile( program );
					}
					catch( ScriptException x )
					{
					}
				}

				try
				{
					compiledScript.eval( scriptContext );
				}
				catch( ScriptException x )
				{
				}
			}
		}
		else
		{
			try
			{
				scriptEngine.eval( new StringReader( program ) );
			}
			catch( ScriptException x )
			{
			}
		}

		return container.getText();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final String program = "from pygments import highlight\n" + "from pygments.lexers import get_lexer_by_name\n" + "from pygments.formatters import HtmlFormatter\n"
		+ "lexer = get_lexer_by_name(container.language, stripall=True)\n" + "formatter = HtmlFormatter(full=True, linenos='table', hl_lines=(container.lineNumber,), title=container.title, style=container.style)\n"
		+ "formatter.style.background_color = container.background\n" + "formatter.style.highlight_color = container.highlight\n" + "container.text = highlight(container.text, lexer, formatter)\n";

	private volatile CompiledScript compiledScript;

	private final String theme;
}
