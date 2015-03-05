/**
 * Copyright 2009-2015 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian;

/**
 * Delegates to {@link Main}.
 * 
 * @author Tal Liron
 */
public abstract class Scripturian
{
	//
	// Main
	//

	public static void main( String[] arguments )
	{
		// Delegate to Main
		Main.main( arguments );
	}
}
