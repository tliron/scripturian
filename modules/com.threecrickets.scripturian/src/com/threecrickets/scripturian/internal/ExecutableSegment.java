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

package com.threecrickets.scripturian.internal;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Scriptlet;
import com.threecrickets.scripturian.exception.ExecutableInitializationException;

/**
 * @author Tal Liron
 * @see Executable
 */
public class ExecutableSegment
{
	//
	// Construction
	//

	public ExecutableSegment( String code, boolean isScriptlet, String languageTag )
	{
		this.code = code;
		this.isScriptlet = isScriptlet;
		this.languageTag = languageTag;
	}

	//
	// Attributes
	//

	public final boolean isScriptlet;

	public final String languageTag;

	public String code;

	public Scriptlet scriptlet;

	//
	// Operations
	//

	public void createScriptlet( Executable executable, LanguageManager manager, boolean allowCompilation ) throws ExecutableInitializationException
	{
		LanguageAdapter adapter = manager.getAdapterByTag( languageTag );
		if( adapter == null )
			throw ExecutableInitializationException.adapterNotFound( executable.getName(), languageTag );

		scriptlet = adapter.createScriptlet( code, executable );

		if( allowCompilation )
			scriptlet.compile();
	}
}