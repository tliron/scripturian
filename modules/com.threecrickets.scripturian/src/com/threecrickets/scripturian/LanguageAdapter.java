package com.threecrickets.scripturian;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.threecrickets.scripturian.exception.ExecutableInitializationException;
import com.threecrickets.scripturian.exception.ExecutionException;

public abstract class LanguageAdapter
{
	public static final String NAME = "name";

	public static final String VERSION = "version";

	public static final String LANGUAGE_NAME = "language.name";

	public static final String LANGUAGE_VERSION = "language.version";

	public static final String EXTENSIONS = "extensions";

	public static final String DEFAULT_EXTENSION = "extension.default";

	public static final String TAGS = "tags";

	public static final String DEFAULT_TAG = "tag.default";

	public abstract Map<String, Object> getAttributes();

	/**
	 * Some languages or their script engines are inherently broken when called
	 * by multiple threads. Returning false here makes sure Scripturian respects
	 * this and blocks.
	 * 
	 * @return True if we support being run by concurrent threads
	 */
	public abstract boolean isThreadSafe();

	public abstract Scriptlet createScriptlet( String code, Executable document ) throws ExecutableInitializationException;

	public abstract String getCodeForLiteralOutput( String literal, Executable document ) throws ExecutableInitializationException;

	public abstract String getCodeForExpressionOutput( String expression, Executable document ) throws ExecutableInitializationException;

	public abstract String getCodeForExpressionInclude( String expression, Executable document ) throws ExecutableInitializationException;

	public abstract Throwable getCauseOrDocumentRunException( String documentName, Throwable throwable );

	public abstract Object invoke( String method, Executable document, ExecutionContext executionContext ) throws NoSuchMethodException, ExecutableInitializationException, ExecutionException;

	public final Lock lock = new ReentrantLock();
}
