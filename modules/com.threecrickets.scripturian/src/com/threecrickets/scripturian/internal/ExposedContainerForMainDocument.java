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

package com.threecrickets.scripturian.internal;

import java.io.File;
import java.io.IOException;

import javax.script.ScriptException;

import com.threecrickets.scripturian.Document;
import com.threecrickets.scripturian.DocumentContext;
import com.threecrickets.scripturian.DocumentSource;
import com.threecrickets.scripturian.MainDocument;

/**
 * This is the <code>document.container</code> variable exposed to scriptlets.
 * 
 * @author Tal Liron
 * @see MainDocument
 */
public class ExposedContainerForMainDocument
{
	//
	// Construction
	//

	public ExposedContainerForMainDocument( MainDocument mainDocument )
	{
		this.mainDocument = mainDocument;
		documentContext = new DocumentContext( mainDocument.getScriptEngineManager() );
	}

	//
	// Operations
	//

	/**
	 * This powerful method allows scriptlets to execute other documents in
	 * place, and is useful for creating large, maintainable applications based
	 * on documents. Included documents can act as a library or toolkit and can
	 * even be shared among many applications. The included document does not
	 * have to be in the same programming language or use the same engine as the
	 * calling scriptlet. However, if they do use the same engine, then methods,
	 * functions, modules, etc., could be shared.
	 * <p>
	 * It is important to note that how this works varies a lot per engine. For
	 * example, in JRuby, every scriptlet is run in its own scope, so that
	 * sharing would have to be done explicitly in the global scope. See the
	 * included JRuby examples for a discussion of various ways to do this.
	 * 
	 * @param name
	 *        The document name
	 * @throws IOException
	 * @throws ScriptException
	 */
	public void include( String name ) throws IOException, ScriptException
	{
		include( name, null );
	}

	/**
	 * As {@link #include(String)}, except that the document is parsed as a
	 * single, non-delimited scriptlet. As such, you must explicitly specify the
	 * name of the scripting engine that should evaluate it.
	 * 
	 * @param name
	 *        The document name
	 * @param engineName
	 *        The script engine name (if null, behaves identically to
	 *        {@link #include(String)}
	 * @throws IOException
	 * @throws ScriptException
	 */
	public void include( String name, String engineName ) throws IOException, ScriptException
	{
		String text = ScripturianUtil.getString( new File( name ) );
		if( engineName != null )
			text = Document.DEFAULT_DELIMITER1_START + engineName + " " + text + Document.DEFAULT_DELIMITER1_END;

		DocumentSource.DocumentDescriptor<Document> documentDescriptor = mainDocument.getDocumentSource().getDocumentDescriptor( name );
		Document document = documentDescriptor.getDocument();
		if( document == null )
		{
			document = new Document( text, mainDocument.getScriptEngineManager(), getDefaultEngineName(), mainDocument.getDocumentSource(), mainDocument.isAllowCompilation() );
			Document existing = documentDescriptor.setDocumentIfAbsent( document );
			if( existing != null )
				document = existing;
		}

		document.run( false, mainDocument.getWriter(), mainDocument.getErrorWriter(), true, documentContext, this, mainDocument.getScriptletController() );
	}

	//
	// Attributes
	//

	/**
	 * The arguments sent to {@link MainDocument#main(String[])}.
	 * 
	 * @return The arguments
	 */
	public String[] getArguments()
	{
		return mainDocument.getArguments();
	}

	/**
	 * The default script engine name to be used if the first scriptlet doesn't
	 * specify one. Defaults to "js".
	 * 
	 * @return The default script engine name
	 * @see #setDefaultEngineName(String)
	 */
	public String getDefaultEngineName()
	{
		return defaultEngineName;
	}

	/**
	 * @param defaultEngineName
	 *        The default script engine name
	 * @see #getDefaultEngineName()
	 */
	public void setDefaultEngineName( String defaultEngineName )
	{
		this.defaultEngineName = defaultEngineName;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final MainDocument mainDocument;

	private final DocumentContext documentContext;

	private String defaultEngineName = "js";
}
