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

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;

/**
 * Delegates to {@link Main}, using the <a
 * href="http://commons.apache.org/daemon/">Apache Commons Daemon</a>.
 * 
 * @author Tal Liron
 */
public class ScripturianDaemon implements Daemon
{
	//
	// Daemon
	//

	public void init( DaemonContext context ) throws Exception
	{
		arguments = context.getArguments();
	}

	public void start() throws Exception
	{
		// Delegate to Main
		Main.main( arguments );
	}

	public void stop() throws Exception
	{
	}

	public void destroy()
	{
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private volatile String[] arguments;
}
