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

/**
 * Access to the document descriptor.
 * 
 * @param <D>
 *        The document type
 */
public interface DocumentDescriptor<D>
{
	/**
	 * Documents may have many names, but this one is preferred to others.
	 * 
	 * @return The preferred name
	 */
	public String getDefaultName();

	/**
	 * The source text for the document.
	 * 
	 * @return The source text
	 */
	public String getText();

	/**
	 * The tag for the document (for a file-based document source, this will
	 * probably be the file name extension).
	 * 
	 * @return The tag
	 */
	public String getTag();

	/**
	 * The document instance. Should be null by default, as it is intended to be
	 * set by the user of the {@link DocumentSource},
	 * 
	 * @return The document instance
	 */
	public D getDocument();

	/**
	 * @param value
	 *        The document instance
	 * @return The existing document instance before we changed it
	 * @see #getDocument()
	 */
	public D setDocument( D value );

	/**
	 * Like {@link #setDocument(Object)}, with an atomic check for null.
	 * 
	 * @param value
	 *        The document instance
	 * @return The existing document instance before we changed it
	 * @see #getDocument()
	 */
	public D setDocumentIfAbsent( D value );
}