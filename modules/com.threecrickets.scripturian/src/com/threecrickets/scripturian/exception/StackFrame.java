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

package com.threecrickets.scripturian.exception;

/**
 * @author Tal Liron
 */
public class StackFrame
{
	//
	// Construction
	//

	public StackFrame( String name, int lineNumber, int columnNumber )
	{
		this.name = name;
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
	}

	public StackFrame( String name )
	{
		this( name, -1, -1 );
	}

	//
	// Attributes
	//

	public String getName()
	{
		return name;
	}

	public int getLineNumber()
	{
		return lineNumber;
	}

	public int getColumnNumber()
	{
		return columnNumber;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final String name;

	private final int lineNumber;

	private final int columnNumber;
}
