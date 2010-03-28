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

import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.threecrickets.scripturian.exception.ExecutableInitializationException;
import com.threecrickets.scripturian.exception.ExecutionException;

/**
 * Allows execution of code in a supported source language.
 * <p>
 * Language adapters are usually accessed via a {@link LanguageManager}
 * instance.
 * 
 * @author Tal Liron
 * @see LanguageManager
 */
public interface LanguageAdapter
{
	//
	// Constants
	//

	public static final String NAME = "name";

	public static final String VERSION = "version";

	public static final String LANGUAGE_NAME = "language.name";

	public static final String LANGUAGE_VERSION = "language.version";

	public static final String EXTENSIONS = "extensions";

	public static final String DEFAULT_EXTENSION = "extension.default";

	public static final String TAGS = "tags";

	public static final String DEFAULT_TAG = "tag.default";

	//
	// Attributes
	//

	public Map<String, Object> getAttributes();

	/**
	 * Some languages or their script engines are inherently broken when called
	 * by multiple threads. Returning false here makes sure Scripturian respects
	 * this and blocks.
	 * 
	 * @return True if we support being run by concurrent threads
	 */
	public boolean isThreadSafe();

	public Lock getLock();

	public String getCodeForLiteralOutput( String literal, Executable document ) throws ExecutableInitializationException;

	public String getCodeForExpressionOutput( String expression, Executable document ) throws ExecutableInitializationException;

	public String getCodeForExpressionInclude( String expression, Executable document ) throws ExecutableInitializationException;

	public Throwable getCauseOrExecutionException( String documentName, Throwable throwable );

	//
	// Operations
	//

	public Scriptlet createScriptlet( String code, Executable document ) throws ExecutableInitializationException;

	public Object invoke( String method, Executable document, ExecutionContext executionContext ) throws NoSuchMethodException, ExecutableInitializationException, ExecutionException;
}
