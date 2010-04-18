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

import org.jruby.NativeException;
import org.jruby.Ruby;
import org.jruby.RubyException;
import org.jruby.embed.io.WriterOutputStream;
import org.jruby.exceptions.RaiseException;
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
import com.threecrickets.scripturian.exception.StackFrame;
import com.threecrickets.scripturian.internal.SwitchableOutputStream;

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
		SwitchableOutputStream switchableOut = (SwitchableOutputStream) executionContext.getAttributes().get( JRUBY_OUT );
		SwitchableOutputStream switchableErr = (SwitchableOutputStream) executionContext.getAttributes().get( JRUBY_ERR );

		if( rubyRuntime == null )
		{
			// We need to create a fresh runtime for each execution context,
			// because it's impossible to have the same runtime support multiple
			// threads running with different standard outs.

			switchableOut = new SwitchableOutputStream( new WriterOutputStream( executionContext.getWriter() ) );
			switchableErr = new SwitchableOutputStream( new WriterOutputStream( executionContext.getErrorWriter() ) );

			// System.setProperty( "jruby.backtrace.style", "ruby_framed" );
			rubyRuntime = Ruby.newInstance( System.in, new PrintStream( switchableOut ), new PrintStream( switchableErr ) );
			executionContext.getAttributes().put( JRUBY_RUNTIME, rubyRuntime );
			executionContext.getAttributes().put( JRUBY_OUT, switchableOut );
			executionContext.getAttributes().put( JRUBY_ERR, switchableErr );
		}
		else
		{
			// Out switchable output stream lets us change the Ruby runtime's
			// standard output/error after it's been created.

			switchableOut.use( new WriterOutputStream( executionContext.getWriter() ) );
			switchableErr.use( new WriterOutputStream( executionContext.getErrorWriter() ) );
		}

		// Expose variables as Ruby globals
		for( Map.Entry<String, Object> entry : executionContext.getExposedVariables().entrySet() )
		{
			IRubyObject value = JavaUtil.convertJavaToUsableRubyObject( rubyRuntime, entry.getValue() );
			rubyRuntime.defineReadonlyVariable( "$" + entry.getKey(), value );
		}

		return rubyRuntime;
	}

	/**
	 * @param x
	 * @return
	 */
	public static ExecutionException createExecutionException( RaiseException x )
	{
		RubyException rubyException = x.getException();
		if( rubyException instanceof NativeException )
		{
			NativeException nativeException = (NativeException) rubyException;
			Throwable cause = nativeException.getCause();
			if( cause instanceof ExecutionException )
				// Pass through
				return (ExecutionException) cause;

			ExecutionException executionException = new ExecutionException( cause.getMessage(), cause );
			for( StackTraceElement stackTraceElement : cause.getStackTrace() )
				if( stackTraceElement.getFileName().length() > 0 )
					executionException.getStack().add( new StackFrame( stackTraceElement ) );
			return executionException;
		}
		else
		{
			ExecutionException executionException = new ExecutionException( x.getMessage(), x );
			for( StackTraceElement stackTraceElement : x.getStackTrace() )
				if( stackTraceElement.getFileName().length() > 0 )
					executionException.getStack().add( new StackFrame( stackTraceElement ) );
			return executionException;
		}
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

	public Scriptlet createScriptlet( String sourceCode, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new JRubyScriptlet( sourceCode, startLineNumber, startColumnNumber, executable );
	}

	public Object invoke( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		entryPointName = toRubyStyle( entryPointName );
		Ruby rubyRuntime = getRubyRuntime( executionContext );
		IRubyObject[] rubyArguments = JavaUtil.convertJavaArrayToRuby( rubyRuntime, arguments );
		try
		{
			IRubyObject value = rubyRuntime.getTopSelf().callMethod( rubyRuntime.getCurrentContext(), entryPointName, rubyArguments );
			return value.toJava( Object.class );
		}
		catch( RaiseException x )
		{
			throw createExecutionException( x );
		}
	}

	public void releaseContext( ExecutionContext executionContext )
	{
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final String JRUBY_RUNTIME = "jruby.rubyRuntime";

	private static final String JRUBY_OUT = "jruby.out";

	private static final String JRUBY_ERR = "jruby.err";

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
