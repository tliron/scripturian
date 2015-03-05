/**
 * Copyright 2009-2015 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.formatter;

import java.io.IOException;
import java.io.StringWriter;

import com.threecrickets.jygments.Jygments;
import com.threecrickets.jygments.ResolutionException;
import com.threecrickets.jygments.format.Formatter;
import com.threecrickets.jygments.grammar.Lexer;
import com.threecrickets.scripturian.document.DocumentDescriptor;
import com.threecrickets.scripturian.document.DocumentFormatter;

/**
 * Use <a href="http://threecrickets.com/jygments/">Jygments</a> to format
 * source code.
 * 
 * @author Tal Liron
 */
public class JygmentsDocumentFormatter<D> implements DocumentFormatter<D>
{
	//
	// Construction
	//

	/**
	 * Constructor. The theme used is "vs".
	 */
	public JygmentsDocumentFormatter()
	{
		this( "vs" );
	}

	/**
	 * Constructor.
	 * 
	 * @param theme
	 *        The theme to use
	 */
	public JygmentsDocumentFormatter( String theme )
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

		StringWriter r = new StringWriter();
		Lexer lexer;
		try
		{
			lexer = Lexer.getByName( language );
			Formatter formatter = Formatter.getByName( "html" );
			Jygments.highlight( documentDescriptor.getSourceCode(), lexer, formatter, r );
			return r.toString();
		}
		catch( ResolutionException x )
		{
			return documentDescriptor.getSourceCode();
		}
		catch( IOException x )
		{
			return documentDescriptor.getSourceCode();
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	@SuppressWarnings("unused")
	private final String theme;
}
