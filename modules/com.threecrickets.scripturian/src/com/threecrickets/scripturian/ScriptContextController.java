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

import javax.script.ScriptContext;
import javax.script.ScriptException;

/**
 * Used in order to add specialized initialization and finalization for
 * scripting engines. For example, to add extra global variables or to perform
 * cleanup of resources provided by the script environment or created by the
 * script.
 * 
 * @author Tal Liron
 */
public interface ScriptContextController
{
	/**
	 * Called when the script is initialized.
	 * 
	 * @param scriptContext
	 *        The script context
	 * @throws ScriptException
	 *         If you throw an exception here, the script will not run
	 */
	public void initialize( ScriptContext scriptContext ) throws ScriptException;

	/**
	 * Called when the script finalizes. This is a good place to clean up
	 * resources you set up during {@link #initialize(ScriptContext)}.
	 * 
	 * @param scriptContext
	 *        The script context
	 */
	public void finalize( ScriptContext scriptContext );
}
