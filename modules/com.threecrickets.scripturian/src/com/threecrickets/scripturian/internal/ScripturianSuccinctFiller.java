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
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.adapter.SuccinctAdapter;
import com.threecrickets.succinct.CastException;
import com.threecrickets.succinct.Filler;
import com.threecrickets.succinct.filler.BeanMapFillerWrapper;

/**
 * A {@link Filler} that supports Scripturian inclusion scriptlets and getting
 * values from services via the Java bean mechanism.
 * 
 * @author Tal Liron
 */
public class ScripturianSuccinctFiller extends BeanMapFillerWrapper
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param manager
	 *        The language manager
	 * @param executable
	 *        The executable
	 * @param executionContext
	 *        The execution context
	 */
	public ScripturianSuccinctFiller( LanguageManager manager, Executable executable, ExecutionContext executionContext )
	{
		super( executionContext.getServices() );
		this.manager = manager;
		this.executable = executable;
		this.executionContext = executionContext;
	}

	//
	// Filler
	//

	@Override
	public Object getValue( String key ) throws CastException
	{
		if( key.startsWith( SuccinctAdapter.INCLUSION_KEY ) )
		{
			String documentName = key.substring( SuccinctAdapter.INCLUSION_KEY.length() );

			// We don't need to return a value, because include already writes
			// to our writer
			ScripturianUtil.containerInclude( manager, executable, executionContext, documentName );

			// We need to return something in order to be considered cast
			return "";
		}
		else
			return super.getValue( key );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The language manager.
	 */
	private final LanguageManager manager;

	/**
	 * The executable.
	 */
	private final Executable executable;

	/**
	 * The execution context.
	 */
	private final ExecutionContext executionContext;
}
