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

package com.threecrickets.scripturian;

import java.io.IOException;
import java.util.Collection;

/**
 * Manages retrieval of document text and caching of arbitrary document
 * implementations via descriptor.
 * <p>
 * Implementations are expected to be thread safe! This includes returned
 * descriptor.
 * 
 * @author Tal Liron
 * @param <D>
 *        The document type
 */
public interface DocumentSource<D>
{
	/**
	 * Gets a document descriptor by its name.
	 * 
	 * @param name
	 *        The document's name
	 * @return The document descriptor
	 * @throws IOException
	 */
	public DocumentDescriptor<D> getDocumentDescriptor( String name ) throws IOException;

	/**
	 * Allows adding or changing document descriptor.
	 * 
	 * @param name
	 *        The document's name
	 * @param text
	 *        The text for the document
	 * @param tag
	 *        The tag
	 * @param document
	 *        The document instance
	 * @return The existing document descriptor before we changed it
	 */
	public DocumentDescriptor<D> setDocumentDescriptor( String name, String text, String tag, D document );

	/**
	 * Allows adding or changing document descriptor, with an atomic check for
	 * null.
	 * 
	 * @param name
	 *        The document's name
	 * @param text
	 *        The text for the document
	 * @param tag
	 *        The tag
	 * @param document
	 *        The document instance
	 * @return The existing document descriptor before we changed it
	 */
	public DocumentDescriptor<D> setDocumentDescriptorIfAbsent( String name, String text, String tag, D document );

	/**
	 * Access to all available document descriptors.
	 * <p>
	 * Note that not all implementations support this operation.
	 * 
	 * @return An collection of document descriptors
	 * @throws UnsupportedOperationException
	 */
	public Collection<DocumentDescriptor<D>> getDocumentDescriptors();
}
