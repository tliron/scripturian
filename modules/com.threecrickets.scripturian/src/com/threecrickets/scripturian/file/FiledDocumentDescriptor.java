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

package com.threecrickets.scripturian.file;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.threecrickets.scripturian.document.DocumentDescriptor;
import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * Document descriptor for {@link DocumentFileSource}.
 * 
 * @author Tal Liron
 */
class FiledDocumentDescriptor<D> implements DocumentDescriptor<D>
{
	//
	// Attributes
	//

	/**
	 * The file, or null if we're an in-memory document.
	 */
	public final File file;

	/**
	 * Whether the document is valid. Calling this method will sometimes cause a
	 * validity check.
	 * 
	 * @return Whether the document is valid
	 * @see DocumentFileSource#getMinimumTimeBetweenValidityChecks()
	 */
	public boolean isValid()
	{
		if( file == null )
			// Always valid if in-memory document
			return true;

		long minimumTimeBetweenValidityChecks = documentSource.minimumTimeBetweenValidityChecks.get();
		if( minimumTimeBetweenValidityChecks == -1 )
			// -1 means never check for validity
			return true;

		long now = System.currentTimeMillis();

		// Are we in the threshold for checking for validity?
		if( ( now - lastValidityCheckTimestamp ) > minimumTimeBetweenValidityChecks )
		{
			// Check for validity
			lastValidityCheckTimestamp = now;
			return file.lastModified() <= timestamp;
		}
		else
			return true;
	}

	//
	// DocumentDescriptor
	//

	public String getDefaultName()
	{
		return defaultName;
	}

	public String getSourceCode()
	{
		return sourceCode;
	}

	public String getTag()
	{
		return tag;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public D getDocument()
	{
		documentLock.readLock().lock();
		try
		{
			return document;
		}
		finally
		{
			documentLock.readLock().unlock();
		}
	}

	public D setDocument( D document )
	{
		documentLock.writeLock().lock();
		try
		{
			D last = this.document;
			this.document = document;
			return last;
		}
		finally
		{
			documentLock.writeLock().unlock();
		}
	}

	public D setDocumentIfAbsent( D document )
	{
		documentLock.readLock().lock();
		try
		{
			if( this.document != null )
				return this.document;

			documentLock.readLock().unlock();
			// (Might change here!)
			documentLock.writeLock().lock();
			try
			{
				if( this.document != null )
					return this.document;

				D last = this.document;
				this.document = document;
				return last;
			}
			finally
			{
				documentLock.writeLock().unlock();
				documentLock.readLock().lock();
			}
		}
		finally
		{
			documentLock.readLock().unlock();
		}
	}

	public DocumentSource<D> getSource()
	{
		return documentSource;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	protected FiledDocumentDescriptor( DocumentFileSource<D> documentSource, String defaultName, String sourceCode, String tag, D document )
	{
		this.documentSource = documentSource;
		this.defaultName = defaultName;
		file = null;
		timestamp = System.currentTimeMillis();
		this.sourceCode = sourceCode;
		this.tag = tag;
		this.document = document;
	}

	protected FiledDocumentDescriptor( DocumentFileSource<D> documentSource, File file ) throws IOException
	{
		this.documentSource = documentSource;
		this.defaultName = documentSource.getRelativeFilePath( file );
		this.file = file;
		timestamp = file.lastModified();
		sourceCode = ScripturianUtil.getString( file );
		tag = ScripturianUtil.getExtension( file );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The document source.
	 */
	private final DocumentFileSource<D> documentSource;

	/**
	 * Lock for access to {@link #document}.
	 */
	private final ReadWriteLock documentLock = new ReentrantReadWriteLock();

	/**
	 * The document.
	 * 
	 * @see #documentLock
	 */
	private D document;

	/**
	 * The default name.
	 */
	private final String defaultName;

	/**
	 * The timestamp or -1.
	 */
	private final long timestamp;

	/**
	 * The document source code.
	 */
	private final String sourceCode;

	/**
	 * The document tag.
	 */
	private final String tag;

	/**
	 * The timestamp of the last validity check.
	 */
	private volatile long lastValidityCheckTimestamp;
}