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

/**
 * Encapsulates context for an {@link Executable}.
 * <p>
 * An execution context is not in itself thread-safe. However, it supports two
 * use cases with different threading behavior.
 * <p>
 * The first occurs when
 * {@link Executable#execute(boolean, ExecutionContext, Object, ExecutionController)}
 * is callable by concurrent threads. In this case, each thread should create
 * <i>its own execution</i> context, and the context is essentially
 * thread-local. Just be aware that if your executable spawns threads, they
 * would need to coordinate access to the context if they need it.
 * <p>
 * TODO: this is wrong!!! The second occurs when
 * {@link Executable#execute(boolean, ExecutionContext, Object, ExecutionController)}
 * is run once, and then
 * {@link Executable#invoke(String, ExecutionContext, Object, ExecutionController)}
 * is callable by concurrent threads. In this case, <i>all invoking threads
 * share the same context</i>. Because the context is immutable (internally
 * {@link #makeImmutable()} is called), it is thread-safe.
 * 
 * @author Tal Liron
 */
public class ExecutionContext
{
	//
	// Construction
	//

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
	 * @return The attributes
	 */
	public Map<String, Object> getAttributes()
	{
		if( immutable )
			return Collections.unmodifiableMap( attributes );
		else
			return attributes;
	}

	/**
	 * @return The exposed variables
	 */
	public Map<String, Object> getExposedVariables()
	{
		if( immutable )
			return Collections.unmodifiableMap( exposedVariables );
		else
			return exposedVariables;
	}

	/**
	 * @return The writer
	 */
	public Writer getWriter()
	{
		return writer;
	}

	/**
	 * @param writer
	 * @return The previous writer
	 */
	public Writer setWriter( Writer writer )
	{
		if( immutable )
			throw new UnsupportedOperationException( "Cannot modify an immutable execution context" );
		Writer old = this.writer;
		this.writer = new PrintWriter( writer, true );
		return old;
	}

	/**
	 * @return The error writer
	 */
	public Writer getErrorWriter()
	{
		return errorWriter;
	}

	/**
	 * @param writer
	 * @return The previous error writer
	 */
	public Writer setErrorWriter( Writer writer )
	{
		if( immutable )
			throw new UnsupportedOperationException( "Cannot modify an immutable execution context" );
		Writer old = this.errorWriter;
		this.errorWriter = writer;
		return old;
	}

	/**
	 * @return The language adapter
	 */
	public LanguageAdapter getAdapter()
	{
		return languageAdapter;
	}

	/**
	 * @param languageAdapter
	 */
	public void setAdapter( LanguageAdapter languageAdapter )
	{
		if( immutable )
			throw new UnsupportedOperationException( "Cannot modify an immutable execution context" );
		this.languageAdapter = languageAdapter;
		languageAdapters.add( languageAdapter );
	}

	/**
	 * @return The language manager
	 */
	public LanguageManager getManager()
	{
		return languageManager;
	}

	/**
	 * Makes this context immutable. Any attempt to alter it or its structures
	 * will result in an {@link UnsupportedOperationException}.
	 * <p>
	 * Calling this method more than once will have no effect. Once made
	 * immutable, execution contexts cannot become mutable again.
	 */
	public void makeImmutable()
	{
		immutable = true;
	}

	//
	// Operations
	//

	/**
	 * Calls {@link LanguageAdapter#releaseContext(ExecutionContext)} on all
	 * adapters that have used this context.
	 */
	public void release()
	{
		for( LanguageAdapter languageAdapter : languageAdapters )
			languageAdapter.releaseContext( this );
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
}
