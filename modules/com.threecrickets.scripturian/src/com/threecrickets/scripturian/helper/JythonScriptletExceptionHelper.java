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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.script.ScriptException;

import com.threecrickets.scripturian.ScriptletExceptionHelper;
import com.threecrickets.scripturian.exception.DocumentRunException;

/**
 * @author Tal Liron
 */
public class JythonScriptletExceptionHelper implements ScriptletExceptionHelper
{
	//
	// Construction
	//

	public JythonScriptletExceptionHelper() throws ClassNotFoundException, SecurityException, NoSuchFieldException, NoSuchMethodException
	{
		pyExceptionClass = getClass().getClassLoader().loadClass( "org.python.core.PyException" );
		pyExceptionValueField = pyExceptionClass.getField( "value" );
		pyObjectClass = getClass().getClassLoader().loadClass( "org.python.core.PyObject" );
		pyObjectToJavaMethod = pyObjectClass.getMethod( "__tojava__", Class.class );
	}

	//
	// ScriptletExceptionHelper
	//

	public DocumentRunException getDocumentRunException( String documentName, Exception exception )
	{
		if( exception instanceof ScriptException )
		{
			Throwable cause = exception.getCause();

			if( pyExceptionClass.isInstance( cause ) )
			{
				try
				{
					// Returns a PyObject
					Object value = pyExceptionValueField.get( cause );
					if( value != null )
					{
						// Might return Py.NoConversion
						Object toJava = pyObjectToJavaMethod.invoke( value, DocumentRunException.class );
						if( toJava instanceof DocumentRunException )
							return (DocumentRunException) toJava;
					}
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

	private final Class<?> pyExceptionClass;

	private final Field pyExceptionValueField;

	private final Class<?> pyObjectClass;

	private final Method pyObjectToJavaMethod;
}
