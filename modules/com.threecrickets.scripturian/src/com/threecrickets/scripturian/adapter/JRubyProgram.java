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
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.jruby.Ruby;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.compiler.ASTCompiler;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.exceptions.RaiseException;
import org.jruby.util.JRubyClassLoader;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * @author Tal Liron
 */
class JRubyProgram extends ProgramBase<JRubyAdapter>
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
	public JRubyProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, JRubyAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	@Override
	public void prepare() throws PreparationException
	{
		if( scriptReference.get() != null )
			return;

		// Note that we parse the node for a different runtime than the
		// one we will run in. It's unclear what the repercussions of
		// this would be, but we haven't detected any trouble yet.

		File classFile = ScripturianUtil.getFileForProgramClass( adapter.getCacheDir(), executable, position );
		String classname = ScripturianUtil.getClassnameForProgram( executable, position );

		synchronized( classFile )
		{
			try
			{
				if( classFile.exists() )
				{
					// Use cached compiled code
					byte[] classByteArray = ScripturianUtil.getBytes( classFile );
					JRubyClassLoader rubyClassLoader = new JRubyClassLoader( adapter.compilerRuntime.getJRubyClassLoader() );
					rubyClassLoader.defineClass( classname, classByteArray );
					Class<?> scriptClass = rubyClassLoader.loadClass( classname );
					scriptReference.compareAndSet( null, (Script) scriptClass.newInstance() );
				}
				else
				{
					Node node = adapter.compilerRuntime.parseEval( sourceCode, executable.getDocumentName(), adapter.compilerRuntime.getCurrentContext().getCurrentScope(), startLineNumber - 1 );

					ASTInspector astInspector = new ASTInspector();
					ASTCompiler astCompiler = adapter.compilerRuntime.getInstanceConfig().newCompiler();
					StandardASMCompiler asmCompiler = new StandardASMCompiler( classname.replace( '.', '/' ), executable.getDocumentName() );
					JRubyClassLoader classLoader = new JRubyClassLoader( adapter.compilerRuntime.getJRubyClassLoader() );

					astInspector.inspect( node );
					astCompiler.compileRoot( node, asmCompiler, astInspector, true, false );
					scriptReference.compareAndSet( null, (Script) asmCompiler.loadClass( classLoader ).newInstance() );

					// Cache it!
					classFile.getParentFile().mkdirs();
					FileOutputStream stream = new FileOutputStream( classFile );
					stream.write( asmCompiler.getClassByteArray() );
					stream.close();

					// script = compilerRuntime.tryCompile( node );
				}
			}
			catch( RaiseException x )
			{
				throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, x );
			}
			catch( InstantiationException x )
			{
				throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, x );
			}
			catch( IllegalAccessException x )
			{
				throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, x );
			}
			catch( ClassNotFoundException x )
			{
				throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, x );
			}
			catch( IOException x )
			{
				throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, x );
			}
		}
	}

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Ruby rubyRuntime = adapter.getRubyRuntime( executionContext );

		try
		{
			Script script = scriptReference.get();
			if( script != null )
				rubyRuntime.runScript( script );
			else
				rubyRuntime.executeScript( sourceCode, executable.getDocumentName() );
		}
		catch( RaiseException x )
		{
			throw JRubyAdapter.createExecutionException( executable.getDocumentName(), x );
		}
		finally
		{
			rubyRuntime.getOut().flush();
			rubyRuntime.getErr().flush();
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The cached compiled script.
	 */
	private final AtomicReference<Script> scriptReference = new AtomicReference<Script>();
}