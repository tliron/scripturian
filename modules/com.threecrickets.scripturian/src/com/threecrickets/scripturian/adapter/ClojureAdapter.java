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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import clojure.lang.Compiler;
import clojure.lang.Namespace;
import clojure.lang.PersistentVector;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import clojure.lang.Compiler.CompilerException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.StackFrame;

/**
 * A {@link LanguageAdapter} that supports the <a
 * href="http://clojure.org/">Clojure</a> language.
 * <p>
 * Note: Clojure adapters in all {@link LanguageManager} will share the same
 * runtime instance of Clojure, which is a JVM-wide singleton.
 * <p>
 * This is important! Even though every execution context gets its own Clojure
 * namespace, all namespaces exist in the same runtime. If one execution context
 * loads code into a namespace, all execution contexts will be able to access
 * it. This means that two contexts cannot use different versions of a library
 * at the same time if the versions have the same namespace.
 * <p>
 * Note that {@link Program#prepare()} does not actually compile the source
 * code, but it does parse it, making it quicker to execute later.
 * 
 * @author Tal Liron
 */
public class ClojureAdapter extends LanguageAdapterBase
{
	//
	// Constants
	//

	/**
	 * Namespace attribute.
	 */
	public static final String CLOJURE_NAMESPACE = "clojure.namespace";

	/**
	 * Prefix prepended to all namespace names created by this adapter.
	 */
	public static final String NAMESPACE_PREFIX = "scripturian";

	//
	// Static operations
	//

	/**
	 * Creates an execution exception with a full stack.
	 * 
	 * @param startLineNumber
	 *        The line number in the document for where the program's source
	 *        code begins
	 * @param x
	 *        The Clojure compiler exception
	 * @return The execution exception
	 */
	public static ExecutionException createExecutionException( int startLineNumber, CompilerException x )
	{
		ArrayList<StackFrame> stack = new ArrayList<StackFrame>();
		Throwable cause = x.getCause();
		String message = cause != null && cause.getMessage() != null ? cause.getMessage() : x.getMessage();

		if( cause instanceof ExecutionException )
			// Add the cause's stack to ours
			stack.addAll( ( (ExecutionException) cause ).getStack() );
		else if( cause instanceof ParsingException )
			// Add the cause's stack to ours
			stack.addAll( ( (ParsingException) cause ).getStack() );
		else
			message = extractExceptionStackFromMessage( message, stack );

		if( !stack.isEmpty() )
		{
			ExecutionException executionException = new ExecutionException( message, x );
			executionException.getStack().addAll( stack );
			return executionException;
		}
		else
			return new ExecutionException( (String) Compiler.SOURCE.deref(), startLineNumber + (Integer) Compiler.LINE.deref(), -1, x );
	}

	/**
	 * Annoyingly, Clojure's CompilerException accepts stack information
	 * (Compiler.SOURCE, Compiler.LINE) in its constructor, but does not store
	 * it directly. This information is indirectly available to us in the error
	 * message. We'll just have to parse this error message to get our valuable
	 * data!
	 * 
	 * @param message
	 *        The exception message
	 * @param stack
	 *        The stack
	 * @return New exception message
	 */
	public static String extractExceptionStackFromMessage( String message, Collection<StackFrame> stack )
	{
		int length = message.length();
		if( length > 0 )
		{
			if( message.charAt( length - 1 ) == ')' )
			{
				int lastParens1 = message.lastIndexOf( '(' );
				if( lastParens1 != -1 )
				{
					String stackFrame = message.substring( lastParens1 + 1, message.length() - 1 );
					String[] split = stackFrame.split( ":" );
					if( split.length == 2 )
					{
						String documentName = split[0];
						try
						{
							int lineNumber = Integer.parseInt( split[1] );

							// System.out.println( message );
							stack.add( new StackFrame( documentName, lineNumber, -1 ) );

							message = message.substring( 0, lastParens1 ).trim();
							// return extractStack( message, stack );
						}
						catch( NumberFormatException x )
						{
						}
					}
				}
			}
		}

		return message;
	}

