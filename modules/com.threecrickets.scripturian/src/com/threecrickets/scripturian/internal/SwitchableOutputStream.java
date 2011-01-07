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

import java.io.IOException;
import java.io.OutputStream;

/**
 * A wrapper for {@link OutputStream} that allows runtime switching of the
 * underlying stream. It is safe to switch by concurrent threads, though there's
 * no way to determine which thread will switch first unless you specifically
 * coordinate access.
 * 
 * @author Tal Liron
 */
public class SwitchableOutputStream extends OutputStream
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param outputStream
	 *        The initial underlying output stream
	 */
	public SwitchableOutputStream( OutputStream outputStream )
	{
		use( outputStream );
	}

	//
	// Operations
	//

	/**
	 * Switch the underlying output stream.
	 * 
	 * @param outputStream
	 *        The output stream
	 */
	public void use( OutputStream outputStream )
	{
		this.outputStream = outputStream;
	}

	//
	// OutputStream
	//

	@Override
	public void write( int b ) throws IOException
	{
		outputStream.write( b );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The underlying output stream.
	 */
	private volatile OutputStream outputStream;
}
