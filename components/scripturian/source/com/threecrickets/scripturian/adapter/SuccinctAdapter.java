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

package com.threecrickets.scripturian.adapter;

import java.util.Arrays;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.internal.ScripturianSuccinctFiller;
import com.threecrickets.succinct.Caster;
import com.threecrickets.succinct.Filler;
import com.threecrickets.succinct.Formatter;
import com.threecrickets.succinct.TemplateSource;
import com.threecrickets.succinct.chunk.Tag;
import com.threecrickets.succinct.chunk.tag.Cast;

/**
 * A {@link LanguageAdapter} that supports the <a
 * href="http://velocity.apache.org/">Velocity</a> templating language.
 * 
 * @author Tal Liron
 */
public class SuccinctAdapter extends LanguageAdapterBase
{
	//
	// Constants
	//

	public static final String SOURCE = "succinct.templateSource";

	public static final String FORMATTER = "succinct.formatter";

	public static final String FILLER = "succinct.filler";

	public static final String CASTER = "succinct.caster";

	public static final String CASTER_CONTEXT = "succinct.casterContext";

	public static final String INCLUSION_KEY = "scripturian.include ";

	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 */
	public SuccinctAdapter() throws LanguageAdapterException
	{
		super( "Succinct", "", null, null, Arrays.asList( "succint", "template" ), null, Arrays.asList( "succinct" ), null );
	}

	//
	// Attributes
	//

	public TemplateSource getTemplateSource( ExecutionContext executionContext )
	{
		TemplateSource templateSource = (TemplateSource) executionContext.getAttributes().get( SuccinctAdapter.SOURCE );
		return templateSource;
	}

	public Formatter getFormatter( ExecutionContext executionContext )
	{
		Formatter formatter = (Formatter) executionContext.getAttributes().get( SuccinctAdapter.FORMATTER );
		return formatter;
	}

	public Filler getFiller( Executable executable, ExecutionContext executionContext )
	{
		Filler filler = (Filler) executionContext.getAttributes().get( SuccinctAdapter.FILLER );
		if( filler == null )
		{
			filler = new ScripturianSuccinctFiller( getManager(), executable, executionContext );
			executionContext.getAttributes().put( SuccinctAdapter.FILLER, filler );
		}
		return filler;
	}

	public Caster<Object> getCaster( ExecutionContext executionContext )
	{
		@SuppressWarnings("unchecked")
		Caster<Object> caster = (Caster<Object>) executionContext.getAttributes().get( SuccinctAdapter.CASTER );
		return caster;
	}

	public Object getCasterContext( ExecutionContext executionContext )
	{
		Object casterContext = executionContext.getAttributes().get( SuccinctAdapter.CASTER_CONTEXT );
		return casterContext;
	}

	//
	// LanguageAdapter
	//

	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		return literal;
	}

	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return Tag.BEGIN + expression + Tag.END;
	}

	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		// We are handling inclusiong via a special Succinct cast.
		// See: ScripturianSuccinctFiller

		return Tag.BEGIN + Cast.MARK + INCLUSION_KEY + expression + Tag.END;
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new SuccinctProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}
}
