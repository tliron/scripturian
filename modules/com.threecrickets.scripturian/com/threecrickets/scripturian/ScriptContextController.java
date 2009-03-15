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
	 * @param scriptContext
	 * @throws ScriptException
	 */
	public void initialize( ScriptContext scriptContext ) throws ScriptException;

	/**
	 * @param scriptContext
	 * @throws ScriptException
	 */
	public void finalize( ScriptContext scriptContext );
}
