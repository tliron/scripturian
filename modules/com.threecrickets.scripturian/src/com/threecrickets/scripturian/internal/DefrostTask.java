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

import java.util.concurrent.Callable;

import com.threecrickets.scripturian.Defroster;
import com.threecrickets.scripturian.Document;
import com.threecrickets.scripturian.DocumentDescriptor;
import com.threecrickets.scripturian.Scripturian;

/**
 * @author Tal Liron
 * @see Defroster
 */
public class DefrostTask implements Callable<Document>
{
	//
	// Construction
	//

	public DefrostTask( DocumentDescriptor<Document> documentDescriptor, Defroster defroster )
	{
		this.documentDescriptor = documentDescriptor;
		this.compiler = defroster;
	}

	//
	// Callable
	//

	public Document call() throws Exception
	{
		Document document = documentDescriptor.getDocument();

		if( document == null )
		{
			String scriptEngineName = Scripturian.getScriptEngineNameByExtension( documentDescriptor.getDefaultName(), documentDescriptor.getTag(), compiler.getScriptEngineManager() );
			String text = documentDescriptor.getText();
			document = new Document( documentDescriptor.getDefaultName(), text, true, compiler.getScriptEngineManager(), scriptEngineName, compiler.getDocumentSource(), true );

			Document existing = documentDescriptor.setDocumentIfAbsent( document );
			if( existing != null )
				document = existing;
		}

		return document;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final DocumentDescriptor<Document> documentDescriptor;

	private final Defroster compiler;
}
