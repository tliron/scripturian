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

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.script.ScriptEngine;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;

/**
 * A {@link LanguageAdapter} that supports the JavaScript language as
 * implemented by <a href="http://www.mozilla.org/rhino/">Rhino</a> via its
 * JSR-223 scripting engine.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"rhino-nonjdk", "js", "javascript", "ecmascript"
})
public class RhinoAdapter extends Jsr223LanguageAdapter
{
	//
	// Construction
	//

	public RhinoAdapter() throws LanguageAdapterException
	{
		try
		{
			wrappedExceptionClass = getClass().getClassLoader().loadClass( "org.mozilla.javascript.WrappedException" );
			wrappedExceptionGetWrappedExceptionMethod = wrappedExceptionClass.getMethod( "getWrappedException" );
			rhinoExceptionClass = getClass().getClassLoader().loadClass( "org.mozilla.javascript.RhinoException" );
			rhinoExceptionDetailsMethod = rhinoExceptionClass.getMethod( "details" );
			rhinoExceptionLineNumberMethod = rhinoExceptionClass.getMethod( "lineNumber" );
			rhinoExceptionColumnNumberMethod = rhinoExceptionClass.getMethod( "columnNumber" );
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

	@Override
	public void beforeCall( ScriptEngine scriptEngine, ExecutionContext executionContext )
	{
		// Rhino expects a PrintWriter.
		if( !( executionContext.getWriter() instanceof PrintWriter ) )
			executionContext.setWriter( new PrintWriter( executionContext.getWriter(), true ) );
	}

	@Override
	public String getScriptletHeader( Executable executable, ScriptEngine scriptEngine )
	{
		// Rhino's default implementation of print() is annoyingly a println().
		// This will fix it.
		return "print=function(s){context.writer.print(String(s));context.writer.flush();};";
	}

	@Override
	public String getTextAsProgram( Executable executable, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\'", "\\\\'" );
		return "print('" + content + "');";
	}

	@Override
	public String getExpressionAsProgram( Executable executable, ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	@Override
	public String getExpressionAsInclude( Executable executable, ScriptEngine scriptEngine, String content )
	{
		return executable.getExposedExecutableName() + ".container.includeDocument(" + content + ");";
	}

	@Override
	public Throwable getCauseOrExecutionException( String documentName, Throwable throwable )
	{
		if( wrappedExceptionClass.isInstance( throwable ) )
		{
			// Unwrap
			try
			{
				return (Throwable) wrappedExceptionGetWrappedExceptionMethod.invoke( throwable );
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
		else if( rhinoExceptionClass.isInstance( throwable ) )
		{
			try
			{
				String details = (String) rhinoExceptionDetailsMethod.invoke( throwable );
				int lineNumber = (Integer) rhinoExceptionLineNumberMethod.invoke( throwable );
				int columnNumber = (Integer) rhinoExceptionColumnNumberMethod.invoke( throwable );
				return new ExecutionException( documentName, lineNumber, columnNumber, details );
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

	private final Class<?> wrappedExceptionClass;

	private final Method wrappedExceptionGetWrappedExceptionMethod;

	private final Class<?> rhinoExceptionClass;

	private final Method rhinoExceptionDetailsMethod;

	private final Method rhinoExceptionLineNumberMethod;

	private final Method rhinoExceptionColumnNumberMethod;
}
