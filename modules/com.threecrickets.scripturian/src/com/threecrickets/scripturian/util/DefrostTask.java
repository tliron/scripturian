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

package com.threecrickets.scripturian.util;

import java.util.Collection;
import java.util.concurrent.Callable;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.document.DocumentDescriptor;
import com.threecrickets.scripturian.document.DocumentSource;

/**
 * A {@link Callable} that makes sure that a {@link Executable} is loaded and
 * possibly prepared, making it ready to execute without delay.
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
	 * @param isTextWithScriptlets
	 *        Whether the executables are "text-with-scriptlets"
	 * @param prepare
	 *        Whether to prepare executables
	 * @return An array of tasks
	 * @see DocumentSource#getDocument(String)
	 */
	public static DefrostTask[] forDocumentSource( DocumentSource<Executable> documentSource, LanguageManager languageManager, boolean isTextWithScriptlets, boolean prepare )
	{
		Collection<DocumentDescriptor<Executable>> documentDescriptors = documentSource.getDocuments();
		DefrostTask[] defrostTasks = new DefrostTask[documentDescriptors.size()];
		int i = 0;
		for( DocumentDescriptor<Executable> documentDescriptor : documentDescriptors )
			defrostTasks[i++] = new DefrostTask( documentDescriptor, documentSource, languageManager, isTextWithScriptlets, prepare );

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
	 * @param isTextWithScriptlets
	 *        Whether the executable is "text-with-scriptlets"
	 * @param prepare
	 *        Whether to prepare executables
	 */
	public DefrostTask( DocumentDescriptor<Executable> documentDescriptor, DocumentSource<Executable> documentSource, LanguageManager languageManager, boolean isTextWithScriptlets, boolean prepare )
	{
		this.documentDescriptor = documentDescriptor;
		this.documentSource = documentSource;
		this.languageManager = languageManager;
		this.isTextWithScriptlets = isTextWithScriptlets;
		this.prepare = prepare;
	}

	//
	// Callable
	//

	public Executable call() throws Exception
	{
		Executable executable = documentDescriptor.getDocument();

		if( executable == null )
		{
			LanguageAdapter adapter = languageManager.getAdapterByExtension( documentDescriptor.getDefaultName(), documentDescriptor.getTag() );
			executable = Executable.createOnce( documentDescriptor, documentSource.getIdentifier(), isTextWithScriptlets, languageManager, adapter != null ? (String) adapter.getAttributes().get(
				LanguageAdapter.DEFAULT_TAG ) : null, prepare );
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
	 * Whether the executable is "text-with-scriptlets".
	 */
	private final boolean isTextWithScriptlets;

	/**
	 * Whether to prepare executables.
	 */
	private final boolean prepare;
}
