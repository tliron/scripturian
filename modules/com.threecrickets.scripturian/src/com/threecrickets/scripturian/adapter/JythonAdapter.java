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

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.python.Version;
import org.python.compiler.LegacyCompiler;
import org.python.core.CompilerFlags;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.core.PythonCompiler;
import org.python.util.PythonInterpreter;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.StackFrame;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * A {@link LanguageAdapter} that supports the Python language as implemented by
 * <a href="http://www.jython.org/">Jython</a>.
 * 
 * @author Tal Liron
 */
public class JythonAdapter extends LanguageAdapterBase
{
	//
	// Constants
	//

	/**
	 * The Python interpreter attribute.
	 */
	public static final String JYTHON_INTERPRETER = "jython.interpreter";

	/**
	 * The Python home property.
	 */
	public static final String PYTHON_HOME = "python.home";

	/**
	 * The default base directory for cached packages. (Jython will add a
	 * "packages" subdirectory underneath.)
	 */
	public static final String PYTHON_PACKAGES_CACHE_DIR = "python";

	/**
	 * The default base directory for cached executables.
	 */
	public static final String PYTHON_EXECUTABLES_CACHE_DIR = "python/executables";

	//
	// Static operations
	//

	/**
	 * Creates an execution exception with a full stack.
	 * 
	 * @param documentName
	 *        The document name
	 * @param x
	 *        The exception
	 * @return The execution exception
	 */
	public static ExecutionException createExecutionException( String documentName, Exception x )
	{
		if( x instanceof PyException )
		{
			PyException pyException = (PyException) x;
			pyException.normalize();

			Throwable cause = x.getCause();
			if( cause instanceof ExecutionException )
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause.getCause() );
				executionException.getStack().addAll( ( (ExecutionException) cause ).getStack() );
				executionException.getStack().add( new StackFrame( documentName, pyException.traceback != null ? pyException.traceback.tb_lineno : -1, -1 ) );
				return executionException;
			}
			else if( cause instanceof ParsingException )
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause.getCause() );
				executionException.getStack().addAll( ( (ParsingException) cause ).getStack() );
				executionException.getStack().add( new StackFrame( documentName, pyException.traceback != null ? pyException.traceback.tb_lineno : -1, -1 ) );
				return executionException;
			}
			else
				return new ExecutionException( documentName, pyException.traceback != null ? pyException.traceback.tb_lineno : -1, -1, Py.formatException( pyException.type, pyException.value ), pyException.getCause() );
		}
		else
			return new ExecutionException( x.getMessage(), x );
	}

	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @throws LanguageAdapterException
	 */
	public JythonAdapter() throws LanguageAdapterException
	{
		super( "Jython", Version.PY_VERSION, "Python", Version.PY_VERSION, Arrays.asList( "py" ), null, Arrays.asList( "python", "py", "jython" ), null );

		String homePath = System.getProperty( PYTHON_HOME );
		File packagesCacheDir = new File( LanguageManager.getCachePath(), PYTHON_PACKAGES_CACHE_DIR );

		// Initialize Jython registry (can only happen once per VM)
		if( PySystemState.registry == null )
		{
			// The packages cache dir is calculated as relative to the home dir,
			// so we'll have to relatavize it back. Note that Jython will add a
			// "packages" subdirectory underneath.
			String packagesCacheDirPath = ScripturianUtil.getRelativeFile( packagesCacheDir, new File( homePath ) ).getPath();

			Properties overridingProperties = new Properties();
			overridingProperties.put( PYTHON_HOME, homePath );
			overridingProperties.put( PySystemState.PYTHON_CACHEDIR, packagesCacheDirPath );

			PySystemState.initialize( System.getProperties(), overridingProperties );
		}

		compiler = new LegacyCompiler();
		compilerFlags = CompilerFlags.getCompilerFlags();
	}

	//
	// Attributes
	//

	/**
	 * Gets a Python interpreter instance stored in the execution context,
	 * creating it if it doesn't exist. Each execution context is guaranteed to
	 * have its own Python interpreter. The interpreter is updated to match the
	 * writers and exposed variables in the execution context.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The Python interpreter
	 */
	public PythonInterpreter getPythonInterpreter( ExecutionContext executionContext )
	{
		PythonInterpreter pythonInterpreter = (PythonInterpreter) executionContext.getAttributes().get( JYTHON_INTERPRETER );

		if( pythonInterpreter == null )
		{
			pythonInterpreter = new PythonInterpreter();
			executionContext.getAttributes().put( JYTHON_INTERPRETER, pythonInterpreter );
		}

		pythonInterpreter.setOut( executionContext.getWriterOrDefault() );
		pythonInterpreter.setErr( executionContext.getErrorWriterOrDefault() );

		// Expose variables as Python globals
		for( Map.Entry<String, Object> entry : executionContext.getExposedVariables().entrySet() )
			pythonInterpreter.set( entry.getKey(), entry.getValue() );

		return pythonInterpreter;
	}

	/**
	 * The base directory for cached executables.
	 * 
	 * @return The cache directory
	 */
	public File getCacheDir()
	{
		return new File( LanguageManager.getCachePath(), PYTHON_EXECUTABLES_CACHE_DIR );
	}

	//
	// LanguageAdapter
	//

	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = literal.replaceAll( "\\n", "\\\\n" );
		literal = literal.replaceAll( "\\\"", "\\\\\"" );
		return "sys.stdout.write(\"" + literal + "\");";
	}

	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "sys.stdout.write(" + expression + ");";
	}

	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		return executable.getExposedExecutableName() + ".container.includeDocument(" + expression + ");";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new JythonProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	@Override
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		entryPointName = toPythonStyle( entryPointName );
		PythonInterpreter pythonInterpreter = getPythonInterpreter( executionContext );
		PyObject method = pythonInterpreter.get( entryPointName );
		if( method == null )
			throw new NoSuchMethodException( entryPointName );
		PyObject[] pythonArguments = Py.javas2pys( arguments );
		try
		{
			PyObject r = method.__call__( pythonArguments );
			return r.__tojava__( Object.class );
		}
		catch( Exception x )
		{
			throw JythonAdapter.createExecutionException( executable.getDocumentName(), x );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	/**
	 * The Python compiler used for scriptlet preparation.
	 */
	protected final PythonCompiler compiler;

	/**
	 * The Python compiler flags used for scriptlet preparation.
	 */
	protected final CompilerFlags compilerFlags;

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * From somethingLikeThis to something_like_this.
	 * 
	 * @param camelCase
	 *        somethingLikeThis
	 * @return something_like_this
	 */
	private static String toPythonStyle( String camelCase )
	{
		StringBuilder r = new StringBuilder();
		char c = camelCase.charAt( 0 );
		if( Character.isUpperCase( c ) )
			r.append( Character.toLowerCase( c ) );
		else
			r.append( c );
		for( int i = 1; i < camelCase.length(); i++ )
		{
			c = camelCase.charAt( i );
			if( Character.isUpperCase( c ) )
			{
				r.append( '_' );
				r.append( Character.toLowerCase( c ) );
			}
			else
				r.append( c );
		}
		return r.toString();
	}
}
