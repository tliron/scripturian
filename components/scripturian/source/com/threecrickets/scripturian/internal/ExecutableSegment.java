/**
 * Copyright 2009-2013 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.internal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * A segment within an executable. "Text-with-scriptlets" executables can have
 * multiple segments, some of which are plain text and some of which are
 * scriptlets in various languages.
 * 
 * @author Tal Liron
 * @see Executable
 */
public class ExecutableSegment
{
	//
	// Constants
	//

	/**
	 * The default base directory for cached executables.
	 */
	public static final String CACHE_DIR = "scripturian";

	private static final String TXT_SUFFIX = ".txt";

	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param startLineNumber
	 *        The line number in the document for where the segment begins
	 * @param startColumnNumber
	 *        The column number in the document for where the segment begins
	 * @param isProgram
	 *        Whether this segment is a program
	 * @param isScriptlet
	 *        Whether this program is a scriptlet
	 * @param languageTag
	 *        The language tag for the program
	 */
	public ExecutableSegment( String sourceCode, int startLineNumber, int startColumnNumber, boolean isProgram, boolean isScriptlet, String languageTag )
	{
		this.sourceCode = sourceCode;
		this.startLineNumber = startLineNumber;
		this.startColumnNumber = startColumnNumber;
		this.isProgram = isProgram;
		this.isScriptlet = isScriptlet;
		this.languageTag = languageTag;
	}

	//
	// Attributes
	//

	/**
	 * Whether this segment is a program.
	 */
	public final boolean isProgram;

	/**
	 * Whether this program is a scriptlet.
	 */
	public final boolean isScriptlet;

	/**
	 * The language tag for scriptlets.
	 */
	public final String languageTag;

	/**
	 * The source code.
	 */
	public String sourceCode;

	/**
	 * The segment's position in the executable.
	 */
	public int position = 0;

	/**
	 * The line number in the document for where the segment begins.
	 */
	public int startLineNumber;

	/**
	 * The column number in the document for where the segment begins.
	 */
	public int startColumnNumber;

	/**
	 * The program.
	 * 
	 * @see #createProgram(Executable, LanguageManager, boolean)
	 */
	public Program program;

	//
	// Operations
	//

	/**
	 * Creates a program for this segment using the appropriate language
	 * adapter.
	 * 
	 * @param executable
	 *        The executable
	 * @param manager
	 *        The language manager
	 * @param prepare
	 *        Whether to prepare the program
	 * @param debug
	 *        Whether to debug the source code
	 * @throws ParsingException
	 * @see #isScriptlet
	 * @see #languageTag
	 * @see #program
	 */
	public void createProgram( Executable executable, LanguageManager manager, boolean prepare, boolean debug ) throws ParsingException
	{
		LanguageAdapter adapter = manager.getAdapterByTag( languageTag );
		if( adapter == null )
			throw ParsingException.adapterNotFound( executable.getDocumentName(), startLineNumber, startColumnNumber, languageTag );

		if( debug && isScriptlet )
		{
			String extension = (String) adapter.getAttributes().get( LanguageAdapter.DEFAULT_EXTENSION );
			extension = extension != null ? "." + extension : TXT_SUFFIX;

			File cacheDir = new File( LanguageManager.getCachePath(), CACHE_DIR );
			File dumpFile = ScripturianUtil.getFileForProgram( cacheDir, executable, position, extension );
			synchronized( dumpFile )
			{
				FileWriter writer = null;
				try
				{
					dumpFile.getParentFile().mkdirs();
					writer = new FileWriter( dumpFile );
					try
					{
						writer.write( sourceCode );
					}
					finally
					{
						writer.close();
					}
				}
				catch( IOException x )
				{
					if( writer != null )
					{
						try
						{
							writer.close();
						}
						catch( IOException xx )
						{
						}
					}
				}
			}
		}

		program = adapter.createProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable );

		if( prepare )
			program.prepare();
	}

	//
	// Object
	//

	@Override
	public String toString()
	{
		return "ExecutableSegment: " + languageTag + ( isProgram ? ", program, " : ", non-program, " ) + ( isScriptlet ? "scriptlet" : "non-scriptlet" );
	}
}