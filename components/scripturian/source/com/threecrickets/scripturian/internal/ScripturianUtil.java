/**
 * Copyright 2009-2015 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
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
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
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
	 * True if we are in a Windows operating system.
	 */
	public static boolean IS_WINDOWS = System.getProperty( "os.name" ).startsWith( "Windows" );

	/**
	 * The line separator.
	 */
	public static String LINE_SEPARATOR = System.getProperty( "line.separator" );

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
	 * Reads a file into a string.
	 * 
	 * @param file
	 *        The file
	 * @param charset
	 *        The charset (null to use default JVM charset; not recommended!)
	 * @return The string read from the file
	 * @throws IOException
	 */
	public static String getString( File file, Charset charset ) throws IOException
	{
		if( IS_WINDOWS )
		{
			// Note: There is no way to force the release of a MappedByteBuffer.
			// Unfortunately, under Windows this causes the file to remain
			// locked against writing. Since this is very annoying during
			// development, we will avoid using NIO for Windows. :(
			//
			// See: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038

			FileInputStream stream = new FileInputStream( file );
			try
			{
				if( charset == null )
					charset = Charset.defaultCharset();
				BufferedReader reader = new BufferedReader( new InputStreamReader( stream, charset ) );
				try
				{
					long length = file.length();
					if( length > Integer.MAX_VALUE )
						throw new IOException( "File too big: " + file.getName() );
					StringWriter contents = new StringWriter( (int) length );
					String line;
					while( ( line = reader.readLine() ) != null )
					{
						contents.append( line );
						contents.append( LINE_SEPARATOR );
					}
					return contents.toString();
				}
				finally
				{
					reader.close();
				}
			}
			finally
			{
				stream.close();
			}
		}
		else
		{
			FileInputStream stream = new FileInputStream( file );
			try
			{
				FileChannel channel = stream.getChannel();
				try
				{
					MappedByteBuffer buffer = channel.map( FileChannel.MapMode.READ_ONLY, 0, channel.size() );
					if( charset == null )
						charset = Charset.defaultCharset();
					return charset.decode( buffer ).toString();
				}
				finally
				{
					channel.close();
				}
			}
			finally
			{
				stream.close();
			}
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

			byte[] bytes = new byte[(int) length];
			ByteBuffer buffer = ByteBuffer.wrap( bytes );

			FileChannel channel = stream.getChannel();
			try
			{
				channel.read( buffer );
				return bytes;
			}
			finally
			{
				channel.close();
			}
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
	 * Calculates a JVM class file for aprogram based on executable partition,
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
		return getFileForProgram( subdirectory, executable, position, CLASS_SUFFIX );
	}

	/**
	 * Calculates a file for a program based on executable partition, executable
	 * document name and program position. <i>You must synchronize access to
	 * this file</i> via the <code>synchronize</code> keyword, in order to
	 * guarantee that another thread will not be writing to the file at the same
	 * time. For this to work, File instances are guaranteed to be unique in
	 * this VM per combination of executable partition, document name and
	 * program position are the same.
	 * 
	 * @param subdirectory
	 *        The cache subdirectory
	 * @param executable
	 *        The executable
	 * @param position
	 *        The program's position in the executable
	 * @param suffix
	 *        The file's suffix
	 * @return The file
	 */
	public static File getFileForProgram( File subdirectory, Executable executable, int position, String suffix )
	{
		String partition = executable.getPartition();

		if( File.separatorChar != '/' )
			partition = partition.replace( '/', File.separatorChar );

		String filename = partition + executable.getDocumentName();
		filename = filename.replace( '-', '_' ).replace( '.', '$' ).replace( ':', '$' ).replace( ' ', '$' );
		filename += "$" + position + "$" + executable.getDocumentTimestamp() + suffix;

		File file = new File( subdirectory, filename );
		File existing = programFiles.get( file.getPath() );
		if( existing != null )
			file = existing;
		else
		{
			existing = programFiles.putIfAbsent( file.getPath(), file );
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

		if( classname.startsWith( File.separator ) )
			classname = classname.substring( 1 );

		classname = classname.replace( '-', '_' ).replace( '.', '$' ).replace( ':', '$' ).replace( ' ', '$' ).replace( "//", "." ).replace( '/', '.' ).replace( "..", "." );
		if( File.separatorChar != '/' )
			classname = classname.replace( File.separator + File.separator, "." ).replace( File.separatorChar, '.' );
		classname += "$" + position + "$" + executable.getDocumentTimestamp();
		return classname;
	}

	/**
	 * Returns the file with its path normalized (all relative ".." and "."
	 * segments resolved).
	 * 
	 * @param file
	 *        The file
	 * @return The normalized file
	 */
	public static File getNormalizedFile( File file )
	{
		return new File( file.toURI().normalize() );
	}

	/**
	 * Returns the path of one file relative to another.
	 * 
	 * @param target
	 *        The target file
	 * @param base
	 *        The base file
	 * @return The target's file relative to the base file
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
		int length = baseSegments.length;
		if( index != length )
		{
			for( int i = index; i < length; ++i )
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
				String containerIncludeCommand = (String) manager.getAttributes().get( LanguageManager.CONTAINER_INCLUDE_COMMAND_ATTRIBUTE );
				try
				{
					includeMethod = containerClass.getMethod( containerIncludeCommand, new Class[]
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

	/**
	 * Wraps literal strings in double quotes, escaping special characters with
	 * backslashes.
	 * 
	 * @param string
	 *        The string
	 * @return The resulting string
	 */
	public static String doubleQuotedLiteral( String string )
	{
		return "\"" + replace( string, PRINT_ESCAPE_PATTERNS, PRINT_ESCAPE_REPLACEMENTS ) + "\"";
	}

	/**
	 * Replaces all occurrences of several regular expressions in order.
	 * 
	 * @param string
	 *        The string
	 * @param patterns
	 *        An array of regular expressions
	 * @param replacements
	 *        An array of replacement strings (must be the same length and order
	 *        as patterns)
	 * @return The resulting string
	 */
	public static String replace( String string, Pattern[] patterns, String[] replacements )
	{
		for( int i = 0, length = patterns.length; i < length; i++ )
			string = patterns[i].matcher( string ).replaceAll( replacements[i] );
		return string;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final String CLASS_SUFFIX = ".class";

	private static Pattern[] PRINT_ESCAPE_PATTERNS = new Pattern[]
	{
		Pattern.compile( "\\\\" ), Pattern.compile( "\\n" ), Pattern.compile( "\\r" ), Pattern.compile( "\\t" ), Pattern.compile( "\\f" ), Pattern.compile( "\\\"" )
	};

	private static String[] PRINT_ESCAPE_REPLACEMENTS = new String[]
	{
		Matcher.quoteReplacement( "\\\\" ), Matcher.quoteReplacement( "\\n" ), Matcher.quoteReplacement( "\\r" ), Matcher.quoteReplacement( "\\t" ), Matcher.quoteReplacement( "\\f" ), Matcher.quoteReplacement( "\\\"" )
	};

	/**
	 * Used by {@link #getFileForProgramClass(File, Executable, int)} to
	 * guarantee uniqueness.
	 */
	private static final ConcurrentMap<String, File> programFiles = new ConcurrentHashMap<String, File>();

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
