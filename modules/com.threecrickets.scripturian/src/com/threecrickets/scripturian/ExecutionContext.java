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

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates context for an {@link Executable}. Every thread calling
 * {@link Executable#execute(boolean, Writer, Writer, boolean, ExecutionContext, Object, ExecutionController)}
 * must use its own context.
 * 
 * @author Tal Liron
 */
public class ExecutionContext
{
	//
	// Construction
	//

	public ExecutionContext( LanguageManager languageManager )
	{
		this.languageManager = languageManager;
	}

	//
	// Attributes
	//

	/**
	 * @return The attributes
	 */
	public Map<String, Object> getAttributes()
	{
		return attributes;
	}

	/**
	 * @return The exposed variables
	 */
	public Map<String, Object> getExposedVariables()
	{
		return exposedVariables;
	}

	/**
	 * @return The writer
	 */
	public Writer getWriter()
	{
		return writer;
	}

	/**
	 * @param writer
	 * @return The previous writer
	 */
	public Writer setWriter( Writer writer )
	{
		Writer old = this.writer;
		this.writer = writer;
		return old;
	}

	/**
	 * @param writer
	 * @param flushLines
	 * @return The previous writer
	 */
	public Writer setWriter( Writer writer, boolean flushLines )
	{
		// Note that some script engines (such as Rhino) expect a
		// PrintWriter, even though the spec defines just a Writer
		return setWriter( new PrintWriter( writer, flushLines ) );
	}

	/**
	 * @return The error writer
	 */
	public Writer getErrorWriter()
	{
		return errorWriter;
	}

	/**
	 * @param writer
	 * @return The previous error writer
	 */
	public Writer setErrorWriter( Writer writer )
	{
		Writer old = this.errorWriter;
		this.errorWriter = writer;
		return old;
	}

	/**
	 * @param writer
	 * @param flushLines
	 * @return The previous error writer
	 */
	public Writer setErrorWriter( Writer writer, boolean flushLines )
	{
		// Note that some script engines (such as Rhino) expect a
		// PrintWriter, even though the spec defines just a Writer
		return setErrorWriter( new PrintWriter( writer, flushLines ) );
	}

	/**
	 * @return The language adapter
	 */
	public LanguageAdapter getAdapter()
	{
		return languageAdapter;
	}

	/**
	 * @param adapter
	 */
	public void setAdapter( LanguageAdapter adapter )
	{
		this.languageAdapter = adapter;
	}

	/**
	 * @return The language manager
	 */
	public LanguageManager getManager()
	{
		return languageManager;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private Map<String, Object> attributes = new HashMap<String, Object>();

	private Map<String, Object> exposedVariables = new HashMap<String, Object>();

	private Writer writer;

	private Writer errorWriter;

	private LanguageManager languageManager;

	private LanguageAdapter languageAdapter;
}
