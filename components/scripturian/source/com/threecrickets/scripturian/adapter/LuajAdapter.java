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

package com.threecrickets.scripturian.adapter;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;

import org.luaj.vm2.Globals;
import org.luaj.vm2.Lua;
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
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.internal.WriterOutputStream;

/**
 * A {@link LanguageAdapter} that supports the Lua language as implemented by <a
 * href="http://luaj.org/luaj.html">Luaj</a>.
 * 
 * @author Tal Liron
 */
public class LuajAdapter extends LanguageAdapterBase
{
	//
	// Constant
	//

	/**
	 * The LuaJ instance context attribute.
	 */
	public static final String LUAJ_GLOBALS = "luaj.globals";

	/**
	 * The default base directory for cached executables.
	 */
	public static final String LUAJ_CACHE_DIR = "lua";

	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 */
	public LuajAdapter() throws LanguageAdapterException
	{
		super( "Luaj", Lua._VERSION, "Lua", "", Arrays.asList( "lua" ), "lua", Arrays.asList( "lua", "luaj" ), "luaj" );
	}

	//
	// Attributes
	//

	/**
	 * Gets the LuaJ globals instance associated with the execution context,
	 * creating it if it doesn't exist. Each execution context is guaranteed to
	 * have its own LuaJ globals instance. The globals instance is updated to
	 * match the writers and services in the execution context.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The LuaJ globals
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

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new LuajProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * A LuaJ function that retrieves an execution context service from a table.
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
}
