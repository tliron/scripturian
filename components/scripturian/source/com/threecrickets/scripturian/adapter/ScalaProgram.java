/**
 * Copyright 2009-2012 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.adapter;

import java.io.PrintWriter;
import java.io.Writer;

import scala.collection.immutable.Nil;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.IMain;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * @author Tal Liron
 */
public class ScalaProgram extends ProgramBase<ScalaAdapter>
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param isScriptlet
	 *        Whether the source code is a scriptlet
	 * @param position
	 *        The program's position in the executable
	 * @param startLineNumber
	 *        The line number in the document for where the program's source
	 *        code begins
	 * @param startColumnNumber
	 *        The column number in the document for where the program's source
	 *        code begins
	 * @param executable
	 *        The executable
	 * @param adapter
	 *        The language adapter
	 */
	public ScalaProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, ScalaAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Settings settings = new Settings();
		settings.usejavacp().tryToSetFromPropertyValue( "true" );
		settings.stopAfter().tryToSetColon( Nil.$colon$colon( "dce" ) );
		settings.debug().tryToSetFromPropertyValue( "true" );

		Writer writer = executionContext.getWriter();
		PrintWriter printWriter = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter( writer, true );

		IMain main = new IMain( settings, printWriter );
		main.setContextClassLoader();

		main.compileString( sourceCode );
	}
}
