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

package com.threecrickets.scripturian.helper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.threecrickets.scripturian.Document;
import com.threecrickets.scripturian.DocumentContext;
import com.threecrickets.scripturian.ScriptletHelper;
import com.threecrickets.scripturian.annotation.ScriptEngines;

/**
 * An {@link ScriptletHelper} that supports the Ruby scripting language as
 * implemented by <a href="http://jruby.codehaus.org/">JRuby</a>.
 * <p>
 * Note that JRuby internally embeds each script in a "main" object, so that
 * methods defined therein cannot be accessible to us after the script runs,
 * unless they are explicitly stored in global variables. For this reason, in
 * order to make your methods invocable, they must be stored as closures in
 * global variables of the same name as the entry point. As so:
 * 
 * <pre>
 *  def myentry value
 *  	print value*3
 *  end
 *  $myentry = method :myentry
 * </pre>
 * 
 * Or even store raw lambdas:
 * 
 * <pre>
 *  $myentry = lambda do |value|
 *  	print value*3
 *  end
 * </pre>
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"jruby", "ruby"
})
public class JRubyScriptletHelper extends ScriptletHelper
{
	//
	// Construction
	//

	public JRubyScriptletHelper() throws ClassNotFoundException, SecurityException, NoSuchFieldException, NoSuchMethodException
	{
		raiseExceptionClass = getClass().getClassLoader().loadClass( "org.jruby.exceptions.RaiseException" );
		raiseExceptionGetExceptionMethod = raiseExceptionClass.getMethod( "getException" );
		nativeExceptionClass = getClass().getClassLoader().loadClass( "org.jruby.NativeException" );
		nativeExceptionGetCauseMethod = nativeExceptionClass.getMethod( "getCause" );
	}

	//
	// ScriptletHelper
	//

	@Override
	public void beforeCall( ScriptEngine scriptEngine, DocumentContext documentContext )
	{
		scriptEngine.setContext( documentContext.getScriptContext() );

		// Move global vars to instance vars
		StringBuilder r = new StringBuilder();
		for( String var : documentContext.getVariableNames() )
		{
			r.append( '@' );
			r.append( var );
			r.append( "=$" );
			r.append( var );
			r.append( ';' );
		}

		try
		{
			scriptEngine.eval( r.toString() );
		}
		catch( ScriptException x )
		{
		}
	}

	@Override
	public String getTextAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "print(\"" + content + "\");";
	}

	@Override
	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	@Override
	public String getExpressionAsInclude( Document document, ScriptEngine scriptEngine, String content )
	{
		// return "require $" + document.getDocumentVariableName() +
		// ".container.source.basePath.toString + '/' + " + content + ";";
		return "$" + document.getDocumentVariableName() + ".container.include_document(" + content + ");";
	}

	@Override
	public String getInvocationAsProgram( Document document, ScriptEngine scriptEngine, String content )
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
	public Throwable getCauseOrDocumentRunException( String documentName, Throwable throwable )
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