	/**
	 * Gets a Clojure namespace stored in the execution context, creating it if
	 * it doesn't exist. Each execution context is guaranteed to have its own
	 * namespace.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The Clojure namespace
	 */
	public static Namespace getClojureNamespace( ExecutionContext executionContext )
	{
		Namespace ns = (Namespace) executionContext.getAttributes().get( CLOJURE_NAMESPACE );
		if( ns == null )
		{
			// We need to create a fresh namespace for each execution context.

			String name = NAMESPACE_PREFIX + namespaceCounter.getAndIncrement();
			ns = Namespace.findOrCreate( Symbol.intern( name ) );
			executionContext.getAttributes().put( CLOJURE_NAMESPACE, ns );
		}

		// Expose our variables by interning them in the namespace
		// for( Map.Entry<String, Object> entry :
		// executionContext.getExposedVariables().entrySet() )
		// Var.intern( ns, Symbol.intern( entry.getKey() ), entry.getValue() );

		return ns;
	}

	/**
	 * From somethingLikeThis to something-like-this.
	 * 
	 * @param camelCase
	 *        somethingLikeThis
	 * @return something-like-this
	 */
	public static String toClojureStyle( String camelCase )
	{
		StringBuilder r = new StringBuilder();
		char c = camelCase.charAt( 0 );
		if( Character.isUpperCase( c ) )
			r.append( Character.toLowerCase( c ) );
		else
			r.append( c );
		for( int i = 1; i < camelCase.length(); i++ )
		{
			c = camelCase.charAt( i );
			if( Character.isUpperCase( c ) )
			{
				r.append( '-' );
				r.append( Character.toLowerCase( c ) );
			}
			else
				r.append( c );
		}
		return r.toString();
	}

	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @throws LanguageAdapterException
	 */
	public ClojureAdapter() throws LanguageAdapterException
	{
		super( "Clojure", "", null, null, Arrays.asList( "clj" ), null, Arrays.asList( "clojure", "clj" ), null );
	}

	//
	// LanguageAdapter
	//

	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = literal.replaceAll( "\\n", "\\\\n" );
		literal = literal.replaceAll( "\\\"", "\\\\\"" );
		return "(print \"" + literal + "\")";
	}

	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "(print " + expression + ")";
	}

	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		String containerIncludeExpressionCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_EXPRESSION_COMMAND );
		return "(.. " + executable.getExposedExecutableName() + " getContainer (" + containerIncludeExpressionCommand + " " + expression + "))";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new ClojureProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	@Override
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		entryPointName = toClojureStyle( entryPointName );
		Namespace ns = getClojureNamespace( executionContext );
		try
		{
			// We must push *ns* in order to use (in-ns) below
			Var.pushThreadBindings( RT.map( RT.CURRENT_NS, ns, RT.OUT, new PrintWriter( executionContext.getWriterOrDefault() ), RT.ERR, new PrintWriter( executionContext.getErrorWriterOrDefault() ) ) );

			IN_NS.invoke( ns.getName() );

			Var function = ns.findInternedVar( Symbol.intern( entryPointName ) );
			if( function == null )
				throw new NoSuchMethodException( entryPointName );

			return function.applyTo( PersistentVector.create( arguments ).seq() );
		}
		catch( NoSuchMethodException x )
		{
			throw x;
		}
		catch( Exception x )
		{
			throw new ExecutionException( executable.getDocumentName(), x.getMessage(), x );
		}
		finally
		{
			Var.popThreadBindings();
		}
	}

	@Override
	public void releaseContext( ExecutionContext executionContext )
	{
		// Remove our namespace
		Namespace ns = (Namespace) executionContext.getAttributes().get( CLOJURE_NAMESPACE );
		if( ns != null )
			Namespace.remove( ns.getName() );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	/**
	 * clojure.core
	 */
	protected static final Symbol CLOJURE_CORE = Symbol.create( "clojure.core" );

	/**
	 * (in-ns)
	 */
	protected static final Var IN_NS = RT.var( "clojure.core", "in-ns" );

	/**
	 * clojure.core/*allow-unresolved-vars*
	 */
	protected static final Var ALLOW_UNRESOLVED_VARS = RT.var( "clojure.core", "*allow-unresolved-vars*" );

	/**
	 * (clojure.core/refer)
	 */
	protected static final Var REFER = RT.var( "clojure.core", "refer" );

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * Counter for generating unique namespace names.
	 */
	private static final AtomicLong namespaceCounter = new AtomicLong();
}
