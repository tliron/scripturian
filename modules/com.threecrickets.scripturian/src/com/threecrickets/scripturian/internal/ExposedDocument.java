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

import java.io.Writer;
import java.util.concurrent.ConcurrentMap;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.threecrickets.scripturian.Document;
import com.threecrickets.scripturian.DocumentContext;

/**
 * This is the <code>document</code> variable exposed to scriptlets. The name is
 * set according to {@link Document#getDocumentVariableName()}.
 * 
 * @author Tal Liron
 * @see Document
 */
public class ExposedDocument
{
	//
	// Construction
	//

	public ExposedDocument( Document document, DocumentContext documentContext, ScriptEngine scriptEngine, Object container )
	{
		this.document = document;
		this.documentContext = documentContext;
		this.scriptEngine = scriptEngine;
		this.container = container;
	}

	//
	// Attributes
	//

	/**
	 * Setting this to something greater than 0 enables caching of the
	 * document's output for a maximum number of milliseconds. By default
	 * cacheDuration is 0, so that each request causes the document to be run.
	 * This class does not handle caching itself. Caching can be provided by
	 * your environment if appropriate.
	 * 
	 * @return The cache duration in milliseconds
	 * @see #setCacheDuration(long)
	 * @see Document#cacheDuration
	 */
	public long getCacheDuration()
	{
		return document.getCacheDuration();
	}

	/**
	 * @param cacheDuration
	 *        The cache duration in milliseconds
	 * @see #getCacheDuration()
	 */
	public void setCacheDuration( long cacheDuration )
	{
		document.setCacheDuration( cacheDuration );
	}

	/**
	 * This is the {@link ScriptContext} used by the document. Scriptlets may
	 * use it to get access to the {@link Writer} objects used for standard
	 * output and standard error.
	 * 
	 * @return The script context
	 */
	public ScriptContext getContext()
	{
		return documentContext.getScriptContext();
	}

	/**
	 * This is the {@link ScriptEngine} used by the scriptlet. Scriptlets may
	 * use it to get information about the engine's capabilities.
	 * 
	 * @return The script engine
	 */
	public ScriptEngine getEngine()
	{
		return scriptEngine;
	}

	/**
	 * This is the {@link ScriptEngineManager} used to create all script engines
	 * in the document. Scriptlets may use it to get information about what
	 * other engines are available.
	 * 
	 * @return The script engine manager
	 */
	public ScriptEngineManager getEngineManager()
	{
		return documentContext.getScriptEngineManager();
	}

	/**
	 * The container.
	 * 
	 * @return The container (or null if none was provided)
	 */
	public Object getContainer()
	{
		return container;
	}

	/**
	 * This {@link ConcurrentMap} provides a convenient location for global
	 * values shared by all scriptlets in all documents.
	 * 
	 * @return The values
	 */
	public ConcurrentMap<String, Object> getMeta()
	{
		return MetaScope.getInstance().getValues();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final Document document;

	private final DocumentContext documentContext;

	private final ScriptEngine scriptEngine;

	private final Object container;
}