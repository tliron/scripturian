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
 * A frame within a call stack.
 * 
 * @author Tal Liron
 * @see ExecutionException
 * @see ParsingException
 */
public class StackFrame
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param documentName
	 * @param lineNumber
	 * @param columnNumber
	 */
	public StackFrame( String documentName, int lineNumber, int columnNumber )
	{
		this.documentName = documentName;
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
	}

	/**
	 * @param documentName
	 */
	public StackFrame( String documentName )
	{
		this( documentName, -1, -1 );
	}

	/**
	 * @param stackTraceElement
	 */
	public StackFrame( StackTraceElement stackTraceElement )
	{
		documentName = stackTraceElement.getFileName();
		lineNumber = stackTraceElement.getLineNumber();
		columnNumber = -1;
	}

	//
	// Attributes
	//

	/**
	 * The document name.
	 * 
	 * @return The document name
	 */
	public String getDocumentName()
	{
		return documentName;
	}

	/**
	 * The line number.
	 * 
	 * @return The line number
	 */
	public int getLineNumber()
	{
		return lineNumber;
	}

	/**
	 * The column number.
	 * 
	 * @return The column number
	 */
	public int getColumnNumber()
	{
		return columnNumber;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The document name.
	 */
	private final String documentName;

	/**
	 * The line number.
	 */
	private final int lineNumber;

	/**
	 * The column number.
	 */
	private final int columnNumber;
}
