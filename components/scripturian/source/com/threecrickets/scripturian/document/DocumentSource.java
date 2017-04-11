/**
 * Copyright 2009-2017 Three Crickets LLC.
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

import com.threecrickets.scripturian.exception.DocumentException;

/**
 * Manages retrieval of text-based documents and caching of arbitrary document
 * implementations via descriptors.
 * <p>
 * Implementations are expected to be safe for concurrent access. This includes
 * returned descriptors.
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
	 * @param documentName
	 *        The document's name
	 * @return The document's descriptor
	 * @throws DocumentException
	 *         In case of a document retrieval error
	 */
	public DocumentDescriptor<D> getDocument( String documentName ) throws DocumentException;

	/**
	 * Allows adding or changing documents.
	 * 
	 * @param documentName
	 *        The document's name
	 * @param sourceCode
	 *        The source code for the document
	 * @param tag
	 *        The tag
	 * @param document
	 *        The document
	 * @return The existing document descriptor before we changed it
	 * @throws DocumentException
	 *         In case of a document retrieval error
	 */
	public DocumentDescriptor<D> setDocument( String documentName, String sourceCode, String tag, D document ) throws DocumentException;

	/**
	 * Allows for atomically adding or changing documents.
	 * 
	 * @param documentName
	 *        The document's name
	 * @param sourceCode
	 *        The source code for the document
	 * @param tag
	 *        The tag
	 * @param document
	 *        The document instance
	 * @return The existing document descriptor before we changed it
	 * @throws DocumentException
	 *         In case of a document retrieval error
	 */
	public DocumentDescriptor<D> setDocumentIfAbsent( String documentName, String sourceCode, String tag, D document ) throws DocumentException;

	/**
	 * Access to all available documents.
	 * <p>
	 * Note that not all implementations support this operation.
	 * 
	 * @return A collection of document descriptors
	 * @throws UnsupportedOperationException
	 *         In case this document source doesn't support this operation
	 */
	public Collection<DocumentDescriptor<D>> getDocuments();

	/**
	 * The identifier for this document source.
	 * 
	 * @return The identifier
	 */
	public String getIdentifier();
}
