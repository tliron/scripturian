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

import java.util.concurrent.atomic.AtomicReference;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.ExtendedParseException;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.StackFrame;

/**
 * @author Tal Liron
 */
class VelocityProgram extends ProgramBase<VelocityAdapter>
{
	//
	// Construction
	//

	/**
	 * Construction.
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
	public VelocityProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, VelocityAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		RuntimeInstance runtimeInstance = adapter.getRuntimeInstance();
		VelocityContext velocityContext = new VelocityContext( executionContext.getServices() );

		SimpleNode nodeTree = nodeTreeReference.get();
		if( nodeTree == null )
		{
			try
			{
				// Dark magicks to allow us to easily escape Velocity tokens
				// (see VelocityAdapter.getSourceCodeForLiteralOutput)
				String sourceCode = "#set($_d='$')#set($_h='#')" + this.sourceCode;

				nodeTree = runtimeInstance.parse( sourceCode, executable.getDocumentName() );

				// We're caching the resulting node tree for the future. Might
				// as well!
				nodeTreeReference.compareAndSet( null, nodeTree );

			}
			catch( ParseException x )
			{
				throw new ParsingException( executable.getDocumentName(), x.currentToken.beginLine + startLineNumber, x.currentToken.beginColumn, x.getMessage(), x );
			}
		}

		try
		{
			runtimeInstance.render( velocityContext, executionContext.getWriter(), executable.getDocumentName(), nodeTree );
		}
		catch( Exception x )
		{
			if( x instanceof ExtendedParseException )
			{
				ExtendedParseException extendedParseException = (ExtendedParseException) x;
				Throwable cause = x.getCause();
				if( cause instanceof ExecutionException )
				{
					ExecutionException executionException = new ExecutionException( cause.getMessage(), cause.getCause() );
					executionException.getStack().addAll( ( (ExecutionException) cause ).getStack() );
					executionException.getStack().add( new StackFrame( extendedParseException.getTemplateName(), extendedParseException.getLineNumber(), extendedParseException.getColumnNumber() ) );
					throw executionException;
				}
				else
					throw new ExecutionException( extendedParseException.getTemplateName(), extendedParseException.getLineNumber(), extendedParseException.getColumnNumber(), x.getMessage(), x );
			}
			else
				throw new ExecutionException( executable.getDocumentName(), x.getMessage(), x );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final AtomicReference<SimpleNode> nodeTreeReference = new AtomicReference<SimpleNode>();
}
