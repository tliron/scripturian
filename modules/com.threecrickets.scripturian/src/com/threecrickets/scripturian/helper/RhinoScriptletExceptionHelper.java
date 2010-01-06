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

import javax.script.ScriptException;

import com.threecrickets.scripturian.ScriptletExceptionHelper;
import com.threecrickets.scripturian.exception.DocumentRunException;

/**
 * @author Tal Liron
 */
public class RhinoScriptletExceptionHelper implements ScriptletExceptionHelper
{
	//
	// Construction
	//

	public RhinoScriptletExceptionHelper() throws ClassNotFoundException, SecurityException, NoSuchMethodException
	{
		wrappedExceptionClass = getClass().getClassLoader().loadClass( "org.mozilla.javascript.WrappedException" );
		wrappedExceptionGetWrappedExceptionMethod = wrappedExceptionClass.getMethod( "getWrappedException" );
		rhinoExceptionClass = getClass().getClassLoader().loadClass( "org.mozilla.javascript.RhinoException" );
		rhinoExceptionDetailsMethod = rhinoExceptionClass.getMethod( "details" );
		rhinoExceptionLineNumberMethod = rhinoExceptionClass.getMethod( "lineNumber" );
		rhinoExceptionColumnNumberMethod = rhinoExceptionClass.getMethod( "columnNumber" );
	}

	//
	// ScriptletExceptionHelper
	//

	public DocumentRunException getDocumentRunException( String documentName, Exception exception )
	{
		if( exception instanceof ScriptException )
		{
			Throwable cause = exception.getCause();

			if( wrappedExceptionClass.isInstance( cause ) )
			{
				// Unwrap
				try
				{
					cause = (Throwable) wrappedExceptionGetWrappedExceptionMethod.invoke( cause );
					if( cause instanceof DocumentRunException )
						return (DocumentRunException) cause;
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

			if( rhinoExceptionClass.isInstance( cause ) )
			{
				try
				{
					String details = (String) rhinoExceptionDetailsMethod.invoke( cause );
					int lineNumber = (Integer) rhinoExceptionLineNumberMethod.invoke( cause );
					int columnNumber = (Integer) rhinoExceptionColumnNumberMethod.invoke( cause );
					return new DocumentRunException( documentName, details, lineNumber, columnNumber );
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
