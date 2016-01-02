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

package com.threecrickets.scripturian.document;

/**
 * Transforms a document described by a {@link DocumentDescriptor} to a
 * different format. Useful for both human- and machine-readable formats.
 * 
 * @author Tal Liron
 * @param <D>
 *        The document type
 */
public interface DocumentFormatter<D>
{
	/**
	 * Formats the document or its source code.
	 * 
	 * @param documentDescriptor
	 *        The document descriptor
	 * @param title
	 *        The result title
	 * @param highlightLineNumber
	 *        The line number to highlight
	 * @return The formatted document
	 */
	public String format( DocumentDescriptor<D> documentDescriptor, String title, int highlightLineNumber );
}
