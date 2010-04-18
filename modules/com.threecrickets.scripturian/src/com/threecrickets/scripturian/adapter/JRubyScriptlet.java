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

package com.threecrickets.scripturian.adapter;

import org.jruby.Ruby;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.Scriptlet;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;

/**
 * @author Tal Liron
 */
class JRubyScriptlet implements Scriptlet
{
	//
	// Construction
	//

	public JRubyScriptlet( String sourceCode, int startLineNumber, int startColumnNumber, Executable executable )
	{
		this.sourceCode = sourceCode;
		this.startLineNumber = startLineNumber;
		this.startColumnNumber = startColumnNumber;
		this.executable = executable;
	}

	//
	// Scriptlet
	//

	public void prepare() throws PreparationException
	{
		try
		{
			// Note that we parse the node for a different runtime than the
			// one we will run in. It's unclear what the repercussions of
			// this would be, but we haven't detected any trouble yet.

			Node node = compilerRuntime.parseEval( sourceCode, executable.getDocumentName(), compilerRuntime.getCurrentContext().getCurrentScope(), startLineNumber - 1 );
			script = compilerRuntime.tryCompile( node );
		}
		catch( RaiseException x )
		{
			// JRuby does not fill in the stack trace correctly, though the
			// error message is fine

			// StackTraceElement[] stack = x.getStackTrace();
			// if( ( stack != null ) && stack.length > 0 )
			// throw new PreparationException( stack[0].getFileName(),
			// stack[0].getLineNumber(), -1, x );

			throw new PreparationException( executable.getDocumentName(), startLineNumber, startColumnNumber, x );
		}
	}

	public Object execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		Ruby rubyRuntime = JRubyAdapter.getRubyRuntime( executionContext );

		try
		{
			if( script != null )
			{
				IRubyObject value = rubyRuntime.runScript( script );
				return value.toJava( Object.class );
			}
			else
			{
				IRubyObject value = rubyRuntime.executeScript( sourceCode, executable.getDocumentName() );
				// IRubyObject value = rubyRuntime.evalScriptlet( sourceCode );
				return value.toJava( Object.class );
			}
		}
		catch( RaiseException x )
		{
			throw JRubyAdapter.createExecutionException( x );
		}
	}

	public String getSourceCode()
	{
		return sourceCode;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final Ruby compilerRuntime = Ruby.newInstance();

	private final String sourceCode;

	private final int startLineNumber;

	private final int startColumnNumber;

	private final Executable executable;

	private Script script;
}