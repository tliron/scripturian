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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageManager;

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
		StringWriter writer = new StringWriter( BUFFER_SIZE );
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
	 * Reads a file into a string. Buffer size is {@link #BUFFER_SIZE}.
	 * 
	 * @param file
	 *        The file
	 * @return The string read from the file
	 * @throws IOException
	 */
	public static String getString( File file ) throws IOException
	{
		FileReader fileReader = new FileReader( file );
		try
		{
			BufferedReader reader = new BufferedReader( fileReader, BUFFER_SIZE );
			try
			{
				return getString( reader );
			}
			finally
			{
				reader.close();
			}
		}
		finally
		{
			fileReader.close();
		}
	}

	/**
	 * Reads a file into a byte array.
	 * 
	 * @param file
	 *        The file
	 * @return The byte array
	 * @throws IOException
	 */
	public static byte[] getBytes( File file ) throws IOException
	{
		FileInputStream stream = new FileInputStream( file );

		try
		{
			long length = file.length();

			if( length > Integer.MAX_VALUE )
				throw new IOException( "File too big: " + file.getName() );

			int ilength = (int) length;
			byte[] bytes = new byte[ilength];
			int alength = stream.read( bytes );

			if( alength != ilength )
				throw new IOException( "Only " + alength + " of " + ilength + " bytes read: " + file.getName() );

			return bytes;
		}
		finally
		{
			stream.close();
		}
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
	 * Calculates a file for a JVM class based on executable partition,
	 * executable document name and program position. <i>You must synchronize
	 * access to this file</i> via the <code>synchronize</code> keyword, in
	 * order to guarantee that another thread will not be writing to the file at
	 * the same time. For this to work, File instances are guaranteed to be
	 * unique in this VM per combination of executable partition, document name
	 * and program position are the same.
	 * 
	 * @param subdirectory
	 *        The cache subdirectory
	 * @param executable
	 *        The executable
	 * @param position
	 *        The program's position in the executable
	 * @return The file
	 */
	public static File getFileForProgramClass( File subdirectory, Executable executable, int position )
	{
		String partition = executable.getPartition();
		if( File.separatorChar != '/' )
			partition = partition.replace( '/', File.separatorChar );

		String filename = partition + executable.getDocumentName();
		filename = filename.replace( '-', '_' ).replace( '.', '$' );
		filename += "$" + position + "$" + executable.getDocumentTimestamp() + ".class";
		File file = new File( subdirectory, filename );
		File existing = programClassFiles.get( file.getPath() );
		if( existing != null )
			file = existing;
		else
		{
			existing = programClassFiles.putIfAbsent( file.getPath(), file );
			if( existing != null )
				file = existing;
		}
		return file;
	}

	/**
	 * Calculates a JVM classname for a program based on executable partition,
	 * executable document name and program position.
	 * 
	 * @param executable
	 *        The executable
	 * @param position
	 *        The program's position in the executable
	 * @return The classname
	 */
	public static String getClassnameForProgram( Executable executable, int position )
	{
		String classname = executable.getPartition() + executable.getDocumentName();
		classname = classname.replace( '-', '_' ).replace( '.', '$' );
		if( File.separatorChar != '/' )
			classname = classname.replace( File.separator + File.separator, "." ).replace( File.separatorChar, '.' );
		classname = classname.replace( "//", "." ).replace( '/', '.' ).replace( "..", "." );
		classname += "$" + position + "$" + executable.getDocumentTimestamp();
		return classname;
	}

	/**
	 * Returns the path of one file relative to another.
	 * 
	 * @param target
	 *        The target file
	 * @param base
	 *        The base file
	 * @return The target's file relative to the base file
	 * @throws IOException
	 */
	public static File getRelativeFile( File target, File base )
	{
		return new File( getRelativePath( target.getAbsolutePath(), base.getAbsolutePath() ) );
	}

	/**
	 * Returns the path of one file relative to another.
	 * 
	 * @param target
	 *        The target path
	 * @param base
	 *        The base path
	 * @return The target's path relative to the base path
	 */
	public static String getRelativePath( String target, String base )
	{
		// See:
		// http://stackoverflow.com/questions/204784/how-to-construct-a-relative-path-in-java-from-two-absolute-paths-or-urls

		String split = Pattern.quote( File.separator );
		String[] baseSegments = base.split( split );
		String[] targetSegments = target.split( split );
		StringBuilder result = new StringBuilder();

		// Skip common segments
		int index = 0;
		for( ; index < targetSegments.length && index < baseSegments.length; ++index )
		{
			if( !targetSegments[index].equals( baseSegments[index] ) )
				break;
		}

		// Backtrack to base directory
		if( index != baseSegments.length )
		{
			for( int i = index; i < baseSegments.length; ++i )
			{
				// "." segments have no effect
				if( !baseSegments[i].equals( "." ) )
					result.append( ".." + File.separator );
			}
		}

		for( ; index < targetSegments.length; ++index )
			result.append( targetSegments[index] + File.separator );

		// Remove final path separator
		if( !target.endsWith( File.separator ) )
			result.delete( result.length() - 1, result.length() );

		return result.toString();
	}

	/**
	 * Calls include on the executable container.
	 * <p>
	 * Internally uses reflection with caching of looked-up methods.
	 * 
	 * @param manager
	 *        The language manager
	 * @param executable
	 *        The executable
	 * @param executionContext
	 *        The execution context
	 * @param documentName
	 *        The document name
	 */
	public static void containerInclude( LanguageManager manager, Executable executable, ExecutionContext executionContext, String documentName )
	{
		Object container = executable.getContainerService( executionContext );
		if( container != null )
		{
			Class<?> containerClass = container.getClass();
			Method includeMethod = includeMethods.get( containerClass );
			if( includeMethod == null )
			{
				String containerIncludeExpressionCommand = (String) manager.getAttributes().get( LanguageManager.CONTAINER_INCLUDE_EXPRESSION_COMMAND );
				try
				{
					includeMethod = containerClass.getMethod( containerIncludeExpressionCommand, new Class[]
					{
						String.class
					} );
				}
				catch( SecurityException x )
				{
				}
				catch( NoSuchMethodException x )
				{
				}
			}

			try
			{
				includeMethod.invoke( container, documentName );
			}
			catch( IllegalArgumentException x )
			{
			}
			catch( IllegalAccessException x )
			{
			}
			catch( InvocationTargetException x )
			{
			}
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * Used by {@link #getFileForProgramClass(File, Executable, int)} to
	 * guarantee uniqueness.
	 */
	private static final ConcurrentMap<String, File> programClassFiles = new ConcurrentHashMap<String, File>();

	/**
	 * Cache of container include methods.
	 * 
	 * @see #containerInclude(LanguageManager, Executable, ExecutionContext,
	 *      String)
	 */
	private static final ConcurrentMap<Class<?>, Method> includeMethods = new ConcurrentHashMap<Class<?>, Method>();

	/**
	 * Disallow inheritance.
	 */
	private ScripturianUtil()
	{
	}
}
