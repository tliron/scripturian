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
import java.io.OutputStream;
import java.io.Writer;

/**
 * Wraps a writer in an output stream.
 * 
 * @author Tal Liron
 */
public class WriterOutputStream extends OutputStream
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @param writer
	 *        The underlying writer
	 */
	public WriterOutputStream( Writer writer )
	{
		this( writer, null );
	}

	/**
	 * Constructor.
	 * 
	 * @param writer
	 *        The underlying writer
	 * @param encoding
	 *        The encoding
	 */
	public WriterOutputStream( Writer writer, String encoding )
	{
		this.writer = writer;
		this.encoding = encoding;
	}

	//
	// Writer
	//

	@Override
	public void close() throws IOException
	{
		writer.close();
	}

	@Override
	public void flush() throws IOException
	{
		writer.flush();
	}

	@Override
	public void write( byte[] b ) throws IOException
	{
		if( encoding == null )
			writer.write( new String( b ) );
		else
			writer.write( new String( b, encoding ) );
	}

	@Override
	public void write( byte[] b, int off, int len ) throws IOException
	{
		if( encoding == null )
			writer.write( new String( b, off, len ) );
		else
			writer.write( new String( b, off, len, encoding ) );
	}

	@Override
	public synchronized void write( int b ) throws IOException
	{
		buffer[0] = (byte) b;
		write( buffer );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The underlying writer.
	 */
	private final Writer writer;

	/**
	 * The optional encoding.
	 */
	private final String encoding;

	/**
	 * Work buffer.
	 */
	private final byte[] buffer = new byte[1];
}
