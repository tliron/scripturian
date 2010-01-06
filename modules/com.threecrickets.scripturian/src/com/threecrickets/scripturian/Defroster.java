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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.script.ScriptEngineManager;

import com.threecrickets.scripturian.internal.DefrostTask;

/**
 * @author Tal Liron
 */
public class Defroster
{
	//
	// Construction
	//

	public Defroster( ScriptEngineManager scriptEngineManager, DocumentSource<Document> documentSource, boolean allowCompilation )
	{
		super();
		this.scriptEngineManager = scriptEngineManager;
		this.documentSource = documentSource;
		this.allowCompilation = allowCompilation;
	}

	//
	// Attributes
	//

	public ScriptEngineManager getScriptEngineManager()
	{
		return scriptEngineManager;
	}

	public DocumentSource<Document> getDocumentSource()
	{
		return documentSource;
	}

	public boolean isAllowCompilation()
	{
		return allowCompilation;
	}

	public boolean wasInterrupted()
	{
		return wasInterrupted;
	}

	public boolean hasErrors()
	{
		return !errors.isEmpty();
	}

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

	private final ScriptEngineManager scriptEngineManager;

	private final DocumentSource<Document> documentSource;

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
			Collection<DocumentDescriptor<Document>> documentDescriptors = documentSource.getDocumentDescriptors();
			ArrayList<Callable<Document>> defrosters = new ArrayList<Callable<Document>>( documentDescriptors.size() );
			for( DocumentDescriptor<Document> documentDescriptor : documentDescriptors )
				defrosters.add( new DefrostTask( documentDescriptor, Defroster.this ) );

			List<Future<Document>> futures;
			try
			{
				futures = executorService.invokeAll( defrosters );

				if( block )
				{
					for( Future<Document> future : futures )
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
