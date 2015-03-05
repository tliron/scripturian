/**
 * Copyright 2009-2015 Three Crickets LLC.
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
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import org.jruby.NativeException;
import org.jruby.Ruby;
import org.jruby.RubyException;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.ast.executable.Script;
import org.jruby.embed.io.WriterOutputStream;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariable.Scope;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Constants;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ClassCache;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.StackFrame;
import com.threecrickets.scripturian.internal.ScripturianUtil;
import com.threecrickets.scripturian.internal.SwitchableOutputStream;

/**
 * A {@link LanguageAdapter} that supports the Ruby language as implemented by
 * <a href="http://jruby.codehaus.org/">JRuby</a>.
 * 
 * @author Tal Liron
 */
public class JRubyAdapter extends LanguageAdapterBase
{
	//
	// Constants
	//

	/**
	 * The Ruby runtime instance attribute.
	 */
	public static final String JRUBY_RUNTIME = JRubyAdapter.class.getCanonicalName() + ".runtime";

	/**
	 * The Ruby class cache attribute.
	 */
	public static final String JRUBY_CLASS_CACHE = JRubyAdapter.class.getCanonicalName() + ".classCache";

	/**
	 * The switchable standard output attribute for the Ruby runtime.
	 */
	public static final String JRUBY_OUT = JRubyAdapter.class.getCanonicalName() + ".out";

	/**
	 * The switchable standard error attribute for the Ruby runtime.
	 */
	public static final String JRUBY_ERR = JRubyAdapter.class.getCanonicalName() + ".err";

	/**
	 * The default base directory for cached executables.
	 */
	public static final String RUBY_CACHE_DIR = "ruby";

	//
	// Static operations
	//

