/**
 * Copyright 2009-2016 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.threecrickets.scripturian.document.DocumentDescriptor;
import com.threecrickets.scripturian.document.DocumentFileSource;
import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.exception.DocumentDependencyLoopException;
import com.threecrickets.scripturian.exception.DocumentException;

/**
 * Document descriptor for {@link DocumentFileSource}.
 * 
 * @author Tal Liron
 */
public class FiledDocumentDescriptor<D> implements DocumentDescriptor<D>
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @param documentSource
	 *        The document source
	 * @param defaultName
	 *        The default name to use for directories
	 * @param sourceCode
	 *        The source code
	 * @param tag
	 *        The descriptor tag
	 * @param document
	 *        The document
	 * @param validate
	 *        Whether to validate the document
	 */
	public FiledDocumentDescriptor( DocumentFileSource<D> documentSource, String defaultName, String sourceCode, String tag, D document, boolean validate )
	{
		this.documentSource = documentSource;
		this.defaultName = defaultName;
		file = null;
		timestamp = System.currentTimeMillis();
		this.sourceCode = sourceCode;
		this.tag = tag;
		this.document = document;
		this.validate = validate;
	}

	/**
	 * Constructor.
	 * 
	 * @param documentSource
	 *        The document source
	 * @param file
	 *        The file
	 * @param read
	 *        Whether to read the source code from the file
	 * @param charset
	 *        The charset to use for reading source code from the file
	 * @throws DocumentException
	 *         In case of a document retrieval error
	 */
	public FiledDocumentDescriptor( DocumentFileSource<D> documentSource, File file, boolean read, Charset charset ) throws DocumentException
	{
		this.documentSource = documentSource;
		this.file = file;
		defaultName = documentSource.getRelativeFilePath( file );
		timestamp = file.lastModified();
		validate = true;

		String sourceCode = null;
		if( read && file.exists() )
		{
			try
			{
				sourceCode = ScripturianUtil.getString( file, charset );
			}
			catch( FileNotFoundException x )
			{
			}
			catch( IOException x )
			{
				throw new DocumentException( "Could not read file " + file, x );
			}
		}

		this.sourceCode = sourceCode;
		tag = ScripturianUtil.getExtension( file );
	}

	//
	// Attributes
	//

	/**
	 * The file, or null if we're an in-memory document.
	 */
	public final File file;

	/**
	 * Whether to validate the document.
	 */
	public final boolean validate;

	/**
	 * Whether the document is valid. Calling this method will sometimes cause a
	 * validity check.
	 * <p>
	 * Note that the validity check will cascade to document we depend on.
	 * 
	 * @return Whether the document is valid
	 * @see DocumentFileSource#getMinimumTimeBetweenValidityChecks()
	 * @see DocumentDescriptor#getDependencies()
	 * @throws DocumentDependencyLoopException
	 */
	public boolean isValid() throws DocumentDependencyLoopException
	{
		return validate ? isValid( new HashSet<String>() ) : true;
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

				this.document = document;
				return null;
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

	public Set<DocumentDescriptor<D>> getDependencies()
	{
		return dependencies;
	}

	public void invalidate()
	{
		invalid = true;
	}

	//
	// Object
	//

	@Override
	public String toString()
	{
		return "FiledDocumentDescriptor: " + defaultName + ", " + tag + ", " + timestamp + ", " + validate;
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
	 * The dependencies.
	 */
	private final Set<DocumentDescriptor<D>> dependencies = new CopyOnWriteArraySet<DocumentDescriptor<D>>();

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

	/**
	 * Cached validity.
	 */
	private volatile boolean invalid;

	/**
	 * Whether the document is valid. Calling this method will sometimes cause a
	 * validity check.
	 * <p>
	 * Note that the validity check will cascade to document we depend on.
	 * 
	 * @param testedDependencies
	 *        A collection to keep track of tested dependencies
	 * @return Whether the document is valid
	 * @throws DocumentDependencyLoopException
	 * @see DocumentFileSource#getMinimumTimeBetweenValidityChecks()
	 * @see DocumentDescriptor#getDependencies()
	 */
	private boolean isValid( Set<String> testedDependencies ) throws DocumentDependencyLoopException
	{
		// Once invalid, always invalid
		if( invalid )
			return false;

		// If any of our dependencies is invalid, then so are we
		if( !areAllDependenciesValid( testedDependencies ) )
		{
			invalid = true;
			return false;
		}

		// No validity checks for in-memory documents
		if( file == null )
			return true;

		long minimumTimeBetweenValidityChecks = documentSource.getMinimumTimeBetweenValidityChecks();

		// -1 means don't check for validity
		if( minimumTimeBetweenValidityChecks == -1 )
		{
			// Valid, for now (might not be later)
			return true;
		}

		long now = System.currentTimeMillis();

		// Are we in the threshold for checking for validity?
		if( ( now - lastValidityCheckTimestamp ) > minimumTimeBetweenValidityChecks )
		{
			// Check for validity
			lastValidityCheckTimestamp = now;
			if( file.lastModified() <= timestamp )
			{
				// Valid, for now (might not be later)
				return true;
			}
			else
			{
				// Invalid
				invalid = true;
				return false;
			}
		}
		else
		{
			// Valid, for now (might not be later)
			return true;
		}
	}

	/**
	 * Test whether are dependencies are valid, while avoiding circular
	 * dependency loops.
	 * 
	 * @param testedDependencies
	 *        A collection to keep track of tested dependencies
	 * @return Whether are dependencies are valid
	 * @throws DocumentDependencyLoopException
	 */
	private boolean areAllDependenciesValid( Set<String> testedDependencies ) throws DocumentDependencyLoopException
	{
		// Do not follow circular dependencies
		if( testedDependencies.contains( getDefaultName() ) )
			return true;

		testedDependencies.add( getDefaultName() );

		try
		{
			for( DocumentDescriptor<D> documentDescriptor : dependencies )
			{
				if( documentDescriptor instanceof FiledDocumentDescriptor<?> )
				{
					FiledDocumentDescriptor<D> filedDocumentDescriptor = (FiledDocumentDescriptor<D>) documentDescriptor;
					if( !filedDocumentDescriptor.isValid( testedDependencies ) )
						return false;
				}
			}
		}
		catch( StackOverflowError x )
		{
			throwDocumentDependencyLoopException();
		}

		return true;
	}

	private void throwDocumentDependencyLoopException() throws DocumentDependencyLoopException
	{
		StringBuilder message = new StringBuilder();
		message.append( "FiledDocumentDescriptor dependency loop for " + getDefaultName() + ":" );
		for( DocumentDescriptor<D> documentDescriptor : dependencies )
		{
			message.append( " \"" );
			message.append( documentDescriptor.getDefaultName() );
			message.append( '\"' );
		}

		throw new DocumentDependencyLoopException( message.toString() );
	}
}