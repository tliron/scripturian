/**
 * 
 */
package com.threecrickets.scripturian;

import javax.script.ScriptContext;
import javax.script.ScriptException;

/**
 * @author Tal Liron
 * @see ScriptedMainContainer
 */
class ScriptedMainScriptContextController implements ScriptContextController
{
	//
	// ScriptContextController
	//

	public void initialize( ScriptContext scriptContext ) throws ScriptException
	{
		scriptContext.setAttribute( containerVariableName, container, ScriptContext.ENGINE_SCOPE );

		if( scriptContextController != null )
			scriptContextController.initialize( scriptContext );
	}

	public void finalize( ScriptContext scriptContext )
	{
		if( scriptContextController != null )
			scriptContextController.finalize( scriptContext );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	protected ScriptedMainScriptContextController( String containerVariableName, ScriptedMainContainer container, ScriptContextController scriptContextController )
	{
		this.containerVariableName = containerVariableName;
		this.container = container;
		this.scriptContextController = scriptContextController;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final String containerVariableName;

	private final ScriptedMainContainer container;

	private final ScriptContextController scriptContextController;
}