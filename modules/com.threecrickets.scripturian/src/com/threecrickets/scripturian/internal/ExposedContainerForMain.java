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

import java.io.IOException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.Main;
import com.threecrickets.scripturian.document.DocumentDescriptor;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.ExecutionException;

/**
 * This is the <code>document.container</code> variable exposed to scriptlets.
 * 
 * @author Tal Liron
 * @see Main
 */
public class ExposedContainerForMain
{
	//
	// Construction
	//

	/**
	 * @param mainDocument
	 * @param executionContext
	 */
	public ExposedContainerForMain( Main mainDocument, ExecutionContext executionContext )
	{
		this.mainDocument = mainDocument;
		this.executionContext = executionContext;
	}

	//
	// Attributes
	//

	//
	// Operations
	//

	/**
	 * This powerful method allows executables to execute other executables in
	 * place, and is useful for creating large, maintainable applications based
	 * on executables. Included executables can act as a library or toolkit and
	 * can even be shared among many applications. The included executable does
	 * not have to be in the same programming language or use the same engine as
	 * the calling executable. However, if they do use the same engine, then
	 * methods, functions, modules, etc., could be shared.
	 * 
	 * @param name
	 *        The document name
	 * @throws IOException
	 * @throws ParsingException
	 * @throws ExecutionException
	 */
	public void includeDocument( String name ) throws IOException, ParsingException, ExecutionException
	{
		DocumentDescriptor<Executable> documentDescriptor = mainDocument.getDocumentSource().getDocument( name );
		Executable document = documentDescriptor.getDocument();
		if( document == null )
		{
			String text = documentDescriptor.getSourceCode();
			document = new Executable( name, text, true, mainDocument.getManager(), getDefaultEngineName(), mainDocument.getDocumentSource(), mainDocument.isAllowCompilation() );

			Executable existing = documentDescriptor.setDocumentIfAbsent( document );
			if( existing != null )
				document = existing;
		}

		document.execute( false, executionContext, this, mainDocument.getScriptletController() );
	}

	/**
	 * As {@link #includeDocument(String)}, except that the document is parsed
	 * as a single, non-delimited script with the engine name derived from the
	 * document descriptor's tag.
	 * 
	 * @param name
	 *        The document name
	 * @throws IOException
	 * @throws ParsingException
	 * @throws ExecutionException
	 */
	public void include( String name ) throws IOException, ParsingException, ExecutionException
	{
		DocumentDescriptor<Executable> documentDescriptor = mainDocument.getDocumentSource().getDocument( name );
		Executable document = documentDescriptor.getDocument();
		if( document == null )
		{
			LanguageAdapter adapter = mainDocument.getManager().getAdapterByExtension( name, documentDescriptor.getTag() );
			String text = documentDescriptor.getSourceCode();
			document = new Executable( name, text, false, mainDocument.getManager(), (String) adapter.getAttributes().get( LanguageAdapter.DEFAULT_TAG ), mainDocument.getDocumentSource(), mainDocument
				.isAllowCompilation() );

			Executable existing = documentDescriptor.setDocumentIfAbsent( document );
			if( existing != null )
				document = existing;
		}

		document.execute( false, executionContext, this, mainDocument.getScriptletController() );
	}

	//
	// Attributes
	//

	/**
	 * The arguments sent to {@link Main#main(String[])}.
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

	private final Main mainDocument;

	private final ExecutionContext executionContext;

	private String defaultEngineName = "js";
}
