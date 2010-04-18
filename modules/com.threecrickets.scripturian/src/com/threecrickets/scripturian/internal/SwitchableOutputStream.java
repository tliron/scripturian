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

package com.threecrickets.scripturian.internal;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Tal Liron
 */
public class SwitchableOutputStream extends OutputStream
{
	//
	// Construction
	//

	public SwitchableOutputStream( OutputStream outputStream )
	{
		use( outputStream );
	}

	//
	// Operations
	//

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

	private volatile OutputStream outputStream;
}
