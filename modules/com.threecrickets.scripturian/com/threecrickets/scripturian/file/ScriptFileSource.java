/**
 * Copyright 2009 Three Crickets.
 * <p>
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * <p>
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * <p>
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * <p>
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * <p>
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
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

import com.threecrickets.scripturian.ScriptSource;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * Reads script files stored in files under a base directory. The file contents
 * are cached, and checked for validity according to their modification
 * timestamps.
 * 
 * @author Tal Liron
 */
public class ScriptFileSource<S> implements ScriptSource<S>
{
	//
	// Construction
	//

	/**
	 * Constructs a script file source.
	 * 
	 * @param basePath
	 *        The base path
	 * @param defaultName
	 *        If the name used in {@link #getScriptDescriptor(String)} points to
	 *        a directory, then this file name in that directory will be used
	 *        instead
	 * @param defaultExtension
	 *        If the name used in {@link #getScriptDescriptor(String)} does not
	 *        point to a valid file or directory, then this extension will be
	 *        added and the name will be tested for again
	 * @param minimumTimeBetweenValidityChecks
	 *        See {@link #getMinimumTimeBetweenValidityChecks()}
	 */
	public ScriptFileSource( File basePath, String defaultName, String defaultExtension, long minimumTimeBetweenValidityChecks )
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
	 * If the name used in {@link #getScriptDescriptor(String)} points to a
	 * directory, then this file name in that directory will be used instead
	 * 
	 * @return The default name
	 */
	public String getDefaultName()
	{
		return defaultName;
	}

	/**
	 * If the name used in {@link #getScriptDescriptor(String)} does not point
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
	// ScriptSource
	//

	/**
	 * This implementation caches the script descriptor, including the script
	 * instance stored in it. The cached descriptor will be reset if the script
	 * file is updated since the last call. In order to avoid checking this
	 * every time this method is called, use
	 * {@link #setMinimumTimeBetweenValidityChecks(long)}.
	 * 
	 * @see ScriptSource#getScriptDescriptor(String)
	 */
	public ScriptDescriptor<S> getScriptDescriptor( String name ) throws IOException
	{
		File file = new File( basePath, name );

		if( ( defaultName != null ) && file.isDirectory() )
			file = new File( file, defaultName );
		else if( ( defaultExtension != null ) && !file.exists() )
			file = new File( basePath, name + "." + defaultExtension );

		name = file.getPath();

		FiledScriptDescriptor filedScriptDescriptor = scriptDescriptors.get( name );

		if( filedScriptDescriptor != null )
		{
			if( !filedScriptDescriptor.isValid( file ) )
				filedScriptDescriptor = null;
		}

		if( filedScriptDescriptor == null )
		{
			filedScriptDescriptor = new FiledScriptDescriptor( file );
			FiledScriptDescriptor existing = scriptDescriptors.putIfAbsent( name, filedScriptDescriptor );
			if( existing != null )
				filedScriptDescriptor = existing;
		}

		return filedScriptDescriptor;
	}

	//
	// Attributes
	//

	/**
	 * Attempts to call {@link #getScriptDescriptor(String)} for a specific name
	 * within less than this time from the previous call will return the cached
	 * descriptor without checking if it is valid.
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

	private final ConcurrentMap<String, FiledScriptDescriptor> scriptDescriptors = new ConcurrentHashMap<String, FiledScriptDescriptor>();

	private final File basePath;

	private final String defaultName;

	private final String defaultExtension;

	private final AtomicLong minimumTimeBetweenValidityChecks = new AtomicLong();

	private class FiledScriptDescriptor implements ScriptDescriptor<S>
	{
		public String getText()
		{
			return content;
		}

		public String getTag()
		{
			return extension;
		}

		public synchronized S getScript()
		{
			return script;
		}

		public synchronized void setScript( S script )
		{
			this.script = script;
		}

		private FiledScriptDescriptor( File file ) throws IOException
		{
			content = ScripturianUtil.getString( file );

			String name = file.getName();
			int dot = name.lastIndexOf( '.' );
			if( dot != -1 )
				extension = name.substring( dot + 1 );
			else
				extension = null;

			timestamp = file.lastModified();
		}

		private boolean isValid( File file )
		{
			long now = System.currentTimeMillis();
			if( now - lastValidityCheck.get() > minimumTimeBetweenValidityChecks.get() )
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

		private final String content;

		private final String extension;

		private S script;
	}
}
