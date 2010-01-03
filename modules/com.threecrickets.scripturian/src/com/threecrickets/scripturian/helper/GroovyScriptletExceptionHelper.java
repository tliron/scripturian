/**
 * Copyright 2009 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://www.threecrickets.com/
 */
package com.threecrickets.scripturian.helper;

import javax.script.ScriptException;

import com.threecrickets.scripturian.ScriptletExceptionHelper;
import com.threecrickets.scripturian.exception.DocumentRunException;

/**
 * @author Tal Liron
 */
public class GroovyScriptletExceptionHelper implements ScriptletExceptionHelper
{
	//
	// ScriptletExceptionHelper
	//

	public DocumentRunException getDocumentRunException( String documentName, Exception exception )
	{
		// Groovy does a slightly more complex wrap
		if( exception instanceof ScriptException )
		{
			Throwable cause = exception.getCause();
			if( cause instanceof ScriptException )
			{
				// Likely a GroovyRuntimeException
				cause = cause.getCause();
				if( cause instanceof DocumentRunException )
					return (DocumentRunException) cause;

				// Wish there were a way to get line numbers from
				// GroovyRuntimeException!
			}
		}

		return null;
	}
}
