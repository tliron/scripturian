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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageInitializationException;

/**
 * An {@link LanguageAdapter} that supports the Python scripting language as
 * implemented by <a href="http://www.jython.org/">Jython</a>.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"jython", "python"
})
public class JythonAdapter extends Jsr223LanguageAdapter
{
	//
	// Construction
	//

	public JythonAdapter() throws LanguageInitializationException
	{
		try
		{
			pyExceptionClass = getClass().getClassLoader().loadClass( "org.python.core.PyException" );
			pyExceptionValueField = pyExceptionClass.getField( "value" );
			pyBaseExceptionClass = getClass().getClassLoader().loadClass( "org.python.core.PyBaseException" );
			pyObjectClass = getClass().getClassLoader().loadClass( "org.python.core.PyObject" );
			pyObjectToJavaMethod = pyObjectClass.getMethod( "__tojava__", Class.class );
		}
		catch( ClassNotFoundException x )
		{
			throw new LanguageInitializationException( getClass(), x );
		}
		catch( SecurityException x )
		{
			throw new LanguageInitializationException( getClass(), x );
		}
		catch( NoSuchMethodException x )
		{
			throw new LanguageInitializationException( getClass(), x );
		}
		catch( NoSuchFieldException x )
		{
			throw new LanguageInitializationException( getClass(), x );
		}
	}

	//
	// ScriptletHelper
	//

	@Override
	public String getScriptletHeader( Executable document, ScriptEngine scriptEngine )
	{
		String version = scriptEngine.getFactory().getEngineVersion();
		String[] split = version.split( "\\." );
		int major = Integer.parseInt( split[0] );
		int minor = Integer.parseInt( split[1] );
		if( ( major >= 2 ) && ( minor >= 5 ) )
			return "import sys;";
		else
			// Apparently the Java Scripting support for Jython (version 2.2.1)
			// does not correctly redirect stdout and stderr. Luckily, the
			// Python interface is compatible with Java's Writer interface, so
			// we can redirect them explicitly.
			return "import sys;sys.stdout=context.writer;sys.stderr=context.errorWriter;";
	}

	@Override
	public String getTextAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "sys.stdout.write(\"" + content + "\"),;";
	}

	@Override
	public String getExpressionAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		return "sys.stdout.write(" + content + ");";
	}

	@Override
	public String getExpressionAsInclude( Executable document, ScriptEngine scriptEngine, String content )
	{
		return document.getExecutableVariableName() + ".container.includeDocument(" + content + ");";
	}

	@Override
	public Throwable getCauseOrDocumentRunException( String documentName, Throwable throwable )
	{
		Throwable root = getRoot( throwable );
		if( root != null )
			return root;

		if( throwable instanceof ScriptException )
		{
			Throwable cause = throwable.getCause();
			root = getRoot( cause );
			if( root != null )
				return root;

			// A wrapped Jython exception -- terrific, because Jython does a
			// great job at returning a complete ScriptException
			return new ExecutionException( documentName, (ScriptException) throwable );
		}

		return null;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final Class<?> pyExceptionClass;

	private final Field pyExceptionValueField;

	private final Class<?> pyBaseExceptionClass;

	private final Class<?> pyObjectClass;

	private final Method pyObjectToJavaMethod;

	/*
	 * private DocumentRunException getDocumentRunexception( PyTraceback tb ) {
	 * DocumentRunException next = tb.tb_next != null ? getDocumentRunexception(
	 * (PyTraceback) tb.tb_next ) : null; return new DocumentRunException(
	 * tb.tb_frame.f_code.co_filename != null ? tb.tb_frame.f_code.co_filename :
	 * "", "", tb.tb_lineno, -1, next ); }
	 */

	private Throwable getRoot( Throwable throwable )
	{
		if( pyExceptionClass.isInstance( throwable ) )
		{
			try
			{
				// Returns a PyObject
				Object value = pyExceptionValueField.get( throwable );
				if( value != null )
				{
					if( pyBaseExceptionClass.isInstance( value ) )
					{
						// PyException e = (PyException) throwable;
						// PyTraceback tb = e.traceback;
						// return getDocumentRunexception( tb );
					}

					// Might return Py.NoConversion
					Object toJava = pyObjectToJavaMethod.invoke( value, Throwable.class );
					if( toJava instanceof Throwable )
						// A native exception
						return (Throwable) toJava;
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

		return null;
	}
}
