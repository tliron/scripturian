/**
 * Copyright 2009-2013 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.adapter;

import java.util.concurrent.atomic.AtomicReference;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.succinct.CastException;
import com.threecrickets.succinct.Caster;
import com.threecrickets.succinct.Filler;
import com.threecrickets.succinct.ParseException;
import com.threecrickets.succinct.RichTemplate;
import com.threecrickets.succinct.Template;
import com.threecrickets.succinct.TemplateSourceException;

/**
 * @author Tal Liron
 */
class SuccinctProgram extends ProgramBase<SuccinctAdapter>
{
	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param isScriptlet
	 *        Whether the source code is a scriptlet
	 * @param position
	 *        The program's position in the executable
	 * @param startLineNumber
	 *        The line number in the document for where the program's source
	 *        code begins
	 * @param startColumnNumber
	 *        The column number in the document for where the program's source
	 *        code begins
	 * @param executable
	 *        The executable
	 * @param adapter
	 *        The language adapter
	 */
	public SuccinctProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, SuccinctAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Template template = templateReference.get();

		if( template == null )
		{
			try
			{
				template = new RichTemplate( sourceCode, adapter.getTemplateSource( executionContext ) );

				// We're caching the resulting template for the future. Might
				// as well!
				templateReference.compareAndSet( null, template );
			}
			catch( TemplateSourceException x )
			{
				throw new ParsingException( executable.getDocumentName(), x.getMessage(), x );
			}
			catch( ParseException x )
			{
				throw new ParsingException( executable.getDocumentName(), x.getMessage(), x );
			}
		}

		template.setFormatter( adapter.getFormatter( executionContext ) );

		try
		{
			Filler filler = adapter.getFiller( executable, executionContext );
			if( filler != null )
				template.cast( filler, executionContext.getWriter() );
			else
			{
				Caster<Object> caster = adapter.getCaster( executionContext );
				if( caster == null )
					throw new ExecutionException( executable.getDocumentName(), "Execution context must contain either a \"" + SuccinctAdapter.FILLER + "\" or a \"" + SuccinctAdapter.CASTER + "\" attribute" );

				caster.cast( template, null, executionContext.getWriter(), adapter.getCasterContext( executionContext ) );
			}
		}
		catch( CastException x )
		{
			throw new ExecutionException( executable.getDocumentName(), x.getMessage(), x );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final AtomicReference<Template> templateReference = new AtomicReference<Template>();
}
