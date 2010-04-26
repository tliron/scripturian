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
import java.util.regex.Pattern;

import com.threecrickets.scripturian.Executable;

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

	/**
	 * Gets the value of a switch-style command line argument.
	 * 
	 * @param name
	 *        The argument name
	 * @param arguments
	 *        The arguments
	 * @param defaultValue
	 *        The default value
	 * @return The value or the default value if not found
	 */
	public static String getSwitchArgument( String name, String[] arguments, String defaultValue )
	{
		name = "--" + name + "=";
		for( String argument : arguments )
			if( argument.startsWith( name ) )
				return argument.substring( name.length() );

		return defaultValue;
	}

	/**
	 * Gets the value of a command line argument, while skipping switch-style
	 * arguments.
	 * 
	 * @param index
	 *        The index of the non-switch argument
	 * @param arguments
	 *        The arguments
	 * @param defaultValue
	 *        The default value
	 * @return The value or the default value if not found
	 */
	public static String getNonSwitchArgument( int index, String[] arguments, String defaultValue )
	{
		int i = 0;
		for( String argument : arguments )
			if( !argument.startsWith( "--" ) )
				if( index == i++ )
					return argument;

		return defaultValue;
	}

	/**
	 * Calculates a filename for a JVM class based on executable partition,
	 * executable document name and scriptlet position.
	 * 
	 * @param executable
	 *        The executable
	 * @param position
	 *        The scriptlet position
	 * @return The file
	 */
	public static String getFilenameForScriptletClass( Executable executable, int position )
	{
		String filename = executable.getDocumentName();
		int lastSlash = filename.lastIndexOf( '/' );
		if( lastSlash != -1 )
			filename = filename.substring( 0, lastSlash ).replace( ".", "_" ) + filename.substring( lastSlash ).replace( "-", "_" ).replace( ".", "$" );
		else
			filename = filename.replace( ".", "$" );
		filename += "$" + position + "$" + executable.getDocumentTimestamp() + ".class";
		return executable.getPartition() + filename;
	}

	public static String getClassnameForScriptlet( Executable executable, int position )
	{
		String classname = executable.getPartition() + executable.getDocumentName();
		classname = classname.replace( "-", "_" );
		classname = classname.replace( ".", "$" );
		classname = classname.replace( "/", "." );
		classname += "$" + position + "$" + executable.getDocumentTimestamp();
		return classname;
	}

	/**
	 * Returns the path of one file relative to another.
	 * 
	 * @param target
	 *        the target directory
	 * @param base
	 *        the base directory
	 * @return target's path relative to the base directory
	 * @throws IOException
	 *         if an error occurs while resolving the files' canonical names
	 */
	public static File getRelativeFile( File target, File base )
	{
		// See:
		// http://stackoverflow.com/questions/204784/how-to-construct-a-relative-path-in-java-from-two-absolute-paths-or-urls

		String[] baseComponents = base.getAbsolutePath().split( Pattern.quote( File.separator ) );
		String[] targetComponents = target.getAbsolutePath().split( Pattern.quote( File.separator ) );

		// skip common components
		int index = 0;
		for( ; index < targetComponents.length && index < baseComponents.length; ++index )
		{
			if( !targetComponents[index].equals( baseComponents[index] ) )
				break;
		}

		StringBuilder result = new StringBuilder();
		if( index != baseComponents.length )
		{
			// backtrack to base directory
			for( int i = index; i < baseComponents.length; ++i )
				result.append( ".." + File.separator );
		}
		for( ; index < targetComponents.length; ++index )
			result.append( targetComponents[index] + File.separator );
		if( !target.getPath().endsWith( "/" ) && !target.getPath().endsWith( "\\" ) )
		{
			// remove final path separator
			result.delete( result.length() - "/".length(), result.length() );
		}
		return new File( result.toString() );
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
