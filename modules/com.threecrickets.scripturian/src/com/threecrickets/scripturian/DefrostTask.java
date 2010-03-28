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
 * It may be easier to use a {@link Defroster}, which is at a higher level than
 * this class.
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
	 *        The document source for executables
	 * @param languageManager
	 *        The language manager for executable initialization
	 * @param allowCompilation
	 *        Whether to compile executables
	 * @return An array of tasks
	 * @see DocumentSource#getDocument(String)
	 */
	public static DefrostTask[] forDocumentSource( DocumentSource<Executable> documentSource, LanguageManager languageManager, boolean allowCompilation )
	{
		Collection<DocumentDescriptor<Executable>> documentDescriptors = documentSource.getDocuments();
		DefrostTask[] defrostTasks = new DefrostTask[documentDescriptors.size()];
		int i = 0;
		for( DocumentDescriptor<Executable> documentDescriptor : documentDescriptors )
			defrostTasks[i++] = new DefrostTask( documentDescriptor, documentSource, languageManager, allowCompilation );

		return defrostTasks;
	}

	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param documentDescriptor
	 *        The document descriptor for the executable
	 * @param documentSource
	 *        The document source for the executables, required for processing
	 *        in-flow tags during executable initialization
	 * @param languageManager
	 *        The language manager for executable initialization
	 * @param allowCompilation
	 *        Whether to compile executables
	 */
	public DefrostTask( DocumentDescriptor<Executable> documentDescriptor, DocumentSource<Executable> documentSource, LanguageManager languageManager, boolean allowCompilation )
	{
		this.documentDescriptor = documentDescriptor;
		this.documentSource = documentSource;
		this.languageManager = languageManager;
		this.allowCompilation = allowCompilation;
	}

	//
	// Callable
	//

	public Executable call() throws Exception
	{
		Executable executable = documentDescriptor.getDocument();

		if( executable == null )
		{
			LanguageAdapter scriptEngine = languageManager.getAdapterByExtension( documentDescriptor.getDefaultName(), documentDescriptor.getTag() );
			String sourceCode = documentDescriptor.getSourceCode();
			executable = new Executable( documentDescriptor.getDefaultName(), sourceCode, false, languageManager, (String) scriptEngine.getAttributes().get( LanguageAdapter.DEFAULT_TAG ), documentSource, allowCompilation );

			Executable existing = documentDescriptor.setDocumentIfAbsent( executable );
			if( existing != null )
				executable = existing;
		}

		return executable;
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
	 * The document descriptor for the executable.
	 */
	private final DocumentDescriptor<Executable> documentDescriptor;

	/**
	 * The document source for the executables, required for processing in-flow
	 * tags during executable initialization.
	 */
	private final DocumentSource<Executable> documentSource;

	/**
	 * The language manager for executable initialization.
	 */
	private final LanguageManager languageManager;

	/**
	 * Whether to compile executables.
	 */
	private final boolean allowCompilation;
}
