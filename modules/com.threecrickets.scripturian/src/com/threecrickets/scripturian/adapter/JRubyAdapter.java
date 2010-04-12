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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jruby.Ruby;
import org.jruby.embed.io.WriterOutputStream;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.Scriptlet;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * A {@link LanguageAdapter} that supports the Ruby language as implemented by
 * <a href="http://jruby.codehaus.org/">JRuby</a>.
 * 
 * @author Tal Liron
 */
public class JRubyAdapter implements LanguageAdapter
{
	//
	// Construction
	//

	public JRubyAdapter() throws LanguageAdapterException
	{
		attributes.put( NAME, "JRuby" );
		attributes.put( VERSION, Constants.VERSION );
		attributes.put( LANGUAGE_NAME, "Ruby" );
		attributes.put( LANGUAGE_VERSION, Constants.RUBY_VERSION );
		attributes.put( EXTENSIONS, Arrays.asList( "rb" ) );
		attributes.put( DEFAULT_EXTENSION, "rb" );
		attributes.put( TAGS, Arrays.asList( "ruby", "jruby" ) );
		attributes.put( DEFAULT_TAG, "ruby" );
	}

	//
	// Static operations
	//

	public static Ruby getRubyRuntime( ExecutionContext executionContext )
	{
		Ruby rubyRuntime = (Ruby) executionContext.getAttributes().get( JRUBY_RUNTIME );
		if( rubyRuntime == null )
		{
			// We need to create a fresh runtime for each execution context,
			// because it's impossible to have the same runtime support multiple
			// threads running with different standard outs.

			PrintStream out = new PrintStream( new WriterOutputStream( executionContext.getWriter() ) );
			PrintStream err = new PrintStream( new WriterOutputStream( executionContext.getErrorWriter() ) );
			rubyRuntime = Ruby.newInstance( System.in, out, err );
			executionContext.getAttributes().put( JRUBY_RUNTIME, rubyRuntime );
		}

		// Expose variables as Ruby globals
		for( Map.Entry<String, Object> entry : executionContext.getExposedVariables().entrySet() )
		{
			IRubyObject value = JavaUtil.convertJavaToUsableRubyObject( rubyRuntime, entry.getValue() );
			rubyRuntime.defineReadonlyVariable( "$" + entry.getKey(), value );
		}

		return rubyRuntime;
	}

	//
	// LanguageAdapter
	//

	public Map<String, Object> getAttributes()
	{
		return attributes;
	}

	public boolean isThreadSafe()
	{
		return true;
	}

	public Lock getLock()
	{
		return lock;
	}

	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = literal.replaceAll( "\\n", "\\\\n" );
		literal = literal.replaceAll( "\\\"", "\\\\\"" );
		return "print(\"" + literal + "\");";
	}

	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "print(" + expression + ");";
	}

	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		return "$" + executable.getExposedExecutableName() + ".container.include_document(" + expression + ");";
	}

	public Throwable getCauseOrExecutionException( String executableName, Throwable throwable )
	{
		return null;
	}

	public Scriptlet createScriptlet( String sourceCode, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new JRubyScriptlet( sourceCode, startLineNumber, startColumnNumber, executable );
	}

	public Object invoke( String entryPointName, Executable executable, ExecutionContext executionContext ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		entryPointName = toRubyStyle( entryPointName );
		Ruby ruby = getRubyRuntime( executionContext );
		IRubyObject value = ruby.getTopSelf().callMethod( ruby.getCurrentContext(), entryPointName );
		return value.toJava( Object.class );
	}

	public void releaseContext( ExecutionContext executionContext )
	{
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final String JRUBY_RUNTIME = "jruby.rubyRuntime";

	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	private final ReentrantLock lock = new ReentrantLock();

	/**
	 * From somethingLikeThis to something_like_this.
	 * 
	 * @param camelCase
	 * @return
	 */
	private static String toRubyStyle( String camelCase )
	{
		StringBuilder r = new StringBuilder();
		char c = camelCase.charAt( 0 );
		if( Character.isUpperCase( c ) )
			r.append( Character.toLowerCase( c ) );
		else
			r.append( c );
		for( int i = 1; i < camelCase.length(); i++ )
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
