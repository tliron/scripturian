/**
 * Copyright 2009 Three Crickets.
 * <p>
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * <p>
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * <p>
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * <p>
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * <p>
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
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
 * 
 * @author Tal Liron
 * @param <S>
 *        The script type
 */
public interface ScriptSource<S>
{
	/**
	 * Access to the script descriptor.
	 * 
	 * @param <S>
	 *        The script type
	 */
	public interface ScriptDescriptor<S>
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
		 * be set by the user of the {@link ScriptSource},
		 * 
		 * @return The script instance
		 */
		public S getScript();

		/**
		 * @param value
		 *        The script instance
		 * @return The existing script instance before we changed it
		 * @see #getScript()
		 */
		public S setScript( S value );
	}

	/**
	 * Gets a script descriptor by its name.
	 * 
	 * @param name
	 *        The script's name
	 * @return The script descriptor
	 * @throws IOException
	 */
	public ScriptDescriptor<S> getScriptDescriptor( String name ) throws IOException;

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
	public ScriptDescriptor<S> setScriptDescriptor( String name, String text, String tag, S script );
}
