/**
 * Copyright 2009-2011 Three Crickets LLC.
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.velocity.runtime.RuntimeInstance;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * A {@link LanguageAdapter} that supports the <a
 * href="http://velocity.apache.org/">Velocity</a> templating language.
 * <p>
 * Language manager attributes prefixed with "velocity." will be used to
 * initialize Velocity's runtime instance.
 * 
 * @author Tal Liron
 */
public class VelocityAdapter extends LanguageAdapterBase
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 */
	public VelocityAdapter() throws LanguageAdapterException
	{
		super( "Velocity", "", null, null, Arrays.asList( "vm" ), null, Arrays.asList( "velocity", "vm" ), null );
	}

	//
	// Attributes
	//

	/**
	 * The Velocity runtime instance for this adapter, creating and initializing
	 * it if it doesn't exist.
	 * 
	 * @return The runtime instance
	 */
	public RuntimeInstance getRuntimeInstance()
	{
		RuntimeInstance runtimeInstance = runtimeInstanceReference.get();
		if( runtimeInstance == null )
		{
			runtimeInstance = new RuntimeInstance();

			try
			{
				LanguageManager manager = getManager();
				if( manager == null )
					runtimeInstance.init();
				else
					runtimeInstance.init( manager.getAttributesAsProperties( "velocity." ) );
			}
			catch( Exception x )
			{
				x.printStackTrace();
			}

			if( !runtimeInstanceReference.compareAndSet( null, runtimeInstance ) )
				runtimeInstance = runtimeInstanceReference.get();
		}
		return runtimeInstance;
	}

	//
	// LanguageAdapter
	//

	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		// Dark magicks to allow us to easily escape Velocity tokens
		// (see VelocityProgram.execute)
		literal = ScripturianUtil.replace( literal, LITERAL_ESCAPE_PATTERNS, LITERAL_ESCAPE_REPLACEMENTS );
		return literal;
	}

	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "${" + expression.trim() + "}";
	}

	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		String containerIncludeExpressionCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_EXPRESSION_COMMAND );
		return "#if($" + executable.getExecutableServiceName() + ".container." + containerIncludeExpressionCommand + "(" + expression + "))#end ";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new VelocityProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final AtomicReference<RuntimeInstance> runtimeInstanceReference = new AtomicReference<RuntimeInstance>();

	private static Pattern[] LITERAL_ESCAPE_PATTERNS = new Pattern[]
	{
		Pattern.compile( "\\$" ), Pattern.compile( "\\#" )
	};

	private static String[] LITERAL_ESCAPE_REPLACEMENTS = new String[]
	{
		"\\${_d}", "\\${_h}"
	};

	static
	{
		// This makes sure class loading will fail if Velocity is not present
		new RuntimeInstance();
	}
}
