/**
 * Copyright 2009-2014 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
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

	public static final String SOURCE_ATTRIBUTE = SuccinctAdapter.class.getCanonicalName() + ".templateSource";

	public static final String FORMATTER_ATTRIBUTE = SuccinctAdapter.class.getCanonicalName() + ".formatter";

	public static final String FILLER_ATTRIBUTE = SuccinctAdapter.class.getCanonicalName() + ".filler";

	public static final String CASTER_ATTRIBUTE = SuccinctAdapter.class.getCanonicalName() + ".caster";

	public static final String CASTER_CONTEXT_ATTRIBUTE = SuccinctAdapter.class.getCanonicalName() + ".casterContext";

	public static final String INCLUSION_KEY = "scripturian.include ";

	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 *         In case of an initialization error
	 */
	public SuccinctAdapter() throws LanguageAdapterException
	{
		super( "Succinct", "", "Succinct", "", Arrays.asList( "succint", "template" ), "succinct", Arrays.asList( "succinct" ), "succinct" );
	}

	//
	// Attributes
	//

	public TemplateSource getTemplateSource( ExecutionContext executionContext )
	{
		TemplateSource templateSource = (TemplateSource) executionContext.getAttributes().get( SuccinctAdapter.SOURCE_ATTRIBUTE );
		return templateSource;
	}

	public Formatter getFormatter( ExecutionContext executionContext )
	{
		Formatter formatter = (Formatter) executionContext.getAttributes().get( SuccinctAdapter.FORMATTER_ATTRIBUTE );
		return formatter;
	}

	public Filler getFiller( Executable executable, ExecutionContext executionContext )
	{
		Filler filler = (Filler) executionContext.getAttributes().get( SuccinctAdapter.FILLER_ATTRIBUTE );
		if( filler == null )
		{
			filler = new ScripturianSuccinctFiller( getManager(), executable, executionContext );
			executionContext.getAttributes().put( SuccinctAdapter.FILLER_ATTRIBUTE, filler );
		}
		return filler;
	}

	public Caster<Object> getCaster( ExecutionContext executionContext )
	{
		@SuppressWarnings("unchecked")
		Caster<Object> caster = (Caster<Object>) executionContext.getAttributes().get( SuccinctAdapter.CASTER_ATTRIBUTE );
		return caster;
	}

	public Object getCasterContext( ExecutionContext executionContext )
	{
		Object casterContext = executionContext.getAttributes().get( SuccinctAdapter.CASTER_CONTEXT_ATTRIBUTE );
		return casterContext;
	}

	//
	// LanguageAdapter
	//

	@Override
	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		// TODO: this breaks nesting; we should escape tags and keep it in
		// Succinct
		return null;
	}

	@Override
	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return Tag.BEGIN + expression + Tag.END;
	}

	@Override
	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		// We are handling inclusion via a special Succinct cast.
		// See: ScripturianSuccinctFiller

		return Tag.BEGIN + Cast.MARK + INCLUSION_KEY + expression + Tag.END;
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new SuccinctProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}
}
