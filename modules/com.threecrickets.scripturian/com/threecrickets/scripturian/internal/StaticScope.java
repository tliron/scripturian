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

package com.threecrickets.scripturian.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Used as global space for sharing objects between scripts. Statics will stay
 * alive for the duration of the Java virtual machine. Any object can be stored
 * here.
 * 
 * @author Tal Liron
 */
public class StaticScope
{
	//
	// Static attributes
	//

	public static StaticScope getInstance()
	{
		return instance;
	}

	/**
	 * Any object can be stored here.
	 */
	public ConcurrentMap<String, Object> getValues()
	{
		return values;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final static StaticScope instance = new StaticScope();

	private final ConcurrentMap<String, Object> values = new ConcurrentHashMap<String, Object>();

	private StaticScope()
	{
	}
}
