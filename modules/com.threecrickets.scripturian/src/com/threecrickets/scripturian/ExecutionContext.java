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
 * {@link Executable#execute(boolean, boolean, Writer, Writer, boolean, DocumentContext, Object, ScriptletController)}
 * must use its own context.
 * 
 * @author Tal Liron
 */
public class ExecutionContext
{
	public ExecutionContext( LanguageManager manager )
	{
		this.manager = manager;
	}

	//
	// Attributes
	//

	/**
	 * @return
	 */
	public Map<String, Object> getAttributes()
	{
		return attributes;
	}

	/**
	 * @return
	 */
	public Map<String, Object> getExposedVariables()
	{
		return exposedVariables;
	}

	/**
	 * @return
	 */
	public Writer getWriter()
	{
		return writer;
	}

	/**
	 * @param writer
	 * @return
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
	 * @return
	 */
	public Writer setWriter( Writer writer, boolean flushLines )
	{
		// Note that some script engines (such as Rhino) expect a
		// PrintWriter, even though the spec defines just a Writer
		return setWriter( new PrintWriter( writer, flushLines ) );
	}

	/**
	 * @return
	 */
	public Writer getErrorWriter()
	{
		return errorWriter;
	}

	/**
	 * @param writer
	 * @return
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
	 * @return
	 */
	public Writer setErrorWriter( Writer writer, boolean flushLines )
	{
		// Note that some script engines (such as Rhino) expect a
		// PrintWriter, even though the spec defines just a Writer
		return setErrorWriter( new PrintWriter( writer, flushLines ) );
	}

	public LanguageAdapter getAdapter()
	{
		return adapter;
	}

	public void setAdapter( LanguageAdapter adapter )
	{
		this.adapter = adapter;
	}

	public LanguageManager getManager()
	{
		return manager;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private Map<String, Object> attributes = new HashMap<String, Object>();

	private Map<String, Object> exposedVariables = new HashMap<String, Object>();

	private Writer writer;

	private Writer errorWriter;

	private LanguageManager manager;

	private LanguageAdapter adapter;
}
