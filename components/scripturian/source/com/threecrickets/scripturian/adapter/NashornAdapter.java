/**
 * Copyright 2009-2013 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.adapter;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;

import jdk.nashorn.api.scripting.NashornException;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.objects.NativeArray;
import jdk.nashorn.internal.objects.NativeJava;
import jdk.nashorn.internal.runtime.Context;
import jdk.nashorn.internal.runtime.ErrorManager;
import jdk.nashorn.internal.runtime.ScriptFunction;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.ScriptRuntime;
import jdk.nashorn.internal.runtime.Source;
import jdk.nashorn.internal.runtime.Version;
import jdk.nashorn.internal.runtime.options.Options;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.internal.ScripturianUtil;
import com.threecrickets.scripturian.internal.SwitchableWriter;

/**
 * A {@link LanguageAdapter} that supports the JavaScript language as
 * implemented by <a
 * href="http://openjdk.java.net/projects/nashorn/">Nashorn</a>.
 * 
 * @author Tal Liron
 */
public class NashornAdapter extends LanguageAdapterBase
{
	//
	// Constant
	//

	/**
	 * The Nashorn context attribute.
	 */
	public static final String NASHORN_CONTEXT = "nashorn.context";

	/**
	 * The Nashorn global scope attribute.
	 */
	public static final String NASHORN_GLOBAL_SCOPE = "nashorn.globalScope";

	/**
	 * The switchable standard output attribute for the Nashorn context.
	 */
	public static final String NASHORN_OUT = "nashorn.out";

	/**
	 * The switchable standard error attribute for the Nashorn context.
	 */
	public static final String NASHORN_ERR = "nashorn.err";

	/**
	 * The default base directory for cached executables.
	 */
	public static final String NASHORN_CACHE_DIR = "javascript";

	//
	// Static operations
	//

	/**
	 * Creates an execution exception.
	 * 
	 * @param documentName
	 *        The document name
	 * @param x
	 *        The exception
	 * @return The execution exception
	 */
	public static ExecutionException createExecutionException( String documentName, NashornException x )
	{
		if( x.getCause() != null )
			return new ExecutionException( documentName, x.getCause() );
		else
			return new ExecutionException( documentName, x );
	}

	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 */
	public NashornAdapter() throws LanguageAdapterException
	{
		super( "Nashorn", Version.version(), "JavaScript", "", Arrays.asList( "js", "javascript", "nashorn" ), "js", Arrays.asList( "javascript", "js", "nashorn" ), "nashorn" );
	}

	//
	// Attributes
	//

	/**
	 * Gets the Nashorn context associated with the execution context, creating
	 * it if it doesn't exist. Each execution context is guaranteed to have its
	 * own Nashorn context. The globals instance is updated to match the writers
	 * and services in the execution context.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The Nashorn context
	 */
	public Context getContext( ExecutionContext executionContext )
	{
		Context context = (Context) executionContext.getAttributes().get( NASHORN_CONTEXT );
		SwitchableWriter switchableOut = (SwitchableWriter) executionContext.getAttributes().get( NASHORN_OUT );
		SwitchableWriter switchableErr = (SwitchableWriter) executionContext.getAttributes().get( NASHORN_ERR );

		if( context == null )
		{
			switchableOut = new SwitchableWriter( executionContext.getWriterOrDefault() );
			switchableErr = new SwitchableWriter( executionContext.getErrorWriterOrDefault() );

			PrintWriter out = new PrintWriter( switchableOut, true );
			PrintWriter err = new PrintWriter( switchableErr, true );

			Options options = new Options( "nashorn", err );
			options.set( "print.no.newline", true );
			ErrorManager errors = new ErrorManager( err );

			context = new Context( options, errors, out, err, Thread.currentThread().getContextClassLoader() );

			executionContext.getAttributes().put( NASHORN_CONTEXT, context );
			executionContext.getAttributes().put( NASHORN_OUT, switchableOut );
			executionContext.getAttributes().put( NASHORN_ERR, switchableErr );
		}
		else
		{
			context.getOut().flush();
			context.getErr().flush();

			// Our switchable writer lets us change the Nashorn context's
			// standard output/error after it's been created.

			switchableOut.use( executionContext.getWriterOrDefault() );
			switchableErr.use( executionContext.getErrorWriterOrDefault() );
		}

		return context;
	}

	public ScriptObject getGlobalScope( Executable executable, ExecutionContext executionContext, Context context )
	{
		ScriptObject globalScope = (ScriptObject) executionContext.getAttributes().get( NASHORN_GLOBAL_SCOPE );

		boolean init = false;
		if( globalScope == null )
		{
			globalScope = context.createGlobal();
			executionContext.getAttributes().put( NASHORN_GLOBAL_SCOPE, globalScope );
			init = true;
		}

		// Define services as properties in scope
		( (ScriptObjectMirror) ScriptObjectMirror.wrap( globalScope, globalScope ) ).putAll( executionContext.getServices() );
		// globalScope.putAll( executionContext.getServices() );

		Context.setGlobal( globalScope );

		if( init )
		{
			ScriptFunction script = context.compileScript( new Source( getClass().getCanonicalName() + ".getGlobalScope", INIT_SOURCE ), globalScope );
			ScriptRuntime.apply( script, globalScope );
		}

		return globalScope;
	}

	/**
	 * The base directory for cached executables.
	 * 
	 * @return The cache directory
	 */
	public File getCacheDir()
	{
		return new File( LanguageManager.getCachePath(), NASHORN_CACHE_DIR );
	}

	//
	// LanguageAdapter
	//

	@Override
	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = ScripturianUtil.doubleQuotedLiteral( literal );
		return "print(" + literal + ");";
	}

	@Override
	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "print(" + expression + ");";
	}

	@Override
	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		String containerIncludeCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_COMMAND_ATTRIBUTE );
		return executable.getExecutableServiceName() + ".container." + containerIncludeCommand + "(" + expression + ");";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new NashornProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	@Override
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		Context context = getContext( executionContext );
		ScriptObject globalScope = getGlobalScope( executable, executionContext, context );

		Object entryPoint = globalScope.get( entryPointName );
		if( !( entryPoint instanceof ScriptFunction ) )
			throw new NoSuchMethodException( entryPointName );

		ScriptFunction function = (ScriptFunction) entryPoint;
		try
		{
			Object r = ScriptRuntime.apply( function, null, arguments );
			if( r instanceof NativeArray )
				r = NativeJava.to( null, r, "java.util.List" );
			return r;
		}
		catch( NashornException x )
		{
			throw createExecutionException( executable.getDocumentName(), x );
		}
		catch( ClassNotFoundException x )
		{
			throw new ExecutionException( executable.getDocumentName(), x );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final String MOZILLA_COMPAT_SOURCE = "load('nashorn:mozilla_compat.js')";

	private static final String PRINTLN_SOURCE = "function println(s){if(undefined!==s){print(s)};if(undefined===println.separator){println.separator=String(java.lang.System.getProperty('line.separator'))}print(println.separator)}";

	private static final String IMPORT_CLASS_SOURCE = "Object.defineProperty(this,'importClass',{configurable:true,enumerable:false,writable:true,value:function(){for(var a in arguments){var clazz=arguments[a];if(Java.isType(clazz)){var className=Java.typeName(clazz);var simpleName=className.substring(className.lastIndexOf('.')+1);this[simpleName]=clazz}else{throw new TypeError(clazz + ' is not a Java class')}}}})";

	private static final String INIT_SOURCE = MOZILLA_COMPAT_SOURCE + ";" + PRINTLN_SOURCE + ";" + IMPORT_CLASS_SOURCE;
}
