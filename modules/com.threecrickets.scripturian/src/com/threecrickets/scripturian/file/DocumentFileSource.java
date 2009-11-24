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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
	 *        instead; note that if an extension is not specified, then the
	 *        first file in the directory with this name, with any extension,
	 *        will be used
	 * @param minimumTimeBetweenValidityChecks
	 *        See {@link #getMinimumTimeBetweenValidityChecks()}
	 */
	public DocumentFileSource( File basePath, String defaultName, long minimumTimeBetweenValidityChecks )
	{
		this.basePath = basePath;
		this.defaultName = defaultName;
		this.minimumTimeBetweenValidityChecks.set( minimumTimeBetweenValidityChecks );
		defaultNameFilter = new StartsWithFilter( defaultName );
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
	 * directory, then this file name in that directory will be used instead.
	 * 
	 * @return The default name
	 */
	public String getDefaultName()
	{
		return defaultName;
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
		// See if we already have a descriptor for this name
		FiledDocumentDescriptor filedDocumentDescriptor = filedDocumentDescriptors.get( name );
		if( filedDocumentDescriptor != null )
			filedDocumentDescriptor = removeIfInvalid( name, filedDocumentDescriptor );

		if( filedDocumentDescriptor == null )
		{
			File file = getFileForName( name );

			// See if we already have a descriptor for this file
			filedDocumentDescriptor = filedDocumentDescriptorsByFile.get( file );
			if( filedDocumentDescriptor != null )
				filedDocumentDescriptor = removeIfInvalid( name, filedDocumentDescriptor );

			if( filedDocumentDescriptor == null )
			{
				// Create a new descriptor
				filedDocumentDescriptor = new FiledDocumentDescriptor( file );
				FiledDocumentDescriptor existing = filedDocumentDescriptors.putIfAbsent( name, filedDocumentDescriptor );
				if( existing != null )
					filedDocumentDescriptor = existing;
				else
					// This is atomically safe, because we'll only get here once
					filedDocumentDescriptorsByFile.put( file, filedDocumentDescriptor );
			}
		}

		return filedDocumentDescriptor;
	}

	/**
	 * @see DocumentSource#setDocumentDescriptor(String, String, String, Object)
	 */
	public DocumentDescriptor<D> setDocumentDescriptor( String name, String text, String tag, D script )
	{
		return filedDocumentDescriptors.put( name, new FiledDocumentDescriptor( text, tag, script ) );
	}

	/**
	 * @see DocumentSource#setDocumentDescriptorIfAbsent(String, String, String,
	 *      Object)
	 */
	public DocumentDescriptor<D> setDocumentDescriptorIfAbsent( String name, String text, String tag, D script )
	{
		return filedDocumentDescriptors.putIfAbsent( name, new FiledDocumentDescriptor( text, tag, script ) );
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

	private final ConcurrentMap<String, FiledDocumentDescriptor> filedDocumentDescriptors = new ConcurrentHashMap<String, FiledDocumentDescriptor>();

	private final Map<File, FiledDocumentDescriptor> filedDocumentDescriptorsByFile = new HashMap<File, FiledDocumentDescriptor>();

	private final File basePath;

	private final String defaultName;

	private final AtomicLong minimumTimeBetweenValidityChecks = new AtomicLong();

	private static class StartsWithFilter implements FilenameFilter
	{
		private final String name;

		private StartsWithFilter( String name )
		{
			this.name = name + ".";
		}

		public boolean accept( File dir, String name )
		{
			return name.startsWith( this.name );
		}
	}

	private final StartsWithFilter defaultNameFilter;

	private File getFileForName( String name )
	{
		File file = new File( basePath, name );

		if( ( defaultName != null ) && file.isDirectory() )
		{
			File[] files = file.listFiles( defaultNameFilter );
			if( ( files != null ) && ( files.length > 0 ) )
				// Return the first file that starts with the default name
				return files[0];
			else
				return new File( file, defaultName );
		}
		else if( !file.exists() )
		{
			File directory = file.getParentFile();
			File[] files = directory.listFiles( new StartsWithFilter( file.getName() ) );
			if( ( files != null ) && ( files.length > 0 ) )
				// Return the first file that starts with the default name
				return files[0];
		}

		return file;
	}

	private FiledDocumentDescriptor removeIfInvalid( String name, FiledDocumentDescriptor filedDocumentDescriptor )
	{
		// Make sure the existing descriptor is valid
		if( !filedDocumentDescriptor.isValid() )
		{
			// Remove invalid descriptor if it's still there
			if( filedDocumentDescriptors.remove( name, filedDocumentDescriptor ) )
			{
				if( filedDocumentDescriptor.file != null )
					// This is atomically safe, because we'll only get here once
					filedDocumentDescriptorsByFile.remove( filedDocumentDescriptor.file );
			}
			filedDocumentDescriptor = null;
		}

		return filedDocumentDescriptor;
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

		// //////////////////////////////////////////////////////////////////////////
		// Private

		private final File file;

		private final long timestamp;

		private final String text;

		private final String tag;

		private final AtomicLong lastValidityCheck;

		private D document;

		private FiledDocumentDescriptor( String text, String tag, D document )
		{
			file = null;
			timestamp = -1;
			this.text = text;
			this.tag = tag;
			lastValidityCheck = new AtomicLong();
			this.document = document;
		}

		private FiledDocumentDescriptor( File file ) throws IOException
		{
			this.file = file;
			timestamp = file.lastModified();
			text = ScripturianUtil.getString( file );
			tag = ScripturianUtil.getExtension( file );
			lastValidityCheck = new AtomicLong();
		}

		private boolean isValid()
		{
			if( file == null )
				// Always valid if not built from a file
				return true;

			long minimumTimeBetweenValidityChecks = DocumentFileSource.this.minimumTimeBetweenValidityChecks.get();
			if( minimumTimeBetweenValidityChecks == -1 )
				// -1 means never check for validity
				return true;

			long now = System.currentTimeMillis();

			// Are we in the threshold for checking for validity?
			if( ( now - lastValidityCheck.get() ) > minimumTimeBetweenValidityChecks )
			{
				// Check for validity
				lastValidityCheck.set( now );
				return file.lastModified() <= timestamp;
			}
			else
				return true;
		}
	}
}
