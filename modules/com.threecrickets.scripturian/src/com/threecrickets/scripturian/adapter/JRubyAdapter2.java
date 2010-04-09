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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
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
public class JRubyAdapter2 implements LanguageAdapter
{
	//
	// Construction
	//

	public JRubyAdapter2() throws LanguageInitializationException
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
		ScriptingContainer scriptingContainer = getScriptingContainer( executionContext, null );
		Object r = scriptingContainer.callMethod( null, method );
		// System.out.println( r );
		return r;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final String SCRIPTING_CONTAINER = "jruby.scriptingContainer";

	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	private final ReentrantLock lock = new ReentrantLock();

	private static ScriptingContainer createScriptingContainer()
	{
		return new ScriptingContainer( LocalContextScope.THREADSAFE, LocalVariableBehavior.TRANSIENT );
	}

	private static ScriptingContainer getScriptingContainer( ExecutionContext executionContext, JRubyScriptlet scriptlet )
	{
		ScriptingContainer scriptingContainer;
		if( scriptlet != null && scriptlet.embedEvalUnit != null )
		{
			scriptingContainer = scriptlet.embedEvalUnitScriptingContainer;
		}
		else
		{
			scriptingContainer = (ScriptingContainer) executionContext.getAttributes().get( SCRIPTING_CONTAINER );
			if( scriptingContainer == null )
			{
				scriptingContainer = createScriptingContainer();
				executionContext.getAttributes().put( SCRIPTING_CONTAINER, scriptingContainer );
			}
		}

		return scriptingContainer;
	}

	private class JRubyScriptlet implements Scriptlet
	{
		public JRubyScriptlet( String script )
		{
			this.script = script;
		}

		public void compile() throws CompilationException
		{
			try
			{
				embedEvalUnitScriptingContainer = createScriptingContainer();
				// embedEvalUnit = embedEvalUnitScriptingContainer.parse( script
				// );
			}
			catch( EvalFailedException x )
			{
				throw new CompilationException( "", x.getMessage(), x );
			}
		}

		public Object execute( ExecutionContext executionContext ) throws ExecutableInitializationException, ExecutionException
		{
			ScriptingContainer scriptingContainer = getScriptingContainer( executionContext, this );

			scriptingContainer.setWriter( executionContext.getWriter() );
			scriptingContainer.setErrorWriter( executionContext.getErrorWriter() );

			for( Map.Entry<String, Object> entry : executionContext.getExposedVariables().entrySet() )
				scriptingContainer.put( "$" + entry.getKey(), entry.getValue() );

			// scriptingContainer.getVarMap().putAll(
			// executionContext.getExposedVariables() );

			// scriptingContainer.getVarMap().getVariableInterceptor().inject(
			// scriptingContainer.getVarMap(), scriptingContainer.getRuntime(),
			// embedEvalUnit.getScope(), 0, receiver );

			if( embedEvalUnit != null )
			{
				// scriptingContainer.getProvider().getRuntime().getCurrentContext().pushScope(
				// embedEvalUnit.getScope() );
				return JavaEmbedUtils.rubyToJava( embedEvalUnit.run() );
			}
			else
			{
				try
				{
					Object r = scriptingContainer.runScriptlet( script );
					// System.out.println( r );
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
			return script;
		}

		private final String script;

		private EmbedEvalUnit embedEvalUnit;

		private ScriptingContainer embedEvalUnitScriptingContainer;
	}
}
