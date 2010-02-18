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

import javax.script.ScriptEngineManager;

/**
 * @author Tal Liron
 * @see Defroster
 */
public class DefrostTask implements Callable<Document>
{
	//
	// Static operations
	//

	public static DefrostTask[] create( DocumentSource<Document> documentSource, ScriptEngineManager scriptEngineManager, boolean allowCompilation )
	{
		Collection<DocumentDescriptor<Document>> documentDescriptors = documentSource.getDocumentDescriptors();
		DefrostTask[] defrostTasks = new DefrostTask[documentDescriptors.size()];
		int i = 0;
		for( DocumentDescriptor<Document> documentDescriptor : documentDescriptors )
			defrostTasks[i++] = new DefrostTask( documentDescriptor, documentSource, scriptEngineManager, allowCompilation );

		return defrostTasks;
	}

	//
	// Construction
	//

	public DefrostTask( DocumentDescriptor<Document> documentDescriptor, DocumentSource<Document> documentSource, ScriptEngineManager scriptEngineManager, boolean allowCompilation )
	{
		this.documentDescriptor = documentDescriptor;
		this.documentSource = documentSource;
		this.scriptEngineManager = scriptEngineManager;
		this.allowCompilation = allowCompilation;
	}

	//
	// Callable
	//

	public Document call() throws Exception
	{
		Document document = documentDescriptor.getDocument();

		if( document == null )
		{
			String scriptEngineName = Scripturian.getScriptEngineNameByExtension( documentDescriptor.getDefaultName(), documentDescriptor.getTag(), scriptEngineManager );
			String text = documentDescriptor.getText();
			document = new Document( documentDescriptor.getDefaultName(), text, true, scriptEngineManager, scriptEngineName, documentSource, allowCompilation );

			Document existing = documentDescriptor.setDocumentIfAbsent( document );
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

	private final DocumentDescriptor<Document> documentDescriptor;

	private final DocumentSource<Document> documentSource;

	private final ScriptEngineManager scriptEngineManager;

	private final boolean allowCompilation;
}
