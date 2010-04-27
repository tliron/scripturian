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

import groovy.lang.Binding;
import groovy.lang.Script;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.tools.GroovyClass;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * @author Tal Liron
 */
class GroovyScriptlet extends ScriptletBase<GroovyAdapter>
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
	public GroovyScriptlet( String sourceCode, int position, int startLineNumber, int startColumnNumber, Executable executable, GroovyAdapter adapter )
	{
		super( sourceCode, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Scriptlet
	//

	@SuppressWarnings("unchecked")
	public void prepare() throws PreparationException
	{
		File classFile = new File( adapter.getCacheDir(), ScripturianUtil.getFilenameForScriptletClass( executable, position ) );
		String classname = ScripturianUtil.getClassnameForScriptlet( executable, position );

		try
		{
			Class<Script> scriptClass;
			if( classFile.exists() )
			{
				byte[] classByteArray = ScripturianUtil.getBytes( classFile );
				scriptClass = adapter.groovyClassLoader.defineClass( classname, classByteArray );
			}
			else
			{
				CompilationUnit compilationUnit = new CompilationUnit( adapter.groovyClassLoader );
				compilationUnit.addSource( classname + ".$", sourceCode );
				compilationUnit.compile( Phases.CLASS_GENERATION );

				// Groovy compiles each closure to its own class, meaning that
				// we may very well have several classes as the result of our
				// compilation. We'll save each of these classes separately.

				for( GroovyClass auxiliaryClass : (List<GroovyClass>) compilationUnit.getClasses() )
				{
					String postfix = auxiliaryClass.getName().substring( classname.length() );
					File auxiliaryClassFile = new File( classFile.getPath().substring( 0, classFile.getPath().length() - 6 ) + postfix + ".class" );

					// Cache it!
					auxiliaryClassFile.getParentFile().mkdirs();
					FileOutputStream stream = new FileOutputStream( auxiliaryClassFile );
					stream.write( auxiliaryClass.getBytes() );
					stream.close();
				}

				scriptClass = adapter.groovyClassLoader.loadClass( classname, false, true );
			}

			// What about the auxiliary classes mentioned above, requires for
			// the instance to work? Well, we've added our cache path to the
			// GroovyClassLoader, so it will load those automatically.

			script = scriptClass.newInstance();
		}
		catch( Exception x )
		{
			x.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Binding binding = adapter.getBinding( executionContext );

		try
		{
			if( script == null )
			{
				// Note that we're caching the resulting parsed script for the
				// future. Might as well!

				Class<Script> scriptClass = adapter.groovyClassLoader.parseClass( sourceCode, executable.getDocumentName() );
				script = scriptClass.newInstance();
			}

			script.setBinding( binding );
			script.run();
		}
		catch( InstantiationException x )
		{
			throw GroovyAdapter.createExecutionException( executable.getDocumentName(), x );
		}
		catch( IllegalAccessException x )
		{
			throw GroovyAdapter.createExecutionException( executable.getDocumentName(), x );
		}
		catch( Exception x )
		{
			throw GroovyAdapter.createExecutionException( executable.getDocumentName(), x );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private Script script;
}