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
class GroovyProgram extends ProgramBase<GroovyAdapter>
{
	//
	// Construction
	//

	/**
	 * Construction.
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
	public GroovyProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, GroovyAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	@Override
	@SuppressWarnings("unchecked")
	public void prepare() throws PreparationException
	{
		File mainClassFile = ScripturianUtil.getFileForProgramClass( adapter.getCacheDir(), executable, position );
		String classname = ScripturianUtil.getClassnameForProgram( executable, position );

		synchronized( mainClassFile )
		{
			try
			{
				Class<Script> scriptClass;
				if( mainClassFile.exists() )
				{
					byte[] classByteArray = ScripturianUtil.getBytes( mainClassFile );
					scriptClass = adapter.groovyClassLoader.defineClass( classname, classByteArray );
				}
				else
				{
					CompilationUnit compilationUnit = new CompilationUnit( adapter.groovyClassLoader );

					// We're adding an extension to the classname, because
					// Groovy expects one and will remove it. Note that it
					// doesn't matter what the extension is.

					compilationUnit.addSource( classname + ".$", sourceCode );
					compilationUnit.compile( Phases.CLASS_GENERATION );

					// Groovy compiles each closure to its own class, meaning
					// that we may very well have several classes as the result
					// of our compilation. We'll save each of these classes
					// separately.

					for( GroovyClass groovyClass : (List<GroovyClass>) compilationUnit.getClasses() )
					{
						String postfix = groovyClass.getName().substring( classname.length() );
						File classFile = new File( mainClassFile.getPath().substring( 0, mainClassFile.getPath().length() - 6 ) + postfix + ".class" );

						// Cache it!
						classFile.getParentFile().mkdirs();
						FileOutputStream stream = new FileOutputStream( classFile );
						stream.write( groovyClass.getBytes() );
						stream.close();
					}

					// We have to re-add the cache to the classpath so that it
					// will recognize our new class files there.

					adapter.groovyClassLoader.addClasspath( adapter.getCacheDir().getPath() );

					scriptClass = adapter.groovyClassLoader.loadClass( classname, false, true );
				}

				// What about the auxiliary classes mentioned above, requires
				// for the instance to work? Well, we've added our cache path to
				// the GroovyClassLoader, so it will load those automatically.

				script = scriptClass.newInstance();
			}
			catch( Exception x )
			{
				x.printStackTrace();
			}
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