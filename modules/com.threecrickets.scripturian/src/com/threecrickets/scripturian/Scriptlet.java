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

import com.threecrickets.scripturian.exception.CompilationException;
import com.threecrickets.scripturian.exception.ExecutableInitializationException;
import com.threecrickets.scripturian.exception.ExecutionException;

/**
 * @author Tal Liron
 */
public interface Scriptlet
{
	//
	// Attributes
	//

	public String getSourceCode();

	//
	// Operations
	//

	public void compile() throws CompilationException;

	public Object execute( ExecutionContext executionContext ) throws ExecutableInitializationException, ExecutionException;
}
