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

package com.threecrickets.scripturian.adapter;

import java.util.Arrays;

import scala.tools.nsc.Properties;
import scala.tools.nsc.interpreter.IMain;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * A {@link LanguageAdapter} that supports the <a
 * href="http://www.scala-lang.org/">Scala</a> language.
 * 
 * @author Tal Liron
 */
public class ScalaAdapter extends LanguageAdapterBase
{
	//
	// Construction
	//

	public ScalaAdapter() throws LanguageAdapterException
	{
		super( "Scala", Properties.versionString(), "Scala", Properties.versionString(), Arrays.asList( "scala" ), "scala", Arrays.asList( "scala" ), "scala" );

	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new ScalaProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	protected IMain main;
}
