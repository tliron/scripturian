/**
 * Copyright 2009-2011 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.document.DocumentSource;

/**
 * Defrosts all documents within a {@link DocumentSource}.
 * 
 * @author Tal Liron
 * @see DefrostTask
 */
public class Defroster
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param documentSource
	 *        The document source for executables
	 * @param languageManager
	 *        The language manager for executable initialization
	 * @param defaultLanguageTag
	 *        The language tag to used if none is specified
	 * @param isTextWithScriptlets
	 *        Whether the executables are "text-with-scriptlets"
	 * @param prepare
	 *        Whether to prepare executables
	 */
	public Defroster( DocumentSource<Executable> documentSource, LanguageManager languageManager, String defaultLanguageTag, boolean isTextWithScriptlets, boolean prepare )
	{
		super();
		this.documentSource = documentSource;
		this.languageManager = languageManager;
		this.defaultLanguageTag = defaultLanguageTag;
		this.isTextWithScriptlets = isTextWithScriptlets;
		this.prepare = prepare;
	}

	//
	// Attributes
	//

	/**
	 * The language manager for executable initialization.
	 * 
	 * @return A language manager
	 */
	public LanguageManager getLanguageManager()
	{
		return languageManager;
	}

	/**
	 * The document source for executables.
	 * 
	 * @return A document source
	 */
	public DocumentSource<Executable> getDocumentSource()
	{
		return documentSource;
	}

	/**
	 * Whether to prepare executables.
	 * 
	 * @return Whether to prepare executables
	 */
	public boolean isPrepare()
	{
		return prepare;
	}

	/**
	 * @return True if defrosting was interrupted
	 */
	public boolean wasInterrupted()
	{
		return wasInterrupted;
	}

	/**
	 * @return True if there were errors
	 * @see #getErrors()
	 */
	public boolean hasErrors()
	{
		return !errors.isEmpty();
	}

	/**
	 * @return The errors
	 * @see #hasErrors()
	 */
	public Collection<Throwable> getErrors()
	{
		return errors;
	}

	//
	// Operations
	//

	/**
	 * @throws InterruptedException
	 */
	public void defrost() throws InterruptedException
	{
		int processors = Runtime.getRuntime().availableProcessors();
		defrost( processors );
	}

	/**
	 * @param threads
	 * @throws InterruptedException
	 */
	public void defrost( int threads ) throws InterruptedException
	{
		if( threads == 1 )
			defrost( Executors.newSingleThreadExecutor() );
		else
			defrost( Executors.newFixedThreadPool( threads ) );
	}

	/**
	 * @param executorService
	 * @throws InterruptedException
	 */
	public void defrost( ExecutorService executorService ) throws InterruptedException
	{
		defrost( executorService, false );
	}

	/**
	 * @param executorService
	 * @param block
	 * @throws InterruptedException
	 */
	public void defrost( ExecutorService executorService, boolean block ) throws InterruptedException
	{
		Task task = new Task( executorService, block );
		if( block )
			task.run();
		else
			new Thread( task ).start();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The document source for executables.
	 */
	private final DocumentSource<Executable> documentSource;

	/**
	 * The language manager for executable initialization.
	 */
	private final LanguageManager languageManager;

	/**
	 * The language tag to used if none is specified.
	 */
	private final String defaultLanguageTag;

	/**
	 * Whether the executables are "text-with-scriptlets".
	 */
	private final boolean isTextWithScriptlets;

	/**
	 * Whether to prepare executables.
	 */
	private final boolean prepare;

	/**
	 * Whether defrosting was interrupted.
	 */
	private volatile boolean wasInterrupted;

	/**
	 * Defrosting errors.
	 */
	private final CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<Throwable>();

	/**
	 * A task to start all defrosting tasks.
	 * 
	 * @author Tal Liron
	 */
	private class Task implements Runnable
	{
		private Task( ExecutorService executorService, boolean block )
		{
			this.executorService = executorService;
			this.block = block;
		}

		//
		// Runnable
		//

		public void run()
		{
			Callable<Executable>[] defrostTasks = DefrostTask.forDocumentSource( documentSource, languageManager, defaultLanguageTag, isTextWithScriptlets, prepare );
			List<Future<Executable>> futures;
			try
			{
				futures = executorService.invokeAll( Arrays.asList( defrostTasks ) );

				if( block )
				{
					for( Future<Executable> future : futures )
					{
						try
						{
							future.get();
						}
						catch( ExecutionException x )
						{
							errors.add( x.getCause() );
						}
					}
				}
			}
			catch( InterruptedException x )
			{
				wasInterrupted = true;

				// Restore interrupt status
				Thread.currentThread().interrupt();
			}
		}

		private final ExecutorService executorService;

		private final boolean block;
	}
}
