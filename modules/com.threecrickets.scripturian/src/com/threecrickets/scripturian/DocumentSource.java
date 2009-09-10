/**
 * Copyright 2009 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://www.threecrickets.com/
 */

package com.threecrickets.scripturian;

import java.io.IOException;

/**
 * Manages retrieval of script text and caching of arbitrary script
 * implementations via descriptors.
 * <p>
 * Implementations are expected to be thread safe! This includes returned
 * descriptors.
 * 
 * @author Tal Liron
 * @param <D>
 *        The script type
 */
public interface DocumentSource<D>
{
	/**
	 * Access to the script descriptor.
	 * 
	 * @param <S>
	 *        The script type
	 */
	public interface DocumentDescriptor<S>
	{
		/**
		 * The text for the script.
		 * 
		 * @return The text
		 */
		public String getText();

		/**
		 * The tag for the script (for a file-based script source, this will
		 * probably be the file name extension).
		 * 
		 * @return The tag
		 */
		public String getTag();

		/**
		 * The script instance. Should be null by default, as it is intended to
		 * be set by the user of the {@link DocumentSource},
		 * 
		 * @return The script instance
		 */
		public S getDocument();

		/**
		 * @param value
		 *        The script instance
		 * @return The existing script instance before we changed it
		 * @see #getDocument()
		 */
		public S setDocument( S value );

		/**
		 * Like {@link #setDocument(Object)}, with an atomic check for null.
		 * 
		 * @param value
		 *        The script instance
		 * @return The existing script instance before we changed it
		 * @see #getDocument()
		 */
		public S setDocumentIfAbsent( S value );
	}

	/**
	 * Gets a script descriptor by its name.
	 * 
	 * @param name
	 *        The script's name
	 * @return The script descriptor
	 * @throws IOException
	 */
	public DocumentDescriptor<D> getDocumentDescriptor( String name ) throws IOException;

	/**
	 * Allows adding or changing script descriptors.
	 * 
	 * @param name
	 *        The script's name
	 * @param text
	 *        The text for the script
	 * @param tag
	 *        The tag
	 * @param script
	 *        The script instance
	 * @return The existing script descriptor before we changed it
	 */
	public DocumentDescriptor<D> setDocumentDescriptor( String name, String text, String tag, D script );

	/**
	 * Allows adding or changing script descriptors, with an atomic check for
	 * null.
	 * 
	 * @param name
	 *        The script's name
	 * @param text
	 *        The text for the script
	 * @param tag
	 *        The tag
	 * @param script
	 *        The script instance
	 * @return The existing script descriptor before we changed it
	 */
	public DocumentDescriptor<D> setDocumentDescriptorIfAbsent( String name, String text, String tag, D script );
}
