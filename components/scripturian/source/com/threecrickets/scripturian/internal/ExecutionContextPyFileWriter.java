/**
 * Copyright 2009-2011 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.internal;

import org.python.core.PyFileWriter;
import org.python.core.PyObject;

import com.threecrickets.scripturian.ExecutionContext;

/**
 * A Jython file writer wrapper that uses an underlying writer stored in the
 * current {@link ExecutionContext}. This is useful for having the same
 * sys.stdout instance write to different underlying writers.
 * 
 * @author Tal Liron
 * @see ExecutionContextPyFileErrorWriter
 */
public class ExecutionContextPyFileWriter extends PyFileWriter
{
	//
	// Constants
	//

	public static final String WRITER = "com.threecrickets.scripturian.internal.ExecutionContextPyFileWriter.writer";

	//
	// Construction
	//

	public ExecutionContextPyFileWriter()
	{
		super( null );
	}

	//
	// PyFileWriter
	//

	@Override
	public boolean closed()
	{
		return getWriter().closed();
	}

	@Override
	public void checkClosed()
	{
		getWriter().checkClosed();
	}

	@Override
	public void flush()
	{
		getWriter().flush();
	}

	@Override
	public void write( PyObject o )
	{
		getWriter().write( o );
	}

	@Override
	public void write( String s )
	{
		getWriter().write( s );
	}

	@Override
	public void writelines( PyObject a )
	{
		getWriter().writelines( a );
	}
	
	public boolean isatty()
	{
		return false;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	/**
	 * The underlying writer for the current execution context. Creates it if it
	 * doesn't exist yet.
	 * 
	 * @return The underlying writer
	 */
	protected PyFileWriter getWriter()
	{
		ExecutionContext executionContext = ExecutionContext.getCurrent();
		PyFileWriter writer = (PyFileWriter) executionContext.getAttributes().get( WRITER );
		if( writer == null )
		{
			writer = new PyFileWriter( executionContext.getWriterOrDefault() );
			executionContext.getAttributes().put( WRITER, writer );
		}
		return writer;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;
}
