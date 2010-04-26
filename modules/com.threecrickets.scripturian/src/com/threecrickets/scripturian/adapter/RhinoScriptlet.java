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

package com.threecrickets.scripturian.adapter;

import java.io.File;
import java.io.FileOutputStream;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * @author Tal Liron
 */
class RhinoScriptlet extends ScriptletBase<RhinoAdapter>
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param position
	 *        The scriptlet position in the document
	 * @param startLineNumber
	 *        The start line number
	 * @param startColumnNumber
	 *        The start column number
	 * @param executable
	 *        The executable
	 * @param adapter
	 *        The language adapter
	 */
	public RhinoScriptlet( String sourceCode, int position, int startLineNumber, int startColumnNumber, Executable executable, RhinoAdapter adapter )
	{
		super( sourceCode, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Scriptlet
	//

	public void prepare() throws PreparationException
	{
		File classFile = new File( adapter.getCacheDir(), ScripturianUtil.getFilenameForScriptletClass( executable, position ) );
		String classname = ScripturianUtil.getClassnameForScriptlet( executable, position );

		try
		{
			byte[] classByteArray;
			if( classFile.exists() )
				classByteArray = ScripturianUtil.getBytes( classFile );
			else
			{
				Object[] compiled = adapter.classCompiler.compileToClassFiles( sourceCode, executable.getDocumentName(), startLineNumber, classname );
				classByteArray = (byte[]) compiled[1];

				// Cache it!
				classFile.getParentFile().mkdirs();
				FileOutputStream stream = new FileOutputStream( classFile );
				stream.write( (byte[]) classByteArray );
				stream.close();
			}

			script = (Script) adapter.generatedClassLoader.defineClass( classname, classByteArray ).newInstance();
		}
		catch( Exception x )
		{
			x.printStackTrace();
		}
	}

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Context context = adapter.enterContext( executionContext );
		try
		{
			ScriptableObject scope = adapter.getScope( executable, executionContext, context, startLineNumber );
			if( script != null )
				script.exec( context, scope );
			else
				context.evaluateString( scope, sourceCode, executable.getDocumentName(), startLineNumber, null );
		}
		catch( Exception x )
		{
			throw RhinoAdapter.createExecutionException( executable.getDocumentName(), x );
		}
		finally
		{
			Context.exit();
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private Script script;
}