/**
 * Copyright 2009-2017 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com
 */

package com.threecrickets.scripturian.internal;

import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.exception.DocumentException;
import com.threecrickets.succinct.TemplateSource;
import com.threecrickets.succinct.TemplateSourceException;

/**
 * @author Tal Liron
 * @param <D>
 */
public class SuccinctTemplateSource<D> implements TemplateSource
{
	//
	// Construction
	//

	public SuccinctTemplateSource( DocumentSource<D> documentSource )
	{
		this.documentSource = documentSource;
	}

	//
	// TemplateSource
	//

	public String getTemplate( String name ) throws TemplateSourceException
	{
		try
		{
			return documentSource.getDocument( name ).getSourceCode();
		}
		catch( DocumentException x )
		{
			throw new TemplateSourceException( x );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final DocumentSource<D> documentSource;
}
