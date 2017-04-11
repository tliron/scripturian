/**
 * Copyright 2009-2017 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
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
import com.threecrickets.scripturian.ParsingContext;
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
	 * Constructor.
	 * 
	 * @param parsingContext
	 *        The parsing context
	 */
	public Defroster( ParsingContext parsingContext )
	{
		this.parsingContext = parsingContext;
	}

	//
	// Attributes
	//

	/**
	 * The parsing context.
	 * 
	 * @return The parsing context
	 */
	public ParsingContext getLanguageManager()
	{
		return parsingContext;
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
	 * Defrosts using a new thread pool with 1 thread per available processor
	 * without blocking.
	 * 
	 * @throws InterruptedException
	 *         In case a thread was interrupted
	 */
	public void defrost() throws InterruptedException
	{
		int processors = Runtime.getRuntime().availableProcessors();
		defrost( processors );
	}

	/**
	 * Defrosts using a new thread pool without blocking.
	 * 
	 * @param threads
	 *        The number of threads to use
	 * @throws InterruptedException
	 *         In case a thread was interrupted
	 */
	public void defrost( int threads ) throws InterruptedException
	{
		if( threads == 1 )
			defrost( Executors.newSingleThreadExecutor() );
		else
			defrost( Executors.newFixedThreadPool( threads ) );
	}

	/**
	 * Defrosts using an executor service without blocking.
	 * 
	 * @param executorService
	 *        The executor service to use
	 * @throws InterruptedException
	 *         In case a thread was interrupted
	 */
	public void defrost( ExecutorService executorService ) throws InterruptedException
	{
		defrost( executorService, false );
	}

	/**
	 * Defrosts using an executor service.
	 * 
	 * @param executorService
	 *        The executor service to use
	 * @param block
	 *        Whether to block until done
	 * @throws InterruptedException
	 *         In case a thread was interrupted
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
	 * The parsing context.
	 */
	private final ParsingContext parsingContext;

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
			Callable<Executable>[] defrostTasks = DefrostTask.createMany( parsingContext );
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
