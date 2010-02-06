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

import com.threecrickets.scripturian.Document;
import com.threecrickets.scripturian.ScriptletHelper;
import com.threecrickets.scripturian.annotation.ScriptEnginePriorityExtensions;
import com.threecrickets.scripturian.annotation.ScriptEngines;
import com.threecrickets.scripturian.exception.DocumentRunException;
import com.threecrickets.scripturian.exception.StackFrame;

/**
 * An {@link ScriptletHelper} that supports the JavaScript scripting
 * language as implemented by <a href="http://www.mozilla.org/rhino/">Rhino</a>.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"rhino-nonjdk", "rhino", "js", "javascript", "JavaScript", "ecmascript", "ECMAScript"
})
@ScriptEnginePriorityExtensions(
{
	"js"
})
public class RhinoScriptletHelper extends ScriptletHelper
{
	//
	// Construction
	//

	public RhinoScriptletHelper() throws ClassNotFoundException, SecurityException, NoSuchMethodException
	{
		wrappedExceptionClass = getClass().getClassLoader().loadClass( "org.mozilla.javascript.WrappedException" );
		wrappedExceptionGetWrappedExceptionMethod = wrappedExceptionClass.getMethod( "getWrappedException" );
		rhinoExceptionClass = getClass().getClassLoader().loadClass( "org.mozilla.javascript.RhinoException" );
		rhinoExceptionDetailsMethod = rhinoExceptionClass.getMethod( "details" );
		rhinoExceptionLineNumberMethod = rhinoExceptionClass.getMethod( "lineNumber" );
		rhinoExceptionColumnNumberMethod = rhinoExceptionClass.getMethod( "columnNumber" );
	}

	//
	// ScriptletHelper
	//

	@Override
	public String getScriptletHeader( Document document, ScriptEngine scriptEngine )
	{
		// Rhino's default implementation of print() is annoyingly a println().
		// This will fix it.
		return "print=function(s){context.writer.print(String(s));context.writer.flush();};";
	}

	@Override
	public String getTextAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\'", "\\\\'" );
		return "print('" + content + "');";
	}

	@Override
	public String getExpressionAsProgram( Document document, ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	@Override
	public String getExpressionAsInclude( Document document, ScriptEngine scriptEngine, String content )
	{
		return document.getDocumentVariableName() + ".container.includeDocument(" + content + ");";
	}

	@Override
	public Throwable getCauseOrDocumentRunException( String documentName, Throwable throwable )
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
				return new DocumentRunException( details, new StackFrame( documentName, lineNumber, columnNumber ) );
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
