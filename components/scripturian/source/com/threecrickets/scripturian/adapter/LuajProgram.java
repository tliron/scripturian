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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.compiler.DumpState;
import org.luaj.vm2.compiler.LuaC;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * @author Tal Liron
 */
public class LuajProgram extends ProgramBase<LuajAdapter>
{
	//
	// Constructor
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
	public LuajProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, LuajAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	@Override
	public void prepare() throws PreparationException
	{
		String documentName = executable.getDocumentName();
		File dumpFile = ScripturianUtil.getFileForProgram( adapter.getCacheDir(), executable, position, LUO_SUFFIX );

		synchronized( dumpFile )
		{
			try
			{
				if( dumpFile.exists() )
				{
					byte[] bytes = ScripturianUtil.getBytes( dumpFile );
					bytesReference.compareAndSet( null, bytes );

					// We can't compile the bytes now, because we don't have an
					// execution context. So we'll finish preparing in
					// execute().
				}
				else
				{
					dumpFile.getParentFile().mkdirs();
					FileOutputStream out = new FileOutputStream( dumpFile );
					try
					{
						Prototype prototype = LuaC.compile( new Utf8Encoder( new StringReader( sourceCode ) ), documentName );
						DumpState.dump( prototype, out, STRIP_DEBUG, NUMBER_FORMAT, LITTLE_ENDIAN );

						prototypeReference.compareAndSet( null, prototype );
					}
					finally
					{
						out.close();
					}
				}
			}
			catch( Exception x )
			{
				throw new PreparationException( executable.getDocumentName(), x );
			}
		}
	}

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Globals globals = adapter.getGlobals( executionContext );

		LuaFunction function = null;

		Prototype prototype = prototypeReference.get();
		if( prototype == null )
		{
			byte[] bytes = bytesReference.get();
			if( bytes != null )
			{
				// Finish preparation
				try
				{
					prototype = LuaC.compile( new ByteArrayInputStream( bytes ), executable.getDocumentName() );
					if( !prototypeReference.compareAndSet( null, prototype ) )
						prototype = prototypeReference.get();
				}
				catch( IOException x )
				{
					throw new PreparationException( executable.getDocumentName(), x );
				}
			}
		}

		if( prototype != null )
			function = new LuaClosure( prototype, globals );

		if( function == null )
		{
			InputStream stream = new Utf8Encoder( new StringReader( sourceCode ) );
			try
			{
				function = LoadState.load( stream, executable.getDocumentName(), "t", globals );
				if( function.isclosure() )
					function = new LuaClosure( function.checkclosure().p, globals );
				else
				{
					function = function.getClass().newInstance();
					function.initupvalue1( globals );
				}
			}
			catch( Exception x )
			{
				throw new ParsingException( executable.getDocumentName(), x );
			}
			finally
			{
				try
				{
					stream.close();
				}
				catch( IOException x )
				{
					throw new ParsingException( executable.getDocumentName(), x );
				}
			}
		}

		try
		{
			function.invoke( LuaValue.NONE );
		}
		catch( LuaError x )
		{
			throw LuajAdapter.createExecutionException( executable.getDocumentName(), x );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final String LUO_SUFFIX = ".luo";

	private static final boolean STRIP_DEBUG = false;

	private static final int NUMBER_FORMAT = DumpState.NUMBER_FORMAT_DEFAULT;

	private static final boolean LITTLE_ENDIAN = false;

	/**
	 * The cached compiled prototype.
	 */
	private final AtomicReference<Prototype> prototypeReference = new AtomicReference<Prototype>();

	/**
	 * The cached bytes.
	 */
	private final AtomicReference<byte[]> bytesReference = new AtomicReference<byte[]>();

	/**
	 * From: LuaScriptEngine
	 */
	private static final class Utf8Encoder extends InputStream
	{
		private Utf8Encoder( Reader reader )
		{
			this.reader = reader;
		}

		public int read() throws IOException
		{
			if( n > 0 )
				return buffer[--n];
			int c = reader.read();
			if( c < 0x80 )
				return c;
			n = 0;
			if( c < 0x800 )
			{
				buffer[n++] = ( 0x80 | ( c & 0x3f ) );
				return ( 0xC0 | ( ( c >> 6 ) & 0x1f ) );
			}
			else
			{
				buffer[n++] = ( 0x80 | ( c & 0x3f ) );
				buffer[n++] = ( 0x80 | ( ( c >> 6 ) & 0x3f ) );
				return ( 0xE0 | ( ( c >> 12 ) & 0x0f ) );
			}
		}

		private final Reader reader;

		private final int[] buffer = new int[2];

		private int n;
	}
}
