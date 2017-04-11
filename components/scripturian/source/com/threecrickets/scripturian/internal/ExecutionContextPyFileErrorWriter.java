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

package com.threecrickets.scripturian.internal;

import org.python.core.PyFileWriter;

import com.threecrickets.scripturian.ExecutionContext;

/**
 * Like {@link ExecutionContextPyFileWriter}, but for the error writer.
 * 
 * @author Tal Liron
 */
public class ExecutionContextPyFileErrorWriter extends ExecutionContextPyFileWriter
{
	//
	// Constants
	//

	public static final String WRITER = ExecutionContextPyFileErrorWriter.class.getCanonicalName() + ".writer";

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	//
	// ExecutionContextPyFileWriter
	//

	@Override
	protected PyFileWriter getWriter()
	{
		ExecutionContext executionContext = ExecutionContext.getCurrent();
		PyFileWriter writer = (PyFileWriter) executionContext.getAttributes().get( WRITER );
		if( writer == null )
		{
			writer = new PyFileWriter( executionContext.getErrorWriterOrDefault() );
			try
			{
				executionContext.getAttributes().put( WRITER, writer );
			}
			catch( UnsupportedOperationException x )
			{
				// Immutable
			}
		}
		return writer;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;
}
