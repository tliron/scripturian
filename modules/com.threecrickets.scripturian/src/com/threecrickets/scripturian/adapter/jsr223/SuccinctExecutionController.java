/**
 * Copyright 2009-2010 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com
 */

package com.threecrickets.scripturian.adapter.jsr223;

import java.util.Map;

import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.ExecutionController;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.succinct.Caster;
import com.threecrickets.succinct.Filler;
import com.threecrickets.succinct.Formatter;
import com.threecrickets.succinct.TemplateSource;

/**
 * @author Tal Liron
 */
public abstract class SuccinctExecutionController implements ExecutionController
{
	//
	// Constants
	//

	public static final String SOURCE = "templateSource";

	public static final String FORMATTER = "templateFormatter";

	public static final String FILLER = "templateFiller";

	public static final String CASTER = "templateCaster";

	public static final String CASTER_ATTRIBUTES = "templateCasterAttributes";

	//
	// Construction
	//

	public SuccinctExecutionController( TemplateSource templateSource, Formatter formatter, Filler filler )
	{
		this.templateSource = templateSource;
		this.formatter = formatter;
		this.filler = filler;
		caster = null;
	}

	public SuccinctExecutionController( TemplateSource templateSource, Formatter formatter, Caster<Map<String, String>> caster )
	{
		this.templateSource = templateSource;
		this.formatter = formatter;
		this.caster = caster;
		filler = null;
	}

	//
	// Attributes
	//

	public TemplateSource getTemplateSource()
	{
		return templateSource;
	}

	public Formatter getFormatter()
	{
		return formatter;
	}

	public Filler getFiller()
	{
		return filler;
	}

	public Caster<Map<String, String>> getCaster()
	{
		return caster;
	}

	public abstract Map<String, String> getCasterAttributes();

	//
	// ScriptletController
	//

	public void initialize( ExecutionContext executionContext ) throws ExecutionException
	{
		executionContext.getAttributes().put( SOURCE, templateSource );
		if( formatter != null )
			executionContext.getAttributes().put( FORMATTER, formatter );
		executionContext.getAttributes().put( SOURCE, templateSource );
		if( filler != null )
			executionContext.getAttributes().put( FILLER, filler );
		else
		{
			executionContext.getAttributes().put( CASTER, caster );
			executionContext.getAttributes().put( CASTER_ATTRIBUTES, getCasterAttributes() );
		}
	}

	public void release( ExecutionContext documentContext )
	{
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final TemplateSource templateSource;

	private final Formatter formatter;

	private final Filler filler;

	private final Caster<Map<String, String>> caster;
}
