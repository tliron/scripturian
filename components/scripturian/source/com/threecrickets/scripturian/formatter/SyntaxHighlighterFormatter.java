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

package com.threecrickets.scripturian.formatter;

import com.threecrickets.scripturian.document.DocumentDescriptor;
import com.threecrickets.scripturian.document.DocumentFormatter;

/**
 * Use <a href="http://alexgorbatchev.com/wiki/SyntaxHighlighter">Syntax
 * Highligher</a> to format source code.
 * 
 * @author Tal Liron
 */
public class SyntaxHighlighterFormatter<D> implements DocumentFormatter<D>
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 */
	public SyntaxHighlighterFormatter()
	{
		this( "syntaxhighlighter/", "Midnight" );
	}

	/**
	 * Constructor.
	 * 
	 * @param baseUrl
	 *        Base URL for syntaxhighlighter (can be relative)
	 * @param theme
	 *        Theme to use
	 */
	public SyntaxHighlighterFormatter( String baseUrl, String theme )
	{
		this.baseUrl = baseUrl;
		this.theme = theme;
	}

	//
	// DocumentFormatter
	//

	public String format( DocumentDescriptor<D> documentDescriptor, String title, int highlightLineNumber )
	{
		String tag = documentDescriptor.getTag();

		String brush = null, alias = tag;
		if( "py".equals( tag ) )
			brush = "Python";
		else if( "rb".equals( tag ) )
			brush = "Ruby";
		else if( "gv".equals( tag ) || "groovy".equals( tag ) )
		{
			brush = "Groovy";
			alias = "groovy";
		}
		else if( "js".equals( tag ) )
			brush = "JScript";
		else if( "clj".equals( tag ) )
			brush = "Clojure";
		else if( "php".equals( tag ) )
			brush = "Php";
		else if( "html".equals( tag ) )
			brush = "Xml";
		else if( "xhtml".equals( tag ) )
			brush = "Xml";
		else if( "xml".equals( tag ) )
			brush = "Xml";
		else if( "xslt".equals( tag ) )
			brush = "Xml";

		if( brush == null )
			return documentDescriptor.getSourceCode();

		StringBuilder html = new StringBuilder();
		html.append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" );
		html.append( "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n" );
		html.append( "  <head>\n" );
		html.append( "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />" );
		html.append( "    <title>" + title + "</title>\n" );
		html.append( "    <link href=\"" + baseUrl + "styles/shCore.css\" rel=\"stylesheet\" type=\"text/css\" />\n" );
		html.append( "    <link href=\"" + baseUrl + "styles/shTheme" + theme + ".css\" rel=\"stylesheet\" type=\"text/css\" />\n" );
		html.append( "    <script type=\"text/javascript\" src=\"" + baseUrl + "src/shCore.js\"></script>\n" );
		html.append( "    <script type=\"text/javascript\" src=\"" + baseUrl + "scripts/shBrush" + brush + ".js\"></script>\n" );
		html.append( "    <script type=\"text/javascript\">\n" );
		html.append( "      SyntaxHighlighter.config.clipboardSwf = '" + baseUrl + "scripts/clipboard.swf';" );
		html.append( "      SyntaxHighlighter.all();\n" );
		html.append( "    </script>\n" );
		html.append( "  </head>\n" );
		html.append( "  <body>\n" );
		html.append( "\n" );
		html.append( "    <noscript>" );
		html.append( "      You must enable JavaScript in your browser in order to see the source code." );
		html.append( "    </noscript>" );
		html.append( "<script type=\"syntaxhighlighter\" class=\"brush: " + alias + ";\"><![CDATA[" );
		html.append( documentDescriptor.getSourceCode() );
		html.append( "]]></script>\n" );
		html.append( "\n" );
		html.append( "  </body>\n" );
		html.append( "</html>\n" );

		return html.toString();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The theme.
	 */
	private final String theme;

	/**
	 * The base URL.
	 */
	private final String baseUrl;
}
