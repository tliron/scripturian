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

import com.threecrickets.scripturian.exception.PreparationException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.ExecutionException;

/**
 * @author Tal Liron
 */
public interface Scriptlet
{
	//
	// Attributes
	//

	/**
	 * @return
	 */
	public String getSourceCode();

	//
	// Operations
	//

	/**
	 * @throws PreparationException
	 */
	public void prepare() throws PreparationException;

	/**
	 * @param executionContext
	 * @return
	 * @throws ParsingException
	 * @throws ExecutionException
	 */
	public Object execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException;
}
