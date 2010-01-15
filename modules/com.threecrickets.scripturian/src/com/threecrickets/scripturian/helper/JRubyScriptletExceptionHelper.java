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

import com.threecrickets.scripturian.ScriptletExceptionHelper;

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
