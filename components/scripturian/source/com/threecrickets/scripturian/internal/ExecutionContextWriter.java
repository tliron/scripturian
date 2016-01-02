/**
 * Copyright 2009-2016 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.internal;

import java.io.IOException;
import java.io.Writer;

import com.threecrickets.scripturian.ExecutionContext;

/**
 * A wrapper for {@link Writer} that uses
 * {@link ExecutionContext#getWriterOrDefault()}.
 * 
 * @author Tal Liron
 */
public class ExecutionContextWriter extends Writer
{
	//
	// Writer
	//

	@Override
	public void write( char[] cbuf, int off, int len ) throws IOException
	{
		getWriter().write( cbuf, off, len );
	}

	@Override
	public void flush() throws IOException
	{
		getWriter().flush();
	}

	@Override
	public void close() throws IOException
	{
		getWriter().close();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	/**
	 * The underlying writer.
	 */
	protected Writer getWriter()
	{
		ExecutionContext executionContext = ExecutionContext.getCurrent();
		return executionContext.getWriterOrDefault();
	}
}
