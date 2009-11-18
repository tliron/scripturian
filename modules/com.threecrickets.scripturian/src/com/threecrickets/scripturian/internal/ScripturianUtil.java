/**
 * Copyright 2009 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://www.threecrickets.com/
 */

package com.threecrickets.scripturian.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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

	public static ConcurrentMap<String, String> scriptEngineNamesByExtension = new ConcurrentHashMap<String, String>();

	static
	{
		scriptEngineNamesByExtension.put( "py", "python" );
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

	public static String getScriptEngineNameByExtension( String name, ScriptEngineManager scriptEngineManager ) throws ScriptException
	{
		int dot = name.lastIndexOf( '.' );
		if( dot == -1 )
			throw new ScriptException( "Name must have an extension: " + name );
		String extension = name.substring( dot + 1 );

		// Try our mapping first
		String engineName = scriptEngineNamesByExtension.get( extension );

		if( engineName == null )
		{
			// Try script engine factory's mappings
			ScriptEngine scriptEngine = scriptEngineManager.getEngineByExtension( extension );
			if( scriptEngine == null )
				throw new ScriptException( "Name's extension is not recognized by any script engine: " + extension );
			try
			{
				return scriptEngine.getFactory().getNames().get( 0 );
			}
			catch( IndexOutOfBoundsException x )
			{
				throw new ScriptException( "Script engine has no names: " + scriptEngine );
			}
		}
		else
			return engineName;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private ScripturianUtil()
	{
	}
}
