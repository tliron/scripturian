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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

/**
 * Utility methods.
 * 
 * @author Tal Liron
 */
public abstract class ScripturianUtil
{
	/**
	 * Size (in bytes) of the buffer used by {@link #getString(File)}.
	 */
	public static final int BUFFER_SIZE = 1024 * 1024;

	/**
	 * Gets the filename extension (whatever is after the period), or null if it
	 * doesn't have one.
	 * 
	 * @param file
	 *        The file
	 * @return The filename extension or null
	 */
	public static String getExtension( File file )
	{
		String filename = file.getName();
		int period = filename.lastIndexOf( '.' );
		if( period != -1 )
			return filename.substring( period + 1 );
		else
			return null;
	}

	/**
	 * Reads a reader into a string.
	 * 
	 * @param reader
	 *        The reader
	 * @return The string read from the reader
	 * @throws IOException
	 */
	public static String getString( Reader reader ) throws IOException
	{
		StringWriter writer = new StringWriter();
		int c;
		while( true )
		{
			c = reader.read();
			if( c == -1 )
				break;

			writer.write( c );
		}

		return writer.toString();
	}

	/**
	 * Reads a file into a string. Uses a buffer (see {@link #BUFFER_SIZE}).
	 * 
	 * @param file
	 *        The file
	 * @return The string read from the file
	 * @throws IOException
	 */
	public static String getString( File file ) throws IOException
	{
		BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( file ) ), BUFFER_SIZE );
		return getString( reader );
	}

	/**
	 * Reads a file into a bute array.
	 * 
	 * @param file
	 *        The file
	 * @return The byte array
	 * @throws IOException
	 */
	public static byte[] getBytes( File file ) throws IOException
	{
		InputStream is = new FileInputStream( file );

		// Get the size of the file
		long length = file.length();

		if( length > Integer.MAX_VALUE )
		{
			// File is too large
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while( offset < bytes.length && ( numRead = is.read( bytes, offset, bytes.length - offset ) ) >= 0 )
		{
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if( offset < bytes.length )
		{
			throw new IOException( "Could not completely read file " + file.getName() );
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * Disallow inheritance.
	 */
	private ScripturianUtil()
	{
	}
}
