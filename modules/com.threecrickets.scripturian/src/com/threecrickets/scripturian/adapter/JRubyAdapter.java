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
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.io.WriterOutputStream;
import org.jruby.javasupport.JavaEmbedUtils;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.Scriptlet;
import com.threecrickets.scripturian.exception.CompilationException;
import com.threecrickets.scripturian.exception.ExecutableInitializationException;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageInitializationException;

/**
 * An {@link LanguageAdapter} that supports the Ruby scripting language as
 * implemented by <a href="http://jruby.codehaus.org/">JRuby</a>.
 * 
 * @author Tal Liron
 */
public class JRubyAdapter implements LanguageAdapter
{
	//
	// Construction
	//

	public JRubyAdapter() throws LanguageInitializationException
	{
		attributes.put( NAME, "JRuby" );
		attributes.put( VERSION, "?" );
		attributes.put( LANGUAGE_NAME, "Ruby" );
		attributes.put( LANGUAGE_VERSION, "?" );
		attributes.put( EXTENSIONS, Arrays.asList( "rb" ) );
		attributes.put( DEFAULT_EXTENSION, "rb" );
		attributes.put( TAGS, Arrays.asList( "ruby", "jruby" ) );
		attributes.put( DEFAULT_TAG, "ruby" );
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

	public String getCodeForLiteralOutput( String literal, Executable executable ) throws ExecutableInitializationException
	{
		literal = literal.replaceAll( "\\n", "\\\\n" );
		literal = literal.replaceAll( "\\\"", "\\\\\"" );
		return "print(\"" + literal + "\");";
	}

	public String getCodeForExpressionOutput( String expression, Executable executable ) throws ExecutableInitializationException
	{
		return "print(" + expression + ");";
	}

	public String getCodeForExpressionInclude( String expression, Executable executable ) throws ExecutableInitializationException
	{
		return "$" + executable.getExposedExecutableName() + ".container.include_document(" + expression + ");";
	}

	public Throwable getCauseOrExecutionException( String executableName, Throwable throwable )
	{
		return null;
	}

	public Scriptlet createScriptlet( String code, Executable executable ) throws ExecutableInitializationException
	{
		return new JRubyScriptlet( code );
	}

	public Object invoke( String method, Executable executable, ExecutionContext executionContext ) throws NoSuchMethodException, ExecutableInitializationException, ExecutionException
	{
		Ruby ruby = getRubyRuntime( executionContext );
		Object r = JavaEmbedUtils.rubyToJava( ruby.getTopSelf().callMethod( ruby.getCurrentContext(), method ) );
		// System.out.println( r );
		return r;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final String RUBY_RUNTIME = "jruby.rubyRuntime";

	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	private final ReentrantLock lock = new ReentrantLock();

	private final Ruby compilerRuntime = Ruby.newInstance();

	private Ruby getRubyRuntime( ExecutionContext executionContext )
	{
		Ruby ruby;
		ruby = (Ruby) executionContext.getAttributes().get( RUBY_RUNTIME );
		if( ruby == null )
		{
			PrintStream out = new PrintStream( new WriterOutputStream( executionContext.getWriter() ) );
			PrintStream err = new PrintStream( new WriterOutputStream( executionContext.getErrorWriter() ) );
			ruby = Ruby.newInstance( System.in, out, err );
			executionContext.getAttributes().put( RUBY_RUNTIME, ruby );
		}

		for( Map.Entry<String, Object> entry : executionContext.getExposedVariables().entrySet() )
			ruby.defineReadonlyVariable( "$" + entry.getKey(), JavaEmbedUtils.javaToRuby( ruby, entry.getValue() ) );

		return ruby;
	}

	private class JRubyScriptlet implements Scriptlet
	{
		public JRubyScriptlet( String sourceCode )
		{
			this.sourceCode = sourceCode;
		}

		public void compile() throws CompilationException
		{
			// try
			{
				Node node = compilerRuntime.parseEval( sourceCode, "prudence", compilerRuntime.getCurrentContext().getCurrentScope(), 0 );
				script = compilerRuntime.tryCompile( node );
			}
			// catch( EvalFailedException x )
			// {
			// throw new CompilationException( "", x.getMessage(), x );
			// }
		}

		public Object execute( ExecutionContext executionContext ) throws ExecutableInitializationException, ExecutionException
		{
			Ruby ruby = getRubyRuntime( executionContext );

			if( script != null )
			{
				return JavaEmbedUtils.rubyToJava( ruby.runScript( script ) );
			}
			else
			{
				try
				{
					Object r = JavaEmbedUtils.rubyToJava( ruby.evalScriptlet( sourceCode ) );
					return r;
				}
				catch( EvalFailedException x )
				{
					throw new ExecutableInitializationException( "", x.getMessage(), x );
				}
			}
			/*
			 * catch( Throwable x ) { throw new ExecutionException(
			 * x.getMessage(), x ); }
			 */
		}

		public String getSourceCode()
		{
			return sourceCode;
		}

		private final String sourceCode;

		private Script script;
	}
}
