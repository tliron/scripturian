package com.threecrickets.scripturian;

import com.threecrickets.scripturian.exception.ExecutableInitializationException;
import com.threecrickets.scripturian.exception.ExecutionException;

public abstract class Scriptlet
{
	public abstract String getCode();

	public abstract void compile() throws ExecutableInitializationException;

	public abstract Object execute( ExecutionContext executionContext ) throws ExecutableInitializationException, ExecutionException;
}
