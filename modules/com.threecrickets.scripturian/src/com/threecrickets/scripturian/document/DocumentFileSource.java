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

package com.threecrickets.scripturian.document;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.threecrickets.scripturian.exception.DocumentException;
import com.threecrickets.scripturian.exception.DocumentNotFoundException;
import com.threecrickets.scripturian.internal.FiledDocumentDescriptor;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * Reads document stored in files under a base directory. The file contents are
 * cached, and checked for validity according to their modification timestamps.
 * The validity check is cascaded according to the dependency tree (see
 * {@link DocumentDescriptor#getDependencies()}.
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
	 * Constructs a document file source. The identifier will be the base path.
	 * 
	 * @param basePath
	 *        The base path
	 * @param defaultName
	 *        If the name used in {@link #getDocument(String)} points to a
	 *        directory, then this file name in that directory will be used
	 *        instead; note that if an extension is not specified, then the
	 *        first file in the directory with this name, with any extension,
	 *        will be used
	 * @param preferredExtension
	 *        An extension to prefer if more than one file with the same name is
	 *        in a directory
	 * @param minimumTimeBetweenValidityChecks
	 *        See {@link #getMinimumTimeBetweenValidityChecks()}
	 */
	public DocumentFileSource( File basePath, String defaultName, String preferredExtension, long minimumTimeBetweenValidityChecks )
	{
		this( basePath.getPath(), basePath, defaultName, preferredExtension, minimumTimeBetweenValidityChecks );
	}

	/**
	 * Constructs a document file source. The identifier will be the base path.
	 * 
	 * @param basePath
	 *        The base path
	 * @param defaultName
	 *        If the name used in {@link #getDocument(String)} points to a
	 *        directory, then this file name in that directory will be used
	 *        instead; note that if an extension is not specified, then the
	 *        first file in the directory with this name, with any extension,
	 *        will be used
	 * @param preferredExtension
	 *        An extension to prefer if more than one file with the same name is
	 *        in a directory
	 * @param minimumTimeBetweenValidityChecks
	 *        See {@link #getMinimumTimeBetweenValidityChecks()}
	 */
	public DocumentFileSource( String basePath, String defaultName, String preferredExtension, long minimumTimeBetweenValidityChecks )
	{
		this( basePath, basePath, defaultName, preferredExtension, minimumTimeBetweenValidityChecks );
	}

	/**
	 * Constructs a document file source.
	 * 
	 * @param identifier
	 *        The identifier
	 * @param basePath
	 *        The base path
	 * @param defaultName
	 *        If the name used in {@link #getDocument(String)} points to a
	 *        directory, then this file name in that directory will be used
	 *        instead; note that if an extension is not specified, then the
	 *        first file in the directory with this name, with any extension,
	 *        will be used
	 * @param preferredExtension
	 *        An extension to prefer if more than one file with the same name is
	 *        in a directory
	 * @param minimumTimeBetweenValidityChecks
	 *        See {@link #getMinimumTimeBetweenValidityChecks()}
	 */
	public DocumentFileSource( String identifier, String basePath, String defaultName, String preferredExtension, long minimumTimeBetweenValidityChecks )
	{
		this( identifier, new File( basePath ), defaultName, preferredExtension, minimumTimeBetweenValidityChecks );
	}

	/**
	 * Constructs a document file source.
	 * 
	 * @param identifier
	 *        The identifier
	 * @param basePath
	 *        The base path
	 * @param defaultName
	 *        If the name used in {@link #getDocument(String)} points to a
	 *        directory, then this file name in that directory will be used
	 *        instead; note that if an extension is not specified, then the
	 *        first file in the directory with this name, with any extension,
	 *        will be used
	 * @param preferredExtension
	 *        An extension to prefer if more than one file with the same name is
	 *        in a directory
	 * @param minimumTimeBetweenValidityChecks
	 *        See {@link #getMinimumTimeBetweenValidityChecks()}
	 */
	public DocumentFileSource( String identifier, File basePath, String defaultName, String preferredExtension, long minimumTimeBetweenValidityChecks )
	{
		this.identifier = identifier;
		this.basePath = ScripturianUtil.getNormalizedFile( basePath );
		this.defaultName = defaultName;
		this.preferredExtension = preferredExtension;
		this.minimumTimeBetweenValidityChecks = minimumTimeBetweenValidityChecks;
		defaultNameFilter = new ExtensionInsensitiveFilter( defaultName );
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
	 * then this file name in that directory will be used instead. If an
	 * extension is not specified, then the preferred extension will be used.
	 * 
	 * @return The default name
	 * @see #setDefaultName(String)
	 */
	public String getDefaultName()
	{
		return defaultName;
	}

	/**
	 * @param defaultName
	 *        The default name
	 * @see #getDefaultName()
	 */
	public void setDefaultName( String defaultName )
	{
		this.defaultName = defaultName;
	}

	/**
	 * An extension to prefer if more than one file with the same name is in a
	 * directory.
	 * 
	 * @return The preferred extension
	 * @see #setPreferredExtension(String)
	 */
	public String getPreferredExtension()
	{
		return preferredExtension != null ? preferredExtension.substring( 1 ) : null;
	}

	/**
	 * @param preferredExtension
	 *        The preferred extension
	 * @see #getPreferredExtension()
	 */
	public void setPreferredExtension( String preferredExtension )
	{
		if( preferredExtension != null )
			this.preferredExtension = "." + preferredExtension;
		else
			this.preferredExtension = null;
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
		return minimumTimeBetweenValidityChecks;
	}

	/**
	 * @param minimumTimeBetweenValidityChecks
	 * @see #getMinimumTimeBetweenValidityChecks()
	 */
	public void setMinimumTimeBetweenValidityChecks( long minimumTimeBetweenValidityChecks )
	{
		this.minimumTimeBetweenValidityChecks = minimumTimeBetweenValidityChecks;
	}

	/**
	 * Gets the file's path relative to the base path.
	 * 
	 * @param file
	 *        The file
	 * @return The path
	 */
	public String getRelativeFilePath( File file )
	{
		return ScripturianUtil.getRelativeFile( ScripturianUtil.getNormalizedFile( file ), basePath ).getPath();
	}

	/**
	 * Gets a document descriptor for the document if it's in the cache.
	 * Otherwise, returns null.
	 * 
	 * @param documentName
	 *        The document name
	 * @return The document descriptor or null
	 */
	public FiledDocumentDescriptor<D> getCachedDocumentDescriptor( String documentName )
	{
		// See if we have a descriptor for this name
		FiledDocumentDescriptor<D> filedDocumentDescriptor = filedDocumentDescriptorsByAlias.get( documentName );
		if( filedDocumentDescriptor != null )
			return filedDocumentDescriptor;

		try
		{
			File file = getFileForDocumentName( documentName );

			// See if we have a descriptor for this file
			return filedDocumentDescriptorsByFile.get( file );
		}
		catch( DocumentNotFoundException x )
		{
			return null;
		}
	}

	/**
	 * Gets the document descriptor, with support for caching descriptors. The
	 * cached descriptor will be reset if the document file is updated since the
	 * last call. In order to avoid checking this every time this method is
	 * called, use {@link #setMinimumTimeBetweenValidityChecks(long)}.
	 * 
	 * @param documentName
	 *        The document name
	 * @param read
	 *        Whether to read the source code from the file
	 * @return The document descriptor
	 * @throws DocumentException
	 */
	public DocumentDescriptor<D> getDocument( String documentName, boolean read ) throws DocumentException
	{
		// See if we already have a descriptor for this name
		FiledDocumentDescriptor<D> filedDocumentDescriptor = filedDocumentDescriptorsByAlias.get( documentName );
		if( filedDocumentDescriptor != null )
			filedDocumentDescriptor = validateDocumentDescriptor( documentName, filedDocumentDescriptor );

		if( filedDocumentDescriptor == null )
		{
			File file = getFileForDocumentName( documentName );

			// See if we already have a descriptor for this file
			filedDocumentDescriptor = filedDocumentDescriptorsByFile.get( file );
			if( filedDocumentDescriptor != null )
				filedDocumentDescriptor = validateDocumentDescriptor( null, filedDocumentDescriptor );

			if( filedDocumentDescriptor == null )
			{
				// Create a new descriptor
				filedDocumentDescriptor = new FiledDocumentDescriptor<D>( this, file, read );
				FiledDocumentDescriptor<D> existing = filedDocumentDescriptorsByFile.putIfAbsent( file, filedDocumentDescriptor );
				if( existing != null )
					filedDocumentDescriptor = existing;
				else
					// This is atomically safe, because we'll only get here once
					// (due to the putIfAbsent above)
					filedDocumentDescriptorsByAlias.put( documentName, filedDocumentDescriptor );
			}
		}

		if( !filedDocumentDescriptor.file.exists() )
			throw new DocumentNotFoundException( "Could not find file for " + documentName );

		return filedDocumentDescriptor;
	}

	//
	// DocumentSource
	//

	/**
	 * @see DocumentSource#getDocument(String)
	 */
	public DocumentDescriptor<D> getDocument( String documentName ) throws DocumentException
	{
		return getDocument( documentName, true );
	}

	/**
	 * @see DocumentSource#setDocument(String, String, String, Object)
	 */
	public DocumentDescriptor<D> setDocument( String documentName, String sourceCode, String tag, D document ) throws DocumentException
	{
		return filedDocumentDescriptorsByAlias.put( documentName, new FiledDocumentDescriptor<D>( this, documentName, sourceCode, tag, document ) );
	}

	/**
	 * @see DocumentSource#setDocumentIfAbsent(String, String, String, Object)
	 */
	public DocumentDescriptor<D> setDocumentIfAbsent( String documentName, String sourceCode, String tag, D document ) throws DocumentException
	{
		return filedDocumentDescriptorsByAlias.putIfAbsent( documentName, new FiledDocumentDescriptor<D>( this, documentName, sourceCode, tag, document ) );
	}

	/**
	 * @see DocumentSource#getDocuments()
	 */
	public Collection<DocumentDescriptor<D>> getDocuments()
	{
		return collectDocumentDescriptors( basePath );
	}

	/**
	 * @see DocumentSource#getIdentifier()
	 */
	public String getIdentifier()
	{
		return identifier;
	}

	//
	// Object
	//

	@Override
	public String toString()
	{
		return "DocumentFileSource: " + identifier + ", " + basePath + ", " + defaultName + ", " + preferredExtension + ", " + minimumTimeBetweenValidityChecks;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The document descriptors.
	 */
	private final ConcurrentMap<String, FiledDocumentDescriptor<D>> filedDocumentDescriptorsByAlias = new ConcurrentHashMap<String, FiledDocumentDescriptor<D>>();

	/**
	 * The document descriptors.
	 */
	private final ConcurrentMap<File, FiledDocumentDescriptor<D>> filedDocumentDescriptorsByFile = new ConcurrentHashMap<File, FiledDocumentDescriptor<D>>();

	/**
	 * The base path.
	 */
	private final File basePath;

	/**
	 * The source identifier.
	 */
	private final String identifier;

	/**
	 * If the name used in {@link #getDocument(String)} points to a directory,
	 * then this file name in that directory will be used instead. If an
	 * extension is not specified, then the preferred extension will be used.
	 * 
	 * @see #preferredExtension
	 */
	private volatile String defaultName;

	/**
	 * An extension to prefer if more than one file with the same name is in a
	 * directory.
	 */
	private volatile String preferredExtension;

	/**
	 * See {@link #getMinimumTimeBetweenValidityChecks()}
	 */
	private volatile long minimumTimeBetweenValidityChecks;

	/**
	 * Recursively collects document descriptors for all files under a base
	 * path.
	 * 
	 * @param basePath
	 *        The base path
	 * @return The document descriptors
	 */
	private Collection<DocumentDescriptor<D>> collectDocumentDescriptors( File basePath )
	{
		ArrayList<DocumentDescriptor<D>> list = new ArrayList<DocumentDescriptor<D>>();

		File[] files = basePath.listFiles();
		if( files != null )
		{
			for( File file : files )
			{
				if( file.isHidden() )
					continue;

				if( file.isDirectory() )
					// Recurse
					list.addAll( collectDocumentDescriptors( file ) );
				else
				{
					FiledDocumentDescriptor<D> filedDocumentDescriptor = filedDocumentDescriptorsByFile.get( file );
					if( filedDocumentDescriptor == null )
					{
						try
						{
							filedDocumentDescriptor = new FiledDocumentDescriptor<D>( this, file, true );
							FiledDocumentDescriptor<D> existing = filedDocumentDescriptorsByFile.putIfAbsent( file, filedDocumentDescriptor );
							if( existing != null )
								filedDocumentDescriptor = existing;
						}
						catch( DocumentException x )
						{
							// Silently skip problem files
						}
					}
					list.add( filedDocumentDescriptor );
				}
			}
		}

		return list;
	}

	/**
	 * Filters all filenames while ignoring their extension.
	 * 
	 * @author Tal Liron
	 */
	private static class ExtensionInsensitiveFilter implements FilenameFilter
	{
		private final String nameWithoutExtension;

		private final int nameWithoutExtensionLength;

		private ExtensionInsensitiveFilter( String nameWithoutExtension )
		{
			this.nameWithoutExtension = nameWithoutExtension;
			nameWithoutExtensionLength = nameWithoutExtension.length();
		}

		public boolean accept( File dir, String name )
		{
			if( name.startsWith( nameWithoutExtension ) )
			{
				int lastPeriod = name.lastIndexOf( '.' );
				if( ( lastPeriod == -1 ) || ( lastPeriod == nameWithoutExtensionLength ) )
					return true;
			}
			return false;
		}
	}

	/**
	 * Filters all filenames while ignoring their extension.
	 */
	private final ExtensionInsensitiveFilter defaultNameFilter;

	/**
	 * Returns a non-directory file, treating the document name as if it were a
	 * path under our base path. If the path specifies a directory, the file
	 * with default name under that directory is used.
	 * 
	 * @param documentName
	 *        The document name
	 * @return The file
	 * @throws DocumentNotFoundException
	 */
	private File getFileForDocumentName( String documentName ) throws DocumentNotFoundException
	{
		File file = new File( basePath, documentName );

		if( ( defaultName != null ) && file.isDirectory() )
		{
			// Return a file with the default name

			File[] filesWithDefaultName = file.listFiles( defaultNameFilter );
			if( ( filesWithDefaultName != null ) && ( filesWithDefaultName.length > 0 ) )
			{
				// Look for preferred extension
				String preferredExtension = this.preferredExtension;
				if( preferredExtension != null )
					for( File fileWithDefaultName : filesWithDefaultName )
						if( fileWithDefaultName.getName().endsWith( preferredExtension ) )
							return fileWithDefaultName;

				// Default to first found
				return filesWithDefaultName[0];
			}
			else
				return new File( file, defaultName );
		}
		else if( !file.exists() )
		{
			// Return a file with our name

			File directory = ScripturianUtil.getNormalizedFile( file.getParentFile() );
			File[] filesWithName = directory.listFiles( new ExtensionInsensitiveFilter( file.getName() ) );
			if( ( filesWithName != null ) && ( filesWithName.length > 0 ) )
			{
				// Look for preferred extension
				String preferredExtension = this.preferredExtension;
				if( preferredExtension != null )
					for( File fileWithName : filesWithName )
						if( fileWithName.getName().endsWith( preferredExtension ) )
							return fileWithName;

				// Default to first found
				return filesWithName[0];
			}

			// No file
			throw new DocumentNotFoundException( "File does not exist: " + file.getPath() );
		}

		return file;
	}

	/**
	 * Removes a document descriptor if it is invalid. Not absolutely necessary,
	 * but can save some memory.
	 * 
	 * @param documentName
	 *        The document name
	 * @param filedDocumentDescriptor
	 *        The document descriptor
	 * @return The document descriptor or null if it was removed
	 */
	private FiledDocumentDescriptor<D> validateDocumentDescriptor( String documentName, FiledDocumentDescriptor<D> filedDocumentDescriptor )
	{
		if( !filedDocumentDescriptor.isValid() )
		{
			// Remove by alias
			if( documentName != null )
				filedDocumentDescriptorsByAlias.remove( documentName, filedDocumentDescriptor );

			// Remove by file
			if( filedDocumentDescriptor.file != null )
				filedDocumentDescriptorsByFile.remove( filedDocumentDescriptor.file, filedDocumentDescriptor );

			filedDocumentDescriptor = null;
		}

		return filedDocumentDescriptor;
	}
}
