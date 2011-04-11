/**
 * Copyright 2009-2011 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.service;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Main;
import com.threecrickets.scripturian.ParsingContext;
import com.threecrickets.scripturian.document.DocumentDescriptor;
import com.threecrickets.scripturian.document.DocumentFileSource;
import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.exception.DocumentException;
import com.threecrickets.scripturian.exception.DocumentNotFoundException;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * This is the <code>document</code> service exposed by {@link Main}.
 * 
 * @author Tal Liron
 * @see Main
 */
public class DocumentService
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @param main
	 *        The main instance
	 * @param executionContext
	 *        The execution context
	 */
	public DocumentService( Main main, ExecutionContext executionContext )
	{
		this.main = main;
		this.executionContext = executionContext;
	}

	//
	// Attributes
	//

	/**
	 * The document source.
	 * 
	 * @return The document source
	 */
	public DocumentSource<Executable> getSource()
	{
		return main.getSource();
	}

	/**
	 * The additional document sources to use.
	 * 
	 * @return The library document sources
	 */
	public CopyOnWriteArrayList<DocumentSource<Executable>> getLibrarySources()
	{
		return main.getLibrarySources();
	}

	/**
	 * For use with {@link #include(String)} and {@link #execute(String)}, this
	 * is the default language tag used for scriptlets in case none is
	 * specified. Defaults to "js".
	 * 
	 * @return The default script language tag
	 * @see #setDefaultLanguageTag(String)
	 */
	public String getDefaultLanguageTag()
	{
		return defaultLanguageTag;
	}

	/**
	 * @param defaultLanguageTag
	 *        The default language tag
	 * @see #getDefaultLanguageTag()
	 */
	public void setDefaultLanguageTag( String defaultLanguageTag )
	{
		this.defaultLanguageTag = defaultLanguageTag;
	}

	/**
	 * An extension to prefer if more than one file with the same name is in a
	 * directory. Only valid is the source is a {@link DocumentFileSource}.
	 * 
	 * @return The preferred extension
	 * @see #setPreferredExtension(String)
	 */
	public String getPreferredExtension()
	{
		DocumentSource<Executable> source = main.getSource();
		if( source instanceof DocumentFileSource<?> )
			return ( (DocumentFileSource<Executable>) source ).getPreferredExtension();
		else
			return null;
	}

	/**
	 * @param preferredExtension
	 *        The preferred extension
	 * @see #getPreferredExtension()
	 */
	public void setPreferredExtension( String preferredExtension )
	{
		DocumentSource<Executable> source = main.getSource();
		if( source instanceof DocumentFileSource<?> )
			( (DocumentFileSource<Executable>) source ).setPreferredExtension( preferredExtension );
	}

	//
	// Operations
	//

	/**
	 * Executes a source code document. The language of the source code will be
	 * determined by the document tag, which is usually the filename extension.
	 * 
	 * @param documentName
	 *        The document name
	 * @throws ParsingException
	 * @throws ExecutionException
	 * @throws DocumentException
	 * @throws IOException
	 * @see LanguageManager#getAdapterByExtension(String, String)
	 */
	public void execute( String documentName ) throws ParsingException, ExecutionException, DocumentException, IOException
	{
		Executable executable = getDocumentDescriptor( documentName, false ).getDocument();
		executable.execute( executionContext, this, main.getExecutionController() );
	}

	/**
	 * Includes a text document into the current location. The document may be a
	 * "text-with-scriptlets" executable, in which case its output could be
	 * dynamically generated.
	 * 
	 * @param documentName
	 *        The document name
	 * @throws ParsingException
	 * @throws ExecutionException
	 * @throws DocumentException
	 * @throws IOException
	 */
	public void include( String documentName ) throws ParsingException, ExecutionException, DocumentException, IOException
	{
		Executable executable = getDocumentDescriptor( documentName, true ).getDocument();
		executable.execute( executionContext, this, main.getExecutionController() );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The main instance.
	 */
	private final Main main;

	/**
	 * The execution context.
	 */
	private final ExecutionContext executionContext;

	/**
	 * The default language tag.
	 */
	private String defaultLanguageTag = "js";

	/**
	 * Fetches a document descriptor from the main source or one of the library
	 * sources.
	 * 
	 * @param documentName
	 *        The document name
	 * @param isTextWithScriplets
	 *        Whether the document is text-with-scriptlets
	 * @return The document descriptor
	 * @throws ParsingException
	 * @throws DocumentException
	 */
	private DocumentDescriptor<Executable> getDocumentDescriptor( String documentName, boolean isTextWithScriplets ) throws ParsingException, DocumentException
	{
		Iterator<DocumentSource<Executable>> iterator = null;

		ParsingContext parsingContext = new ParsingContext();
		parsingContext.setLanguageManager( main.getLanguageManager() );
		parsingContext.setDefaultLanguageTag( defaultLanguageTag );
		parsingContext.setPrepare( main.isPrepare() );
		parsingContext.setDocumentSource( getSource() );

		while( true )
		{
			try
			{
				return Executable.createOnce( documentName, isTextWithScriplets, parsingContext );
			}
			catch( DocumentNotFoundException x )
			{
				if( iterator == null )
				{
					Iterable<DocumentSource<Executable>> sources = main.getLibrarySources();
					iterator = sources != null ? sources.iterator() : null;
				}

				if( ( iterator == null ) || !iterator.hasNext() )
					throw x;

				parsingContext.setDocumentSource( iterator.next() );
			}
		}
	}
}
