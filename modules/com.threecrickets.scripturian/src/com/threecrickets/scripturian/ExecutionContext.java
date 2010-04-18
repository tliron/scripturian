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

package com.threecrickets.scripturian;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.threecrickets.scripturian.internal.ThreadLocalExecutionContext;

/**
 * Encapsulates context for an {@link Executable}.
 * <p>
 * You should always {@link #release()} an execution context when done using it.
 * <p>
 * An execution context is not in itself thread-safe. However, it supports two
 * use cases with different threading behavior.
 * <p>
 * The first occurs when
 * {@link Executable#execute(ExecutionContext, Object, ExecutionController)} is
 * callable by concurrent threads. In this case, each thread should create
 * <i>its own execution context</i>, and the context in such cases is
 * essentially thread-unique. Just be aware that if your executable spawns
 * threads, they would need to coordinate access to the context if they need it.
 * <p>
 * The second occurs when
 * {@link Executable#prepareForInvocation(ExecutionContext, Object, ExecutionController)}
 * is called, and then {@link Executable#invoke(String, Object...)} is callable
 * by concurrent threads. In this case, <i>all invoking threads share the same
 * execution context</i>. Because the context is immutable (internally
 * {@link #makeImmutable()} is called), it is "thread-safe" to the extent that
 * you better not try to modify it. Otherwise, an {@link IllegalStateException}
 * is thrown.
 * 
 * @author Tal Liron
 */
public class ExecutionContext
{
	//
	// Static attributes
	//

	/**
	 * The execution context set for this thread.
	 * 
	 * @return An execution context or null
	 * @see #makeCurrent()
	 */
	public static ExecutionContext getCurrent()
	{
		ExecutionContext executionContext = ThreadLocalExecutionContext.current.get();
		if( ( executionContext != null ) && executionContext.released )
		{
			ThreadLocalExecutionContext.current.set( null );
			return null;
		}
		else
			return executionContext;
	}

	//
	// Construction
	//

	/**
	 * Construction
	 * 
	 * @param languageManager
	 * @param writer
	 * @param errorWriter
	 */
	public ExecutionContext( LanguageManager languageManager, Writer writer, Writer errorWriter )
	{
		this.languageManager = languageManager;
		this.writer = writer;
		this.errorWriter = errorWriter;
	}

	//
	// Attributes
	//

	/**
	 * General-purpose attributes. Most useful for language adapters and other
	 * users along the execution chain to store contextual state.
	 * <p>
	 * Immutable contexts will return an unmodifiable map.
	 * 
	 * @return The attributes
	 * @see #isImmutable()
	 */
	public Map<String, Object> getAttributes()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		if( immutable )
			return Collections.unmodifiableMap( attributes );
		else
			return attributes;
	}

	/**
	 * Variables exposed to executables using this context.
	 * <p>
	 * Immutable contexts will return an unmodifiable map.
	 * 
	 * @return The exposed variables
	 * @see #isImmutable()
	 */
	public Map<String, Object> getExposedVariables()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		if( immutable )
			return Collections.unmodifiableMap( exposedVariables );
		else
			return exposedVariables;
	}

	/**
	 * The standard output set for executables using this context.
	 * 
	 * @return The writer or null
	 */
	public Writer getWriter()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		return writer;
	}

	/**
	 * The standard output set for executables using this context.
	 * 
	 * @param writer
	 * @return The previous writer or null
	 */
	public Writer setWriter( Writer writer )
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );
		else if( immutable )
			throw new IllegalStateException( "Cannot modify an immutable execution context" );

		Writer old = this.writer;
		this.writer = new PrintWriter( writer, true );
		return old;
	}

	/**
	 * The standard error set for executables using this context.
	 * 
	 * @return The error writer or null
	 */
	public Writer getErrorWriter()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		return errorWriter;
	}

	/**
	 * * The standard error set for executables using this context.
	 * 
	 * @param writer
	 * @return The previous error writer or null
	 */
	public Writer setErrorWriter( Writer writer )
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );
		else if( immutable )
			throw new IllegalStateException( "Cannot modify an immutable execution context" );

		Writer old = this.errorWriter;
		this.errorWriter = writer;
		return old;
	}

	/**
	 * The last language adapter used by the context.
	 * 
	 * @return The language adapter or null
	 */
	public LanguageAdapter getAdapter()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		return languageAdapter;
	}

	/**
	 * Sets an adapter as a user of this context. The adapter will then get a
	 * change to release contextual state when {@link #release()} is called.
	 * 
	 * @param languageAdapter
	 * @see LanguageAdapter#releaseContext(ExecutionContext)
	 */
	public void setAdapter( LanguageAdapter languageAdapter )
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );
		else if( immutable )
			throw new IllegalStateException( "Cannot modify an immutable execution context" );

		this.languageAdapter = languageAdapter;
		languageAdapters.add( languageAdapter );
	}

	/**
	 * The language manager for which this context is set. Note that contexts
	 * should not be shared between language managers.
	 * 
	 * @return The language manager
	 */
	public LanguageManager getManager()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		return languageManager;
	}

	/**
	 * Immutable contexts will throw an {@link IllegalStateException} whenever
	 * an attempt is made to change them.
	 * 
	 * @return True if immutable
	 */
	public boolean isImmutable()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		return immutable;
	}

	//
	// Operations
	//

	/**
	 * Makes this context immutable. Any attempt to change it will result in an
	 * {@link IllegalStateException}.
	 * <p>
	 * Calling this method more than once will have no effect. Once made
	 * immutable, execution contexts cannot become mutable again.
	 */
	public void makeImmutable()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		immutable = true;
	}

	/**
	 * Sets this execution context for this thread.
	 * 
	 * @return The previous current execution context or null
	 * @see #getCurrent()
	 */
	public ExecutionContext makeCurrent()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		ExecutionContext old = ThreadLocalExecutionContext.current.get();
		ThreadLocalExecutionContext.current.set( this );
		return old;
	}

	/**
	 * Calls {@link LanguageAdapter#releaseContext(ExecutionContext)} on all
	 * adapters that have used this context.
	 * <p>
	 * After this call, any attempt to access the context will result in an
	 * {@link IllegalStateException}.
	 */
	public void release()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		safeRelease();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final Map<String, Object> attributes = new HashMap<String, Object>();

	private final Map<String, Object> exposedVariables = new HashMap<String, Object>();

	private final Set<LanguageAdapter> languageAdapters = new HashSet<LanguageAdapter>();

	private Writer writer;

	private Writer errorWriter;

	private LanguageManager languageManager;

	private LanguageAdapter languageAdapter;

	private volatile boolean immutable;

	private volatile boolean released;

	protected void safeRelease()
	{
		for( LanguageAdapter languageAdapter : languageAdapters )
			languageAdapter.releaseContext( this );
		released = true;
	}
}
