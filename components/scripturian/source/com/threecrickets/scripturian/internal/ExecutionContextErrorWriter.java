/**
 * Copyright 2009-2014 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.internal;

import java.io.Writer;

import com.threecrickets.scripturian.ExecutionContext;

/**
 * @author Tal Liron
 */
public class ExecutionContextErrorWriter extends ExecutionContextWriter
{
	// //////////////////////////////////////////////////////////////////////////
	// Protected

	//
	// ExecutionContextWriter
	//

	@Override
	protected Writer getWriter()
	{
		ExecutionContext executionContext = ExecutionContext.getCurrent();
		return executionContext.getErrorWriterOrDefault();
	}
}
