/**
 * Copyright 2009-2017 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.adapter;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import jep.Jep;
import jep.JepException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * A {@link LanguageAdapter} that supports the Python language as implemented by
 * <a href="http://jepp.sourceforge.net/">Jepp</a>.
 * 
 * @author Tal Liron
 */
public class JeppAdapter extends LanguageAdapterBase
{
	//
	// Constants
	//

	/**
	 * The Jepp runtime attribute.
	 */
	public static final String JEPP_RUNTIME = JeppAdapter.class.getCanonicalName() + ".runtime";

	/**
	 * The default base directory for cached executables.
	 */
	public static final String JEPP_CACHE_DIR = "python/jepp";

	//
	// Static attributes
	//

	/**
	 * Gets a Jepp runtime stored in the execution context, creating it if it
	 * doesn't exist. Each execution context is guaranteed to have its own Jepp
	 * runtime. The runtime is updated to match the writers and services in the
	 * execution context.
	 * 
	 * @param executable
	 *        The executable
	 * @param executionContext
	 *        The execution context
	 * @return The Jepp runtime
	 * @throws JepException
	 *         In case of a Jepp error
	 */
	public static Jep getJeppRuntime( Executable executable, ExecutionContext executionContext ) throws JepException
	{
		Jep jeppRuntime = (Jep) executionContext.getAttributes().get( JEPP_RUNTIME );
		if( jeppRuntime == null )
		{
			jeppRuntime = new Jep();
			executionContext.getAttributes().put( JEPP_RUNTIME, jeppRuntime );

			// Enable imports that were disabled in Jepp 2.4
			jeppRuntime.eval( "from jep import *;__builtins__.__import__=jep.jimport" );
		}

		jeppRuntime.eval( "import sys" );

		// Append library locations to sys.path
		for( URI uri : executionContext.getLibraryLocations() )
		{
			try
			{
				String path = new File( uri ).getPath();
				jeppRuntime.eval( "sys.path.append('" + path.replace( "'", "\\'" ) + "')" );
			}
			catch( IllegalArgumentException x )
			{
				// URI is not a file
			}
		}

		// Expose services in runtime
		for( Map.Entry<String, Object> entry : executionContext.getServices().entrySet() )
			jeppRuntime.set( entry.getKey(), entry.getValue() );

		// Connect writers to sys
		jeppRuntime.eval( "sys.stdout=" + executable.getExecutableServiceName() + ".getContext().getWriter();sys.stderr=" + executable.getExecutableServiceName() + ".getContext().getErrorWriter()" );

		return jeppRuntime;
	}

	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 *         In case of an initialization error
	 */
	public JeppAdapter() throws LanguageAdapterException
	{
		super( "Jepp", "", "Python", "", Arrays.asList( "py", "jepp" ), "py", Arrays.asList( "python", "py", "jepp" ), "jepp" );
	}

	//
	// Attributes
	//

	/**
	 * The base directory for cached executables.
	 * 
	 * @return The cache directory
	 */
	public File getCacheDir()
	{
		return new File( LanguageManager.getCachePath(), JEPP_CACHE_DIR );
	}

	//
	// LanguageAdapter
	//

	@Override
	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = ScripturianUtil.doubleQuotedLiteral( literal );
		return "sys.stdout.write(" + literal + "),;";
	}

	@Override
	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "sys.stdout.write(str(" + expression + "));";
	}

	@Override
	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		String containerIncludeCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_COMMAND_ATTRIBUTE );
		return executable.getExecutableServiceName() + ".getContainer()." + containerIncludeCommand + "(" + expression + ");";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new JeppProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	@Override
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		entryPointName = toPythonStyle( entryPointName );
		try
		{
			Jep jeppRuntime = JeppAdapter.getJeppRuntime( executable, executionContext );
			return jeppRuntime.invoke( entryPointName, arguments );
		}
		catch( JepException x )
		{
			throw new ExecutionException( executable.getDocumentName(), x.getMessage(), x );
		}
	}

	@Override
	public void releaseContext( ExecutionContext executionContext )
	{
		Jep jep = (Jep) executionContext.getAttributes().get( JEPP_RUNTIME );
		if( jep != null )
			jep.close();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * From somethingLikeThis to something_like_this.
	 * 
	 * @param camelCase
	 *        somethingLikeThis
	 * @return something_like_this
	 */
	private static String toPythonStyle( String camelCase )
	{
		StringBuilder r = new StringBuilder();
		char c = camelCase.charAt( 0 );
		if( Character.isUpperCase( c ) )
			r.append( Character.toLowerCase( c ) );
		else
			r.append( c );
		for( int i = 1, length = camelCase.length(); i < length; i++ )
		{
			c = camelCase.charAt( i );
			if( Character.isUpperCase( c ) )
			{
				r.append( '_' );
				r.append( Character.toLowerCase( c ) );
			}
			else
				r.append( c );
		}
		return r.toString();
	}
}
