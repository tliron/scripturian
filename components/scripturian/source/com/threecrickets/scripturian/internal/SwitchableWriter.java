/**
 * Copyright 2009-2014 Three Crickets LLC.
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

/**
 * A wrapper for {@link Writer} that allows runtime switching of the underlying
 * writer. It is safe to switch by concurrent threads, though there's no way to
 * determine which thread will switch first unless you specifically coordinate
 * access.
 * 
 * @author Tal Liron
 */
public class SwitchableWriter extends Writer
{
	//
	// Constructor
	//

	/**
	 * Constructor.
	 * 
	 * @param writer
	 *        The initial underlying writer
	 */
	public SwitchableWriter( Writer writer )
	{
		super();
		use( writer );
	}

	//
	// Operations
	//

	/**
	 * Switch the underlying writer.
	 * 
	 * @param writer
	 *        The writer
	 */
	public void use( Writer writer )
	{
		this.writer = writer;
	}

	//
	// Writer
	//

	@Override
	public void write( char[] cbuf, int off, int len ) throws IOException
	{
		writer.write( cbuf, off, len );
	}

	@Override
	public void flush() throws IOException
	{
		writer.flush();
	}

	@Override
	public void close() throws IOException
	{
		writer.close();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The underlying writer.
	 */
	private volatile Writer writer;
}
