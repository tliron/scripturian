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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.threecrickets.scripturian.document.DocumentDescriptor;
import com.threecrickets.scripturian.document.DocumentSource;

/**
 * Reads document stored in files under a base directory. The file contents are
 * cached, and checked for validity according to their modification timestamps.
 * <p>
 * Documents added to the file source exist only in memory, and are not actually
 * saved to a file.
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
	 *        If the name used in {@link #getDocument(String)} points to a
	 *        directory, then this file name in that directory will be used
	 *        instead; note that if an extension is not specified, then the
	 *        first file in the directory with this name, with any extension,
	 *        will be used
	 * @param minimumTimeBetweenValidityChecks
	 *        See {@link #getMinimumTimeBetweenValidityChecks()}
	 */
	public DocumentFileSource( File basePath, String defaultName, long minimumTimeBetweenValidityChecks )
	{
		this.basePath = basePath;
		this.basePathLength = basePath.getPath().length();
		this.defaultName = defaultName;
		this.minimumTimeBetweenValidityChecks.set( minimumTimeBetweenValidityChecks );
		defaultNameFilter = new StartsWithFilter( defaultName );
	}

	/**
	 * Constructs a document file source.
	 * 
	 * @param basePath
	 *        The base path
	 * @param defaultName
	 *        If the name used in {@link #getDocument(String)} points to a
	 *        directory, then this file name in that directory will be used
	 *        instead; note that if an extension is not specified, then the
	 *        first file in the directory with this name, with any extension,
	 *        will be used
	 * @param minimumTimeBetweenValidityChecks
	 *        See {@link #getMinimumTimeBetweenValidityChecks()}
	 */
	public DocumentFileSource( String basePath, String defaultName, long minimumTimeBetweenValidityChecks )
	{
		this( new File( basePath ), defaultName, minimumTimeBetweenValidityChecks );
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
	 * If the name used in {@link #getDocument(String)} points to a directory,
	 * then this file name in that directory will be used instead.
	 * 
	 * @return The default name
	 */
	public String getDefaultName()
	{
		return defaultName;
	}

	/**
	 * Attempts to call {@link #getDocument(String)} for a specific name within
	 * less than this time from the previous call will return the cached
	 * descriptor without checking if it is valid. A value of -1 disables all
	 * validity checking.
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
	 * @see DocumentSource#getDocument(String)
	 */
	public DocumentDescriptor<D> getDocument( String documentName ) throws IOException
	{
		// See if we already have a descriptor for this name
		FiledDocumentDescriptor<D> filedDocumentDescriptor = filedDocumentDescriptors.get( documentName );
		if( filedDocumentDescriptor != null )
			filedDocumentDescriptor = removeIfInvalid( documentName, filedDocumentDescriptor );

		if( filedDocumentDescriptor == null )
		{
			File file = getFileForDocumentName( documentName );

			// See if we already have a descriptor for this file
			filedDocumentDescriptor = filedDocumentDescriptorsByFile.get( file );
			if( filedDocumentDescriptor != null )
				filedDocumentDescriptor = removeIfInvalid( documentName, filedDocumentDescriptor );

			if( filedDocumentDescriptor == null )
			{
				// Create a new descriptor
				filedDocumentDescriptor = new FiledDocumentDescriptor<D>( this, file );
				FiledDocumentDescriptor<D> existing = filedDocumentDescriptors.putIfAbsent( documentName, filedDocumentDescriptor );
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
	 * @see DocumentSource#setDocument(String, String, String, Object)
	 */
	public DocumentDescriptor<D> setDocument( String documentName, String sourceCode, String tag, D document )
	{
		return filedDocumentDescriptors.put( documentName, new FiledDocumentDescriptor<D>( this, documentName, sourceCode, tag, document ) );
	}

	/**
	 * @see DocumentSource#setDocumentIfAbsent(String, String, String, Object)
	 */
	public DocumentDescriptor<D> setDocumentIfAbsent( String documentName, String sourceCode, String tag, D document )
	{
		return filedDocumentDescriptors.putIfAbsent( documentName, new FiledDocumentDescriptor<D>( this, documentName, sourceCode, tag, document ) );
	}

	/**
	 * @see DocumentSource#getDocuments()
	 */
	public Collection<DocumentDescriptor<D>> getDocuments()
	{
		return getDocumentDescriptors( basePath );
	}

	/**
	 * @see com.threecrickets.scripturian.document.DocumentSource#getIdentifier()
	 */
	public String getIdentifier()
	{
		return basePath.toString();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The document descriptors.
	 */
	private final ConcurrentMap<String, FiledDocumentDescriptor<D>> filedDocumentDescriptors = new ConcurrentHashMap<String, FiledDocumentDescriptor<D>>();

	/**
	 * The document descriptors.
	 */
	private final ConcurrentMap<File, FiledDocumentDescriptor<D>> filedDocumentDescriptorsByFile = new ConcurrentHashMap<File, FiledDocumentDescriptor<D>>();

	/**
	 * The base path.
	 */
	private final File basePath;

	/**
	 * Length of the base path cached for performance.
	 */
	private final int basePathLength;

	/**
	 * If the name used in {@link #getDocument(String)} points to a directory,
	 * then this file name in that directory will be used instead; note that if
	 * an extension is not specified, then the first file in the directory with
	 * this name, with any extension, will be used.
	 */
	private final String defaultName;

	/**
	 * See {@link #getMinimumTimeBetweenValidityChecks()}
	 */
	final AtomicLong minimumTimeBetweenValidityChecks = new AtomicLong();

	/**
	 * Recursively collects document descriptors for all files under a base
	 * path.
	 * 
	 * @param basePath
	 *        The base path
	 * @return The document descriptors
	 */
	private Collection<DocumentDescriptor<D>> getDocumentDescriptors( File basePath )
	{
		ArrayList<DocumentDescriptor<D>> list = new ArrayList<DocumentDescriptor<D>>();

		File[] files = basePath.listFiles();
		if( files != null )
		{
			for( File file : files )
			{
				if( file.isDirectory() )
					// Recurse
					list.addAll( getDocumentDescriptors( file ) );
				else
				{
					FiledDocumentDescriptor<D> filedDocumentDescriptor = filedDocumentDescriptorsByFile.get( file );
					if( filedDocumentDescriptor == null )
					{
						try
						{
							filedDocumentDescriptor = new FiledDocumentDescriptor<D>( this, file );
							FiledDocumentDescriptor<D> existing = filedDocumentDescriptorsByFile.putIfAbsent( file, filedDocumentDescriptor );
							if( existing != null )
								filedDocumentDescriptor = existing;
						}
						catch( IOException x )
						{
						}
					}
					list.add( filedDocumentDescriptor );
				}
			}
		}

		return list;
	}

	/**
	 * Filters all filenames that start with a prefix.
	 * 
	 * @author Tal Liron
	 */
	private static class StartsWithFilter implements FilenameFilter
	{
		private final String prefix;

		private StartsWithFilter( String prefix )
		{
			this.prefix = prefix + ".";
		}

		public boolean accept( File dir, String name )
		{
			return name.startsWith( prefix );
		}
	}

	/**
	 * Filters all filenames that start with a prefix.
	 */
	private final StartsWithFilter defaultNameFilter;

	/**
	 * Returns a non-directory file, treating the document name as if it were a
	 * path under our base path. If the path specifies a directory, the file
	 * with default name under that directory is used.
	 * 
	 * @param documentName
	 *        The document name
	 * @return The file
	 */
	private File getFileForDocumentName( String documentName )
	{
		File file = new File( basePath, documentName );

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

	/**
	 * Gets the file's path relative to the base path.
	 * 
	 * @param file
	 *        The file
	 * @return The path
	 */
	String getRelativeFilePath( File file )
	{
		return file.getPath().substring( basePathLength );
	}

	/**
	 * Removes a file descriptor if it is no longer valid.
	 * 
	 * @param documentName
	 *        The document name
	 * @param filedDocumentDescriptor
	 *        The document descriptor
	 * @return The document descriptor or null if it was removed
	 */
	private FiledDocumentDescriptor<D> removeIfInvalid( String documentName, FiledDocumentDescriptor<D> filedDocumentDescriptor )
	{
		// Make sure the existing descriptor is valid
		if( !filedDocumentDescriptor.isValid() )
		{
			// Remove invalid descriptor if it's still there
			if( filedDocumentDescriptors.remove( documentName, filedDocumentDescriptor ) )
			{
				if( filedDocumentDescriptor.file != null )
					// This is atomically safe, because we'll only get here once
					filedDocumentDescriptorsByFile.remove( filedDocumentDescriptor.file );
			}
			filedDocumentDescriptor = null;
		}

		return filedDocumentDescriptor;
	}
}
