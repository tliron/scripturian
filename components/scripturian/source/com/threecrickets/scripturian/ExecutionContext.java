/**
 * Copyright 2009-2012 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
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
 * {@link Executable#makeEnterable(Object, ExecutionContext, Object, ExecutionController)}
 * is called, and then {@link Executable#enter(Object, String, Object...)} is
 * callable by concurrent threads. In this case, <i>all invoking threads share
 * the same execution context</i>. Because the context is immutable (internally
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
	 * @see #disconnect()
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
	// Static operations
	//

	/**
	 * Makes sure there is no execution context associated with this thread.
	 * 
	 * @see #makeCurrent()
	 * @see #getCurrent()
	 */
	public static void disconnect()
	{
		ThreadLocalExecutionContext.current.set( null );
	}

	//
	// Construction
	//

	/**
	 * Constructor.
	 */
	public ExecutionContext()
	{
		this( null );
	}

	/**
	 * Constructor.
	 * 
	 * @param writer
	 *        The standard output set for executables using this context
	 */
	public ExecutionContext( Writer writer )
	{
		this( writer, null );
	}

	/**
	 * Constructor.
	 * 
	 * @param writer
	 *        The standard output set for executables using this context
	 * @param errorWriter
	 *        The standard error set for executables using this context
	 */
	public ExecutionContext( Writer writer, Writer errorWriter )
	{
		this.writer = writer;
		this.errorWriter = errorWriter;
	}

	//
	// Attributes
	//

	/**
	 * General-purpose attributes. Useful for configuring special language
	 * features not supports by the context. Additionally, language adapters and
	 * other components along the execution chain might use this to store
	 * contextual state.
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
	 * Locations where language adapters might search for extra libraries. The
	 * exact use varies per language adapter. "Location" URIs are often
	 * filesystem directory paths, but some adapters might support paths to
	 * specific library files and network URLs.
	 * <p>
	 * Immutable contexts will return an unmodifiable list.
	 * 
	 * @see File#toURI()
	 */
	public List<URI> getLibraryLocations()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		if( immutable )
			return Collections.unmodifiableList( libraryLocations );
		else
			return libraryLocations;
	}

	/**
	 * Services exposed to executables using this context.
	 * <p>
	 * Immutable contexts will return an unmodifiable map.
	 * 
	 * @return The exposed services
	 * @see #isImmutable()
	 */
	public Map<String, Object> getServices()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		if( immutable )
			return Collections.unmodifiableMap( services );
		else
			return services;
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
	 * @return The writer or the default writer
	 */
	public Writer getWriterOrDefault()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		return writer != null ? writer : defaultWriter;
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
	 * The standard error set for executables using this context.
	 * 
	 * @return The error writer or the default error writer
	 */
	public Writer getErrorWriterOrDefault()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		return errorWriter != null ? errorWriter : defaultErrorWriter;
	}

	/**
	 * The standard error set for executables using this context.
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
	 * Sets a language adapter as a user of this context. The adapter will then
	 * get a change to release contextual state when {@link #release()} is
	 * called.
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
	 * Whether this context is enterable.
	 * 
	 * @return True if enterable
	 * @see #enter(Executable, String, Object...)
	 * @see Executable#makeEnterable(Object, ExecutionContext, Object,
	 *      ExecutionController)
	 */
	public boolean isEnterable()
	{
		if( released )
			throw new IllegalStateException( "Cannot access released execution context" );

		return enterable;
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
	 * Enters the executable at a stored, named location, via the last language
	 * adapter that used this context. According to the language, the entry
	 * point can be a function, method, lambda, closure, etc.
	 * <p>
	 * The context must have been previously made enterable by a call to
	 * {@link Executable#makeEnterable(Object, ExecutionContext, Object, ExecutionController)}
	 * .
	 * 
	 * @param executable
	 *        The executable
	 * @param entryPointName
	 *        The name of the entry point
	 * @param arguments
	 *        Optional state to pass to the entry point
	 * @return State returned from the entry point or null
	 * @throws ParsingException
	 * @throws ExecutionException
	 * @throws NoSuchMethodException
	 * @see #isEnterable()
	 * @see #getAdapter()
	 */
	public Object enter( Executable executable, String entryPointName, Object... arguments ) throws ParsingException, ExecutionException, NoSuchMethodException
	{
		if( !enterable )
			throw new IllegalStateException( "This execution context is not enterable" );

		makeCurrent();

		LanguageAdapter languageAdapter = this.languageAdapter;
		if( !languageAdapter.isThreadSafe() )
			languageAdapter.getLock().lock();

		try
		{
			return languageAdapter.enter( entryPointName, executable, this, arguments );
		}
		finally
		{
			if( !languageAdapter.isThreadSafe() )
				languageAdapter.getLock().unlock();
		}
	}

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
	 * @see #disconnect()
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

		for( LanguageAdapter languageAdapter : languageAdapters )
			languageAdapter.releaseContext( this );

		released = true;
	}

	//
	// Object
	//

	@Override
	public String toString()
	{
		return "ExecutionContext: " + ( enterable ? "enterable, " : "non-enterable, " ) + ( immutable ? "immutable, " : "mutable, " ) + ( released ? "released" : "unreleased" );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	/**
	 * Whether this context is enterable.
	 */
	protected volatile boolean enterable;

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * General-purpose attributes. Useful for configuring special language
	 * features not supports by the context. Additionally, language adapters and
	 * other components along the execution chain might use this to store
	 * contextual state.
	 */
	private final Map<String, Object> attributes = new HashMap<String, Object>();

	/**
	 * Locations where language adapters might search for extra libraries. The
	 * exact use varies per language adapter. "Location" URIs are often
	 * filesystem directory paths, but some adapters might support paths to
	 * specific library files and network URLs.
	 */
	private final List<URI> libraryLocations = new ArrayList<URI>();

	/**
	 * Services exposed to executables using this context.
	 */
	private final Map<String, Object> services = new HashMap<String, Object>();

	/**
	 * Language adapters that have used this context.
	 * 
	 * @see #release()
	 */
	private final Set<LanguageAdapter> languageAdapters = new HashSet<LanguageAdapter>();

	/**
	 * The standard output set for executables using this context.
	 */
	private Writer writer;

	/**
	 * The default standard output set for executables using this context.
	 */
	private static volatile Writer defaultWriter = new OutputStreamWriter( System.out );

	/**
	 * The standard error set for executables using this context.
	 */
	private Writer errorWriter;

	/**
	 * The default standard output set for executables using this context.
	 */
	private static volatile Writer defaultErrorWriter = new OutputStreamWriter( System.err );

	/**
	 * The last language adapter used by the context.
	 */
	private LanguageAdapter languageAdapter;

	/**
	 * Whether this context is immutable.
	 */
	private volatile boolean immutable;

	/**
	 * Whether this context has been released.
	 */
	private volatile boolean released;
}
