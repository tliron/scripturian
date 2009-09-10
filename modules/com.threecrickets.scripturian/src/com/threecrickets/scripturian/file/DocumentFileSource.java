/**
 * Copyright 2009 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://www.threecrickets.com/
 */

package com.threecrickets.scripturian.file;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.threecrickets.scripturian.DocumentSource;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * Reads document stored in files under a base directory. The file contents are
 * cached, and checked for validity according to their modification timestamps.
 * 
 * @author Tal Liron
 */
public class DocumentFileSource<D> implements DocumentSource<D>
{
	//
	// Construction
	//

	/**
	 * Constructs a document file source.
	 * 
	 * @param basePath
	 *        The base path
	 * @param defaultName
	 *        If the name used in {@link #getDocumentDescriptor(String)} points
	 *        to a directory, then this file name in that directory will be used
	 *        instead
	 * @param defaultExtension
	 *        If the name used in {@link #getDocumentDescriptor(String)} does
	 *        not point to a valid file or directory, then this extension will
	 *        be added and the name will be tested for again
	 * @param minimumTimeBetweenValidityChecks
	 *        See {@link #getMinimumTimeBetweenValidityChecks()}
	 */
	public DocumentFileSource( File basePath, String defaultName, String defaultExtension, long minimumTimeBetweenValidityChecks )
	{
		this.basePath = basePath;
		this.defaultName = defaultName;
		this.defaultExtension = defaultExtension;
		this.minimumTimeBetweenValidityChecks.set( minimumTimeBetweenValidityChecks );
	}

	//
	// Attributes
	//

	/**
	 * The base path.
	 * 
	 * @return The base path
	 */
	public File getBasePath()
	{
		return basePath;
	}

	/**
	 * If the name used in {@link #getDocumentDescriptor(String)} points to a
	 * directory, then this file name in that directory will be used instead
	 * 
	 * @return The default name
	 */
	public String getDefaultName()
	{
		return defaultName;
	}

	/**
	 * If the name used in {@link #getDocumentDescriptor(String)} does not point
	 * to a valid file or directory, then this extension will be added and the
	 * name will be tested for again
	 * 
	 * @return The default extension
	 */
	public String getDefaultExtension()
	{
		return defaultExtension;
	}

	//
	// DocumentSource
	//

	/**
	 * This implementation caches the document descriptor, including the
	 * document instance stored in it. The cached descriptor will be reset if
	 * the document file is updated since the last call. In order to avoid
	 * checking this every time this method is called, use
	 * {@link #setMinimumTimeBetweenValidityChecks(long)}.
	 * 
	 * @see DocumentSource#getDocumentDescriptor(String)
	 */
	public DocumentDescriptor<D> getDocumentDescriptor( String name ) throws IOException
	{
		File file = getFileForName( name );
		name = file.getPath();

		FiledDocumentDescriptor filedDocumentDescriptor = documentDescriptors.get( name );

		if( filedDocumentDescriptor != null )
		{
			// Make sure the existing descriptor is valid
			if( !filedDocumentDescriptor.isValid( file ) )
			{
				// Remove invalid descriptor if it's still there
				documentDescriptors.remove( name, filedDocumentDescriptor );
				filedDocumentDescriptor = null;
			}
		}

		if( filedDocumentDescriptor == null )
		{
			// Create a new descriptor
			filedDocumentDescriptor = new FiledDocumentDescriptor( file );
			FiledDocumentDescriptor existing = documentDescriptors.putIfAbsent( name, filedDocumentDescriptor );
			if( existing != null )
				filedDocumentDescriptor = existing;
		}

		return filedDocumentDescriptor;
	}

	/**
	 * @see DocumentSource#setDocumentDescriptor(String, String, String, Object)
	 */
	public DocumentDescriptor<D> setDocumentDescriptor( String name, String text, String tag, D script )
	{
		name = getFileForName( name ).getPath();
		return documentDescriptors.put( name, new FiledDocumentDescriptor( text, tag, script ) );
	}

	/**
	 * @see DocumentSource#setDocumentDescriptorIfAbsent(String, String, String,
	 *      Object)
	 */
	public DocumentDescriptor<D> setDocumentDescriptorIfAbsent( String name, String text, String tag, D script )
	{
		name = getFileForName( name ).getPath();
		return documentDescriptors.putIfAbsent( name, new FiledDocumentDescriptor( text, tag, script ) );
	}

	//
	// Attributes
	//

	/**
	 * Attempts to call {@link #getDocumentDescriptor(String)} for a specific
	 * name within less than this time from the previous call will return the
	 * cached descriptor without checking if it is valid. A value of -1 disables
	 * all validity checking.
	 * 
	 * @return The minimum time between validity checks in milliseconds
	 * @see #setMinimumTimeBetweenValidityChecks(long)
	 */
	public long getMinimumTimeBetweenValidityChecks()
	{
		return minimumTimeBetweenValidityChecks.get();
	}

	/**
	 * @param minimumTimeBetweenValidityChecks
	 * @see #getMinimumTimeBetweenValidityChecks()
	 */
	public void setMinimumTimeBetweenValidityChecks( long minimumTimeBetweenValidityChecks )
	{
		this.minimumTimeBetweenValidityChecks.set( minimumTimeBetweenValidityChecks );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final ConcurrentMap<String, FiledDocumentDescriptor> documentDescriptors = new ConcurrentHashMap<String, FiledDocumentDescriptor>();

	private final File basePath;

	private final String defaultName;

	private final String defaultExtension;

	private final AtomicLong minimumTimeBetweenValidityChecks = new AtomicLong();

	private File getFileForName( String name )
	{
		File file = new File( basePath, name );

		if( ( defaultName != null ) && file.isDirectory() )
			file = new File( file, defaultName );
		else if( ( defaultExtension != null ) && !file.exists() )
			file = new File( basePath, name + "." + defaultExtension );

		return file;
	}

	private class FiledDocumentDescriptor implements DocumentDescriptor<D>
	{
		public String getText()
		{
			return text;
		}

		public String getTag()
		{
			return tag;
		}

		public synchronized D getDocument()
		{
			return document;
		}

		public synchronized D setDocument( D document )
		{
			D old = this.document;
			this.document = document;
			return old;
		}

		public synchronized D setDocumentIfAbsent( D document )
		{
			D old = this.document;
			if( old == null )
				this.document = document;
			return old;
		}

		private FiledDocumentDescriptor( String text, String tag, D document )
		{
			this.text = text;
			this.tag = tag;
			this.document = document;

			// This will disable validity checks
			timestamp = -1;
		}

		private FiledDocumentDescriptor( File file ) throws IOException
		{
			text = ScripturianUtil.getString( file );

			String name = file.getName();
			int dot = name.lastIndexOf( '.' );
			if( dot != -1 )
				tag = name.substring( dot + 1 );
			else
				tag = null;

			timestamp = file.lastModified();
		}

		private boolean isValid( File file )
		{
			if( timestamp == -1 )
				return true;

			long minimumTimeBetweenValidityChecks = DocumentFileSource.this.minimumTimeBetweenValidityChecks.get();
			if( minimumTimeBetweenValidityChecks == -1 )
				return true;

			long now = System.currentTimeMillis();

			if( ( now - lastValidityCheck.get() ) > minimumTimeBetweenValidityChecks )
			{
				lastValidityCheck.set( now );
				return file.lastModified() <= timestamp;
			}
			else
				return true;
		}

		// //////////////////////////////////////////////////////////////////////////
		// Private

		private final long timestamp;

		private final AtomicLong lastValidityCheck = new AtomicLong();

		private final String text;

		private final String tag;

		private D document;
	}
}
