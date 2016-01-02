/**
 * Copyright 2009-2016 Three Crickets LLC.
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
import java.io.PrintStream;
import java.net.URI;
import java.util.Arrays;

import org.luaj.vm2.Globals;
import org.luaj.vm2.Lua;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.internal.ScripturianUtil;
import com.threecrickets.scripturian.internal.WriterOutputStream;

/**
 * A {@link LanguageAdapter} that supports the Lua language as implemented by <a
 * href="http://luaj.org/luaj/README.html">Luaj</a>.
 * 
 * @author Tal Liron
 */
public class LuajAdapter extends LanguageAdapterBase
{
	//
	// Constant
	//

	/**
	 * The Luaj instance context attribute.
	 */
	public static final String LUAJ_GLOBALS = LuajAdapter.class.getCanonicalName() + ".globals";

	/**
	 * The default base directory for cached executables.
	 */
	public static final String LUAJ_CACHE_DIR = "lua";

	//
	// Static operations
	//

	/**
	 * Creates an execution exception.
	 * 
	 * @param documentName
	 *        The document name
	 * @param x
	 *        The exception
	 * @return The execution exception
	 */
	public static ExecutionException createExecutionException( String documentName, LuaError x )
	{
		if( x.getCause() != null )
			return new ExecutionException( documentName, x.getCause() );
		else
			return new ExecutionException( documentName, x );
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
	public LuajAdapter() throws LanguageAdapterException
	{
		super( "Luaj", getImplementationVersion(), "Lua", "", Arrays.asList( "lua" ), "lua", Arrays.asList( "lua", "luaj" ), "luaj" );
	}

	//
	// Attributes
	//

	/**
	 * Gets the Luaj globals instance associated with the execution context,
	 * creating it if it doesn't exist. Each execution context is guaranteed to
	 * have its own Luaj globals instance. The globals instance is updated to
	 * match the writers and services in the execution context.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The Luaj globals
	 */
	public Globals getGlobals( ExecutionContext executionContext )
	{
		Globals globals = (Globals) executionContext.getAttributes().get( LUAJ_GLOBALS );

		if( globals == null )
		{
			globals = JsePlatform.standardGlobals();
			executionContext.getAttributes().put( LUAJ_GLOBALS, globals );
		}

		// Standard output and error
		globals.STDOUT = new PrintStream( new WriterOutputStream( executionContext.getWriterOrDefault() ), true );
		globals.STDERR = new PrintStream( new WriterOutputStream( executionContext.getErrorWriterOrDefault() ), true );

		// Append library locations to the Ruby class loader
		StringBuilder path = new StringBuilder();
		for( URI uri : executionContext.getLibraryLocations() )
		{
			try
			{
				File file = new File( uri );
				if( path.length() > 0 )
					path.append( ';' );
				path.append( file.getPath() );
				path.append( "/?.lua" );
			}
			catch( IllegalArgumentException x )
			{
				// URI is not a file
			}
		}
		globals.package_.setLuaPath( path.toString() );

		// Define services as entries in metatable
		LuaTable metatable = new LuaTable();
		metatable.rawset( LuaValue.INDEX, new GetService( executionContext ) );
		globals.setmetatable( metatable );

		return globals;
	}

	/**
	 * The base directory for cached executables.
	 * 
	 * @return The cache directory
	 */
	public File getCacheDir()
	{
		return new File( LanguageManager.getCachePath(), LUAJ_CACHE_DIR );
	}

	//
	// LanguageAdapter
	//

	@Override
	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = ScripturianUtil.doubleQuotedLiteral( literal );
		return "io.write(" + literal + ");";
	}

	@Override
	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "io.write(" + expression + ");";
	}

	@Override
	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		String containerIncludeCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_COMMAND_ATTRIBUTE );
		return executable.getExecutableServiceName() + ":getContainer():" + containerIncludeCommand + "(" + expression + ");";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new LuajProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	@Override
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		entryPointName = toLuaStyle( entryPointName );
		Globals globals = getGlobals( executionContext );

		LuaValue entryPoint = globals.get( entryPointName );
		if( entryPoint == LuaValue.NIL )
			throw new NoSuchMethodException( entryPointName );

		LuaValue[] luaArguments = new LuaValue[arguments.length];
		for( int i = arguments.length - 1; i >= 0; i-- )
			luaArguments[i] = CoerceJavaToLua.coerce( arguments[i] );
		try
		{
			LuaValue r = entryPoint.invoke( luaArguments ).arg1();
			if( r == LuaValue.NIL )
				r = null;

			return r;
		}
		catch( LuaError x )
		{
			throw createExecutionException( executable.getDocumentName(), x );
		}
		finally
		{
			globals.STDOUT.flush();
			globals.STDERR.flush();
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * Luaj implementation version.
	 * 
	 * @return Luaj implementation version
	 */
	private static String getImplementationVersion()
	{
		String version = Lua._VERSION;
		if( version.startsWith( "Luaj-" ) )
			version = version.substring( 5 );
		return version;
	}

	/**
	 * A Luaj function that retrieves an execution context service from a table.
	 */
	private static class GetService extends TwoArgFunction
	{
		public GetService( ExecutionContext executionContext )
		{
			this.executionContext = executionContext;
		}

		public LuaValue call( LuaValue table, LuaValue key )
		{
			if( key.isstring() )
				return CoerceJavaToLua.coerce( executionContext.getServices().get( key.tojstring() ) );
			else
				return this.rawget( key );
		}

		private final ExecutionContext executionContext;
	}

	/**
	 * From somethingLikeThis to something_like_this.
	 * 
	 * @param camelCase
	 *        somethingLikeThis
	 * @return something_like_this
	 */
	private static String toLuaStyle( String camelCase )
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