	/**
	 * Creates an execution exception with a full stack.
	 * 
	 * @param x
	 *        The Ruby exception
	 * @return The execution exception
	 */
	public static ExecutionException createExecutionException( RaiseException x )
	{
		RubyException rubyException = x.getException();
		if( rubyException instanceof NativeException )
		{
			NativeException nativeException = (NativeException) rubyException;
			Throwable cause = nativeException.getCause();

			if( cause instanceof ExecutionException )
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause );
				executionException.getStack().addAll( ( (ExecutionException) cause ).getStack() );
				for( RubyStackTraceElement stackTraceElement : rubyException.getBacktraceElements() )
					executionException.getStack().add( new StackFrame( stackTraceElement.getFileName(), stackTraceElement.getLineNumber(), -1 ) );
				return executionException;
			}
			else if( cause instanceof ParsingException )
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause );
				executionException.getStack().addAll( ( (ParsingException) cause ).getStack() );
				for( RubyStackTraceElement stackTraceElement : rubyException.getBacktraceElements() )
					executionException.getStack().add( new StackFrame( stackTraceElement.getFileName(), stackTraceElement.getLineNumber(), -1 ) );
				return executionException;
			}
			else
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause );
				for( RubyStackTraceElement stackTraceElement : rubyException.getBacktraceElements() )
					executionException.getStack().add( new StackFrame( stackTraceElement.getFileName(), stackTraceElement.getLineNumber(), -1 ) );
				return executionException;
			}
		}
		else
		{
			ExecutionException executionException = new ExecutionException( rubyException.message.asJavaString(), x );
			for( StackTraceElement stackTraceElement : x.getStackTrace() )
				if( stackTraceElement.getFileName().length() > 0 )
					executionException.getStack().add( new StackFrame( stackTraceElement ) );
			return executionException;
		}
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
	public JRubyAdapter() throws LanguageAdapterException
	{
		super( "JRuby", Constants.VERSION, "Ruby", Constants.RUBY_VERSION, Arrays.asList( "rb" ), "rb", Arrays.asList( "ruby", "rb", "jruby" ), "jruby" );

		RubyInstanceConfig config = new RubyInstanceConfig();
		config.setClassCache( getRubyClassCache() );
		config.setCompileMode( CompileMode.OFF );
		compilerRuntime = Ruby.newInstance( config );
	}

	//
	// Attributes
	//

	/**
	 * The class cache shared between all Ruby runtime instances.
	 * 
	 * @return The class cache
	 */
	public ClassCache<Script> getRubyClassCache()
	{
		@SuppressWarnings("unchecked")
		ClassCache<Script> classCache = (ClassCache<Script>) getAttributes().get( JRUBY_CLASS_CACHE );
		if( classCache == null )
		{
			classCache = new ClassCache<Script>( JRubyAdapter.class.getClassLoader(), 4096 );
			@SuppressWarnings("unchecked")
			ClassCache<Script> existing = (ClassCache<Script>) getAttributes().put( JRUBY_CLASS_CACHE, classCache );
			if( existing != null )
				classCache = existing;
		}
		return classCache;
	}

	/**
	 * Gets a Ruby runtime instance stored in the execution context, creating it
	 * if it doesn't exist. Each execution context is guaranteed to have its own
	 * Ruby runtime. The runtime instance is updated to match the writers and
	 * services in the execution context.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The Ruby runtime
	 */
	public Ruby getRubyRuntime( ExecutionContext executionContext )
	{
		Ruby rubyRuntime = (Ruby) executionContext.getAttributes().get( JRUBY_RUNTIME );
		SwitchableOutputStream switchableOut = (SwitchableOutputStream) executionContext.getAttributes().get( JRUBY_OUT );
		SwitchableOutputStream switchableErr = (SwitchableOutputStream) executionContext.getAttributes().get( JRUBY_ERR );

		if( rubyRuntime == null )
		{
			// We need to create a fresh runtime for each execution context,
			// because it's impossible to have the same runtime support multiple
			// threads running with different standard outs.

			switchableOut = new SwitchableOutputStream( new WriterOutputStream( executionContext.getWriterOrDefault() ) );
			switchableErr = new SwitchableOutputStream( new WriterOutputStream( executionContext.getErrorWriterOrDefault() ) );

			// System.setProperty( "jruby.jit.cache", "true" );
			// System.setProperty( "jruby.jit.codeCache", "ttt" );

			RubyInstanceConfig config = new RubyInstanceConfig();
			config.setClassCache( getRubyClassCache() );
			config.setCompileMode( CompileMode.OFF );
			config.setOutput( new PrintStream( switchableOut ) );
			config.setError( new PrintStream( switchableErr ) );
			rubyRuntime = Ruby.newInstance( config );
			executionContext.getAttributes().put( JRUBY_RUNTIME, rubyRuntime );
			executionContext.getAttributes().put( JRUBY_OUT, switchableOut );
			executionContext.getAttributes().put( JRUBY_ERR, switchableErr );
		}
		else
		{
			rubyRuntime.getOut().flush();
			rubyRuntime.getErr().flush();

			// Our switchable output stream lets us change the Ruby runtime's
			// standard output/error after it's been created.

			switchableOut.use( new WriterOutputStream( executionContext.getWriterOrDefault() ) );
			switchableErr.use( new WriterOutputStream( executionContext.getErrorWriterOrDefault() ) );
		}

		// Append library locations to the Ruby class loader
		for( URI uri : executionContext.getLibraryLocations() )
		{
			try
			{
				File file = new File( uri );
				rubyRuntime.getJRubyClassLoader().addURL( file.toURI().toURL() );
			}
			catch( IllegalArgumentException x )
			{
				// URI is not a file
			}
			catch( MalformedURLException x )
			{
			}
		}

		// Expose services as Ruby globals
		for( Map.Entry<String, Object> entry : executionContext.getServices().entrySet() )
		{
			// Note that we're using the shared compilerRuntime to do the
			// conversions, so that we can cache Java proxies. It's very, very
			// slow if we have to generate them on the fly every time!

			IRubyObject value = JavaUtil.convertJavaToRuby( compilerRuntime, entry.getValue() );
			rubyRuntime.defineReadonlyVariable( "$" + entry.getKey(), value, Scope.GLOBAL );
		}

		return rubyRuntime;
	}

	/**
	 * The base directory for cached executables.
	 * 
	 * @return The cache directory
	 */
	public File getCacheDir()
	{
		return new File( LanguageManager.getCachePath(), RUBY_CACHE_DIR );
	}

	//
	// LanguageAdapter
	//

	@Override
	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = ScripturianUtil.doubleQuotedLiteral( literal );
		return "print(" + literal + ");";
	}

	@Override
	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "print(" + expression + ");";
	}

	@Override
	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		String containerIncludeCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_COMMAND_ATTRIBUTE );
		containerIncludeCommand = toRubyStyle( containerIncludeCommand );
		return "$" + executable.getExecutableServiceName() + ".container." + containerIncludeCommand + "(" + expression + ");";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new JRubyProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	@Override
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		entryPointName = toRubyStyle( entryPointName );
		Ruby rubyRuntime = getRubyRuntime( executionContext );

		// Note that we're using the shared compilerRuntime to do the
		// conversions, so that we can cache Java proxies. It's very, very
		// slow if we have to generate them on the fly every time!

		IRubyObject[] rubyArguments = JavaUtil.convertJavaArrayToRuby( compilerRuntime, arguments );
		try
		{
			IRubyObject value = rubyRuntime.getTopSelf().callMethod( rubyRuntime.getCurrentContext(), entryPointName, rubyArguments );
			return value.toJava( Object.class );
		}
		catch( RaiseException x )
		{
			throw createExecutionException( x );
		}
		finally
		{
			rubyRuntime.getOut().flush();
			rubyRuntime.getErr().flush();
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	/**
	 * A shared Ruby runtime instance used for scriptlet preparation.
	 */
	protected final Ruby compilerRuntime;

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * From somethingLikeThis to something_like_this.
	 * 
	 * @param camelCase
	 *        somethingLikeThis
	 * @return something_like_this
	 */
	private static String toRubyStyle( String camelCase )
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
