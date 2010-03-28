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
 * Transforms a document described by a {@link DocumentDescriptor} to a
 * different format. Useful for both human- and machine-readable formats.
 * 
 * @author Tal Liron
 * @param <D>
 */
public interface DocumentFormatter<D>
{
	public String format( DocumentDescriptor<D> documentDescriptor, String title, int highlightLineNumber );
}
