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

package com.threecrickets.scripturian.document;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.threecrickets.scripturian.exception.DocumentException;

/**
 * Chains one or more document sources in order.
 * 
 * @author Tal Liron
 * @param <D>
 *        The document type
 */
public class ChainDocumentSource<D> implements DocumentSource<D>
{
	//
	// Construction

	public ChainDocumentSource( String identifier )
	{
		this.identifier = identifier;
	}

	//
	// Attributes
	//

	public List<DocumentSource<D>> getSources()
	{
		return sources;
	}

	//
	// DocumentSource
	//

	public DocumentDescriptor<D> getDocument( String documentName ) throws DocumentException
	{
		for( DocumentSource<D> documentSource : sources )
		{
			DocumentDescriptor<D> descriptor = documentSource.getDocument( documentName );
			if( descriptor != null )
				return descriptor;
		}
		return null;
	}

	public DocumentDescriptor<D> setDocument( String documentName, String sourceCode, String tag, D document ) throws DocumentException
	{
		return sources.iterator().next().setDocument( documentName, sourceCode, tag, document );
	}

	public DocumentDescriptor<D> setDocumentIfAbsent( String documentName, String sourceCode, String tag, D document ) throws DocumentException
	{
		return sources.iterator().next().setDocumentIfAbsent( documentName, sourceCode, tag, document );
	}

	public Collection<DocumentDescriptor<D>> getDocuments()
	{
		LinkedList<DocumentDescriptor<D>> documents = new LinkedList<DocumentDescriptor<D>>();
		for( DocumentSource<D> source : sources )
			documents.addAll( source.getDocuments() );
		return documents;
	}

	public String getIdentifier()
	{
		return identifier;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final String identifier;

	private final CopyOnWriteArrayList<DocumentSource<D>> sources = new CopyOnWriteArrayList<DocumentSource<D>>();
}
