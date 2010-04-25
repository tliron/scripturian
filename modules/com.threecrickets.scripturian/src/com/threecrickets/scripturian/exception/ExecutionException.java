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

import java.util.ArrayList;
import java.util.List;

import com.threecrickets.scripturian.Executable;

/**
 * An execution exception. Can occur during the execution and invocation phases
 * of an executable.
 * 
 * @author Tal Liron
 * @see Executable
 */
public class ExecutionException extends Exception
{
	//
	// Construction
	//

	public ExecutionException( String documentName, int lineNumber, int columnNumber, String message )
	{
		super( message );
		stack.add( new StackFrame( documentName, lineNumber, columnNumber ) );
	}

	public ExecutionException( String documentName, int lineNumber, int columnNumber, String message, Throwable cause )
	{
		super( message != null ? message : cause.getClass().getName(), cause );
		stack.add( new StackFrame( documentName, lineNumber, columnNumber ) );
	}

	public ExecutionException( String documentName, int lineNumber, int columnNumber, Throwable cause )
	{
		super( cause );
		stack.add( new StackFrame( documentName, lineNumber, columnNumber ) );
	}

	public ExecutionException( String documentName, String message, Throwable cause )
	{
		super( message != null ? message : cause.getClass().getName(), cause );
		stack.add( new StackFrame( documentName ) );
	}

	public ExecutionException( String documentName, String message )
	{
		super( message );
		stack.add( new StackFrame( documentName ) );
	}

	public ExecutionException( String message, Throwable cause )
	{
		super( message != null ? message : cause.getClass().getName(), cause );
	}

	public ExecutionException( ParsingException parsingException )
	{
		this( parsingException.getCause() != null ? parsingException.getCause().getMessage() : parsingException.getMessage(), parsingException );
		stack.addAll( parsingException.getStack() );
	}

	//
	// Attributes
	//

	/**
	 * The call stack.
	 * 
	 * @return The call stack
	 */
	public List<StackFrame> getStack()
	{
		return stack;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;

	/**
	 * The call stack.
	 */
	private final ArrayList<StackFrame> stack = new ArrayList<StackFrame>();
}
