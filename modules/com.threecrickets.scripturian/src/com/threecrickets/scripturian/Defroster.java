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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Tal Liron
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
	 *        The document source
	 * @param manager
	 *        The script engine manager to use for document initialization
	 * @param allowCompilation
	 *        Whether to allow compilation for initialized documents
	 */
	public Defroster( DocumentSource<Executable> documentSource, LanguageManager manager, boolean allowCompilation )
	{
		super();
		this.documentSource = documentSource;
		this.manager = manager;
		this.allowCompilation = allowCompilation;
	}

	//
	// Attributes
	//

	/**
	 * @return
	 */
	public LanguageManager getManager()
	{
		return manager;
	}

	/**
	 * @return
	 */
	public DocumentSource<Executable> getDocumentSource()
	{
		return documentSource;
	}

	/**
	 * @return
	 */
	public boolean isAllowCompilation()
	{
		return allowCompilation;
	}

	/**
	 * @return
	 */
	public boolean wasInterrupted()
	{
		return wasInterrupted;
	}

	/**
	 * @return
	 */
	public boolean hasErrors()
	{
		return !errors.isEmpty();
	}

	/**
	 * @return
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
	 * The document source.
	 */
	private final DocumentSource<Executable> documentSource;

	/**
	 * The script engine manager to use for document initialization.
	 */
	private final LanguageManager manager;

	/**
	 * Whether to allow compilation for initialized documents.
	 */
	private final boolean allowCompilation;

	private volatile boolean wasInterrupted;

	private final CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<Throwable>();

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
			Callable<Executable>[] defrostTasks = DefrostTask.forDocumentSource( documentSource, manager, allowCompilation );
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
			}
		}

		private final ExecutorService executorService;

		private final boolean block;
	}
}
