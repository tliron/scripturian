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

package com.threecrickets.scripturian.internal;

import com.threecrickets.scripturian.Main;

/**
 * This is the <code>application</code> exposed by {@link Main}.
 * 
 * @author Tal Liron
 * @see Main
 */
public class ExposedApplication
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param main
	 *        The main instance
	 */
	public ExposedApplication( Main main )
	{
		this.main = main;
	}

	//
	// Attributes
	//

	//
	// Attributes
	//

	/**
	 * An array of the string arguments sent to {@link Main#main(String[])}.
	 * 
	 * @return The arguments
	 */
	public String[] getArguments()
	{
		return main.getArguments();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The main instance.
	 */
	private final Main main;
}
