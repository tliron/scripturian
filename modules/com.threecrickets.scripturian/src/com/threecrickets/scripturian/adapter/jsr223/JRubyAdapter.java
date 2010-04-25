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

package com.threecrickets.scripturian.adapter.jsr223;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.exception.LanguageAdapterException;

/**
 * A {@link LanguageAdapter} that supports the Ruby language as implemented by
 * <a href="http://jruby.codehaus.org/">JRuby</a> via its JSR-223 scripting
 * engine.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"jruby", "ruby"
})
public class JRubyAdapter extends Jsr223LanguageAdapter
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @throws LanguageAdapterException
	 */
	public JRubyAdapter() throws LanguageAdapterException
	{
		try
		{
			raiseExceptionClass = getClass().getClassLoader().loadClass( "org.jruby.exceptions.RaiseException" );
			raiseExceptionGetExceptionMethod = raiseExceptionClass.getMethod( "getException" );
			nativeExceptionClass = getClass().getClassLoader().loadClass( "org.jruby.NativeException" );
			nativeExceptionGetCauseMethod = nativeExceptionClass.getMethod( "getCause" );
		}
		catch( ClassNotFoundException x )
		{
			throw new LanguageAdapterException( getClass(), x );
		}
		catch( SecurityException x )
		{
			throw new LanguageAdapterException( getClass(), x );
		}
		catch( NoSuchMethodException x )
		{
			throw new LanguageAdapterException( getClass(), x );
		}
	}

	//
	// Jsr223LanguageAdapter
	//

	/*
	 * @Override public boolean isCompilable() { return false; }
	 */

	@Override
	public void beforeCall( ScriptEngine scriptEngine, ExecutionContext executionContext )
	{
		ScriptContext scriptContext = getScriptContext( executionContext );
		scriptEngine.setContext( scriptContext );

		// Move global vars to instance vars
		StringBuilder r = new StringBuilder();
		for( String var : executionContext.getExposedVariables().keySet() )
		{
			r.append( '@' );
			r.append( var );
			r.append( "=$" );
			r.append( var );
			r.append( ';' );
		}

		try
		{
			scriptEngine.eval( r.toString(), scriptContext );
		}
		catch( ScriptException x )
		{
		}
	}

	@Override
	public String getSourceCodeForLiteralOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "print(\"" + content + "\");";
	}

	@Override
	public String getSourceCodeForExpressionOutput( Executable executable, ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	@Override
	public String getSourceCodeForExpressionInclude( Executable executable, ScriptEngine scriptEngine, String content )
	{
		// return "require $" + executable.getDocumentVariableName() +
		// ".container.source.basePath.toString + '/' + " + content + ";";
		return "$" + executable.getExposedExecutableName() + ".container.include_executable(" + content + ");";
	}

	@Override
	public String getInvocationAsProgram( Executable executable, ScriptEngine scriptEngine, String content, Object... arguments )
	{
		/*
		 * String version = scriptEngine.getFactory().getEngineVersion();
		 * String[] split = version.split( "\\." ); int major = split.length >=
		 * 1 ? Integer.parseInt( split[0] ) : 0; int minor = split.length >= 2 ?
		 * Integer.parseInt( split[1] ) : 0; int revision = split.length >= 3 ?
		 * Integer.parseInt( split[2] ) : 0;
		 */

		return null;
		// return content + "();";
		// else
		// return "$" + content + ".call;";
	}

	@Override
	public Throwable getCauseOrExecutionException( String documentName, Throwable throwable )
	{
		if( raiseExceptionClass.isInstance( throwable ) )
		{
			try
			{
				Object rubyException = raiseExceptionGetExceptionMethod.invoke( throwable );
				if( nativeExceptionClass.isInstance( rubyException ) )
					return (Throwable) nativeExceptionGetCauseMethod.invoke( rubyException );

				// Wish there were a way to get line numbers from
				// RubyException!
			}
			catch( IllegalArgumentException x )
			{
				x.printStackTrace();
			}
			catch( IllegalAccessException x )
			{
				x.printStackTrace();
			}
			catch( InvocationTargetException x )
			{
				x.printStackTrace();
			}
		}

		return null;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final Class<?> raiseExceptionClass;

	private final Method raiseExceptionGetExceptionMethod;

	private final Class<?> nativeExceptionClass;

	private final Method nativeExceptionGetCauseMethod;
}
