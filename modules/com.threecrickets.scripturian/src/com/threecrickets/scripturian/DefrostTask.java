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

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * A {@link Callable} that makes sure that a {@link Executable} is tied to a
 * {@link DocumentDescriptor}, making it ready to use without the delay of
 * initialization, parsing, compilation, etc.
 * <p>
 * It may be easier to use a {@link Defroster}.
 * 
 * @author Tal Liron
 * @see Defroster
 */
public class DefrostTask implements Callable<Executable>
{
	//
	// Static operations
	//

	/**
	 * Creates a defrost task for each document descriptor in a document source.
	 * 
	 * @param documentSource
	 *        The document source
	 * @param manager
	 *        The script engine manager to use for document initialization
	 * @param allowCompilation
	 *        Whether to allow compilation for initialized documents
	 * @return An array of tasks
	 * @see DocumentSource#getDocumentDescriptor(String)
	 */
	public static DefrostTask[] forDocumentSource( DocumentSource<Executable> documentSource, LanguageManager manager, boolean allowCompilation )
	{
		Collection<DocumentDescriptor<Executable>> documentDescriptors = documentSource.getDocumentDescriptors();
		DefrostTask[] defrostTasks = new DefrostTask[documentDescriptors.size()];
		int i = 0;
		for( DocumentDescriptor<Executable> documentDescriptor : documentDescriptors )
			defrostTasks[i++] = new DefrostTask( documentDescriptor, documentSource, manager, allowCompilation );

		return defrostTasks;
	}

	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param documentDescriptor
	 *        The document descriptor
	 * @param documentSource
	 *        The document source, required for processing in-flow tags during
	 *        document initialization
	 * @param manager
	 *        The script engine manager to use for document initialization
	 * @param allowCompilation
	 *        Whether to allow compilation for initialized documents
	 */
	public DefrostTask( DocumentDescriptor<Executable> documentDescriptor, DocumentSource<Executable> documentSource, LanguageManager manager, boolean allowCompilation )
	{
		this.documentDescriptor = documentDescriptor;
		this.documentSource = documentSource;
		this.manager = manager;
		this.allowCompilation = allowCompilation;
	}

	//
	// Callable
	//

	public Executable call() throws Exception
	{
		Executable document = documentDescriptor.getDocument();

		if( document == null )
		{
			LanguageAdapter scriptEngine = manager.getAdapterByExtension( documentDescriptor.getDefaultName(), documentDescriptor.getTag() );
			String text = documentDescriptor.getText();
			document = new Executable( documentDescriptor.getDefaultName(), text, false, manager, (String) scriptEngine.getAttributes().get( LanguageAdapter.DEFAULT_TAG ), documentSource, allowCompilation );

			Executable existing = documentDescriptor.setDocumentIfAbsent( document );
			if( existing != null )
				document = existing;
		}

		return document;
	}

	//
	// Object
	//

	@Override
	public String toString()
	{
		return documentDescriptor.getDefaultName();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The document descriptor.
	 */
	private final DocumentDescriptor<Executable> documentDescriptor;

	/**
	 * The document source, required for processing in-flow tags during document
	 * initialization.
	 */
	private final DocumentSource<Executable> documentSource;

	/**
	 * The script engine manager to use for document initialization.
	 */
	private final LanguageManager manager;

	/**
	 * Whether to allow compilation for initialized documents.
	 */
	private final boolean allowCompilation;
}
