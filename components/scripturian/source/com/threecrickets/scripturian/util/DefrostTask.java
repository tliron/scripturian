/**
 * Copyright 2009-2015 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.util;

import java.util.Collection;
import java.util.concurrent.Callable;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ParsingContext;
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
	 * @param parsingContext
	 *        The parsing context
	 * @return An array of tasks
	 * @see DocumentSource#getDocument(String)
	 */
	public static DefrostTask[] createMany( ParsingContext parsingContext )
	{
		Collection<DocumentDescriptor<Executable>> documentDescriptors = parsingContext.getDocumentSource().getDocuments();
		DefrostTask[] defrostTasks = new DefrostTask[documentDescriptors.size()];
		int i = 0;
		for( DocumentDescriptor<Executable> documentDescriptor : documentDescriptors )
			defrostTasks[i++] = new DefrostTask( documentDescriptor, parsingContext );

		return defrostTasks;
	}

	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @param documentDescriptor
	 *        The document descriptor for the executable
	 * @param parsingContext
	 *        The parsing context
	 */
	public DefrostTask( DocumentDescriptor<Executable> documentDescriptor, ParsingContext parsingContext )
	{
		this.documentDescriptor = documentDescriptor;
		this.parsingContext = parsingContext;
	}

	//
	// Callable
	//

	public Executable call() throws Exception
	{
		Executable executable = documentDescriptor.getDocument();

		if( executable == null )
			executable = Executable.createOnce( documentDescriptor, parsingContext.getDefaultParser(), parsingContext );

		return executable;
	}

	//
	// Object
	//

	@Override
	public String toString()
	{
		return "DefrosterTask: " + documentDescriptor;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The document descriptor for the executable.
	 */
	private final DocumentDescriptor<Executable> documentDescriptor;

	/**
	 * The parsing context.
	 */
	private final ParsingContext parsingContext;
}
