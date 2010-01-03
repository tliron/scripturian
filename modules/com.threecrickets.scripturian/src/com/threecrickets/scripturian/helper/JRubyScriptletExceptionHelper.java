/**
 * Copyright 2009 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://www.threecrickets.com/
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
public class JRubyScriptletExceptionHelper implements ScriptletExceptionHelper
{
	//
	// Construction
	//

	public JRubyScriptletExceptionHelper() throws ClassNotFoundException, SecurityException, NoSuchFieldException, NoSuchMethodException
	{
		raiseExceptionClass = getClass().getClassLoader().loadClass( "org.jruby.exceptions.RaiseException" );
		raiseExceptionGetExceptionMethod = raiseExceptionClass.getMethod( "getException" );
		nativeExceptionClass = getClass().getClassLoader().loadClass( "org.jruby.NativeException" );
		nativeExceptionGetCauseMethod = nativeExceptionClass.getMethod( "getCause" );
	}

	//
	// ScriptletExceptionHelper
	//

	public DocumentRunException getDocumentRunException( String documentName, Exception exception )
	{
		if( exception instanceof ScriptException )
		{
			Throwable cause = exception.getCause();
			if( raiseExceptionClass.isInstance( cause ) )
			{
				try
				{
					Object rubyException = raiseExceptionGetExceptionMethod.invoke( cause );
					if( nativeExceptionClass.isInstance( rubyException ) )
					{
						cause = (Throwable) nativeExceptionGetCauseMethod.invoke( rubyException );
						if( cause instanceof DocumentRunException )
							return (DocumentRunException) cause;
					}
					else
					{
						// Wish there were a way to get line numbers from
						// RubyException!
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

	private final Class<?> raiseExceptionClass;

	private final Method raiseExceptionGetExceptionMethod;

	private final Class<?> nativeExceptionClass;

	private final Method nativeExceptionGetCauseMethod;
}
