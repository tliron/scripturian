package com.threecrickets.scripturian;

/**
 * Shared static attributes for Scripturian.
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
		// Delegate to MainDocument
		Main.main( arguments );
	}
}
