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

package com.threecrickets.scripturian.document;

import java.io.IOException;
import java.util.Collection;

/**
 * Manages retrieval of text-based documents and caching of arbitrary document
 * implementations via descriptors.
 * <p>
 * Implementations are expected to be thread safe! This includes returned
 * descriptors.
 * 
 * @author Tal Liron
 * @param <D>
 *        The document type
 * @see DocumentDescriptor
 */
public interface DocumentSource<D>
{
	/**
	 * Gets a document by its name.
	 * 
	 * @param name
	 *        The document's name
	 * @return The document's descriptor
	 * @throws IOException
	 */
	public DocumentDescriptor<D> getDocument( String name ) throws IOException;

	/**
	 * Allows adding or changing documents.
	 * 
	 * @param name
	 *        The document's name
	 * @param sourceCode
	 *        The source code for the document
	 * @param tag
	 *        The tag
	 * @param document
	 *        The document
	 * @return The existing document descriptor before we changed it
	 */
	public DocumentDescriptor<D> setDocument( String name, String sourceCode, String tag, D document );

	/**
	 * Allows adding or changing documents, with an atomic check for null.
	 * 
	 * @param name
	 *        The document's name
	 * @param sourceCode
	 *        The source code for the document
	 * @param tag
	 *        The tag
	 * @param document
	 *        The document instance
	 * @return The existing document descriptor before we changed it
	 */
	public DocumentDescriptor<D> setDocumentIfAbsent( String name, String sourceCode, String tag, D document );

	/**
	 * Access to all available documents.
	 * <p>
	 * Note that not all implementations support this operation.
	 * 
	 * @return An collection of document descriptors
	 * @throws UnsupportedOperationException
	 */
	public Collection<DocumentDescriptor<D>> getDocuments();
}
