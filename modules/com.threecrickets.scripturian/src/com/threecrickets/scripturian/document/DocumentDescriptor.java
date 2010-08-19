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

import java.util.Set;

/**
 * Document descriptors are provided by a {@link DocumentSource} as a way to
 * describe and access documents.
 * 
 * @author Tal Liron
 * @param <D>
 *        The document type
 */
public interface DocumentDescriptor<D>
{
	/**
	 * Documents may have many names, but this one is preferred to others.
	 * 
	 * @return The default name
	 */
	public String getDefaultName();

	/**
	 * The source code for the document.
	 * 
	 * @return The source code
	 */
	public String getSourceCode();

	/**
	 * The tag for the document (for a file-based document source, this will
	 * probably be the file name extension).
	 * 
	 * @return The tag
	 */
	public String getTag();

	/**
	 * The document timestamp.
	 * 
	 * @return The document timestamp
	 */
	public long getTimestamp();

	/**
	 * The document instance. Should be null by default, as it is intended to be
	 * set by the user of the {@link DocumentSource}.
	 * 
	 * @return The document instance
	 */
	public D getDocument();

	/**
	 * @param document
	 *        The document instance
	 * @return The existing document instance before we changed it
	 * @see #getDocument()
	 */
	public D setDocument( D document );

	/**
	 * Like {@link #setDocument(Object)}, with an atomic check for null.
	 * 
	 * @param document
	 *        The document instance
	 * @return The existing document instance before we changed it
	 * @see #getDocument()
	 */
	public D setDocumentIfAbsent( D document );

	/**
	 * The document source from whence this document came.
	 * 
	 * @return The document source
	 */
	public DocumentSource<D> getSource();

	/**
	 * This document might be affected in some way (for example, reloaded,
	 * recompiled, etc.) if documents it depends on are affected. The exact
	 * effect depends on the implementation of the document, the document
	 * source, or other mechanisms.
	 * 
	 * @return The names of dependent documents
	 */
	public Set<String> getDependencies();
}