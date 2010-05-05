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

import java.util.Arrays;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.optimizer.ClassCompiler;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * A {@link LanguageAdapter} that supports the <a
 * href="http://threecrickets.com/succinct/">Succinct</a> templating language.
 * 
 * @author Tal Liron
 */
public class SuccinctAdapter extends LanguageAdapterBase
{
	//
	// Constants
	//

	//
	// Static operations
	//

	/**
	 * Creates an execution exception with a full stack.
	 * 
	 * @param documentName
	 *        The document name
	 * @param x
	 *        The exception
	 * @return The execution exception
	 */
	public static ExecutionException createExecutionException( String documentName, Exception x )
	{
		return new ExecutionException( documentName, x.getMessage(), x );
	}

	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @throws LanguageAdapterException
	 */
	public SuccinctAdapter() throws LanguageAdapterException
	{
		super( "Velocity", new ContextFactory().enterContext().getImplementationVersion(), "Velocity", new ContextFactory().enterContext().getImplementationVersion(), Arrays.asList( "vm" ), null, Arrays.asList(
			"velocity", "vm" ), null );

		CompilerEnvirons compilerEnvirons = new CompilerEnvirons();
		classCompiler = new ClassCompiler( compilerEnvirons );
		generatedClassLoader = Context.getCurrentContext().createClassLoader( ClassLoader.getSystemClassLoader() );
		Context.exit();
	}

	//
	// Attributes
	//

	//
	// LanguageAdapter
	//

	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = literal.replaceAll( "\\n", "\\\\n" );
		literal = literal.replaceAll( "\\'", "\\\\'" );
		return "print('" + literal + "');";
	}

	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "print(" + expression + ");";
	}

	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		String containerIncludeExpressionCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_EXPRESSION_COMMAND );
		return executable.getExposedExecutableName() + ".container." + containerIncludeExpressionCommand + "(" + expression + ");";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new SuccinctProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	@Override
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		return null;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	/**
	 * Rhino class compiler.
	 */
	protected final ClassCompiler classCompiler;

	/**
	 * Rhino class loader.
	 */
	protected final GeneratedClassLoader generatedClassLoader;

	// //////////////////////////////////////////////////////////////////////////
	// Private
}
