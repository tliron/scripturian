/**
 * Copyright 2009-2012 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.adapter;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.python.Version;
import org.python.compiler.LegacyCompiler;
import org.python.core.CompilerFlags;
import org.python.core.Options;
import org.python.core.Py;
import org.python.core.PyBaseException;
import org.python.core.PyException;
import org.python.core.PyFileWriter;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.core.PyTraceback;
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
import com.threecrickets.scripturian.internal.ExecutionContextPyFileErrorWriter;
import com.threecrickets.scripturian.internal.ExecutionContextPyFileWriter;
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
	 * @param startLineNumber
	 *        The line number in the document for where the program's source
	 *        code begins
	 * @param x
	 *        The exception
	 * @return The execution exception
	 */
	public static ExecutionException createExecutionException( String documentName, int startLineNumber, Exception x )
	{
		if( x instanceof PyException )
		{
			PyException pyException = (PyException) x;
			pyException.normalize();

			ExecutionException executionException;
			Throwable cause = x.getCause();
			if( cause instanceof ExecutionException )
			{
				executionException = new ExecutionException( cause.getMessage(), cause.getCause() );
				executionException.getStack().addAll( ( (ExecutionException) cause ).getStack() );
			}
			else if( cause instanceof ParsingException )
			{
				executionException = new ExecutionException( cause.getMessage(), cause.getCause() );
				executionException.getStack().addAll( ( (ParsingException) cause ).getStack() );
			}
			else
			{
				if( pyException.value instanceof PyBaseException )
					executionException = new ExecutionException( ( (PyBaseException) pyException.value ).message.toString(), pyException );
				else
					executionException = new ExecutionException( pyException.getMessage(), pyException );
			}

			for( PyTraceback traceback = pyException.traceback; traceback instanceof PyTraceback; traceback = (PyTraceback) traceback.tb_next )
				executionException.getStack().add( new StackFrame( traceback.tb_frame.f_code.co_filename, traceback.tb_lineno + startLineNumber, -1 ) );

			return executionException;
		}
		else
			return new ExecutionException( documentName, x.getMessage(), x );
	}

	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 */
	public JythonAdapter() throws LanguageAdapterException
	{
		super( "Jython", Version.getBuildInfo(), "Python", Version.PY_VERSION, Arrays.asList( "py" ), "py", Arrays.asList( "python", "py", "jython" ), "jython" );

		if( PySystemState.registry == null )
		{
			String homePath = System.getProperty( PYTHON_HOME );
			if( homePath == null )
				throw new LanguageAdapterException( this.getClass(), "Must define " + PYTHON_HOME + " system property in order to use Jython adapter" );

			homePath = new File( homePath ).getAbsolutePath();

			File packagesCacheDir = new File( LanguageManager.getCachePath(), PYTHON_PACKAGES_CACHE_DIR );

			// The packages cache dir must be absolute or relative to the home
			// dir, so we'll have to relativize it back if it's not absolute.
			// Also note that Jython will add a "packages" subdirectory
			// underneath.
			String packagesCacheDirPath;
			if( packagesCacheDir.isAbsolute() )
				packagesCacheDirPath = packagesCacheDir.getPath();
			else
				packagesCacheDirPath = ScripturianUtil.getRelativePath( packagesCacheDir.getAbsolutePath(), homePath );

			Properties overridingProperties = new Properties();
			overridingProperties.put( PYTHON_HOME, homePath );
			overridingProperties.put( PySystemState.PYTHON_CACHEDIR, packagesCacheDirPath );

			// Reduce default verbosity (otherwise we get annoying
			// "processing new jar" messages)
			Options.verbose = Py.WARNING;

			PySystemState.initialize( System.getProperties(), overridingProperties );
		}

		compiler = new LegacyCompiler();
		compilerFlags = CompilerFlags.getCompilerFlags();

		sharedSystemState = new PySystemState();
	}

	//
	// Attributes
	//

	/**
	 * Gets a Python interpreter instance stored in the execution context,
	 * creating it if it doesn't exist. Each execution context is guaranteed to
	 * have its own Python interpreter. The interpreter is updated to match the
	 * writers and services in the execution context.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @param executable
	 *        The executable
	 * @return The Python interpreter
	 */
	public PythonInterpreter getPythonInterpreter( ExecutionContext executionContext, Executable executable ) throws LanguageAdapterException
	{
		PythonInterpreter pythonInterpreter = (PythonInterpreter) executionContext.getAttributes().get( JYTHON_INTERPRETER );

		if( pythonInterpreter == null )
		{
			// Note: If we don't explicitly create a new system state here,
			// Jython will reuse system state found on this thread!

			PySystemState systemState = new PySystemState();
			systemState.modules = sharedSystemState.modules;
			systemState.path = sharedSystemState.path;

			pythonInterpreter = new PythonInterpreter( null, systemState );

			// Writers
			pythonInterpreter.setOut( new ExecutionContextPyFileWriter() );
			pythonInterpreter.setErr( new ExecutionContextPyFileErrorWriter() );

			pythonInterpreter.exec( "import sys,site" );

			executionContext.getAttributes().put( JYTHON_INTERPRETER, pythonInterpreter );
		}
		else
		{
			flush( pythonInterpreter );
		}

		// Append library locations to sys.path
		for( URI uri : executionContext.getLibraryLocations() )
		{
			try
			{
				String path = new File( uri ).getPath();
				if( File.separatorChar != '/' )
					path = path.replace( File.separatorChar, '/' );
				pythonInterpreter.exec( "site.addsitedir('" + path.replace( "'", "\\'" ) + "')" );
			}
			catch( IllegalArgumentException x )
			{
				// URI is not a file
			}
		}

		// Expose services as Python globals
		for( Map.Entry<String, Object> entry : executionContext.getServices().entrySet() )
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

	@Override
	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = ScripturianUtil.doubleQuotedLiteral( literal );
		// return executable.getExposedExecutableName() +
		// ".context.writer.write(" + literal + ");";
		return "sys.stdout.write(" + literal + ");";
	}

	@Override
	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		// return executable.getExposedExecutableName() +
		// ".context.writer.write(" + expression + ");";
		return "sys.stdout.write(str(" + expression + "));";
	}

	@Override
	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		String containerIncludeExpressionCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_EXPRESSION_COMMAND );
		return executable.getExecutableServiceName() + ".container." + containerIncludeExpressionCommand + "(" + expression + ");";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new JythonProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	@Override
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		entryPointName = toPythonStyle( entryPointName );
		PythonInterpreter pythonInterpreter = (PythonInterpreter) executionContext.getAttributes().get( JYTHON_INTERPRETER );
		Py.setSystemState( pythonInterpreter.getSystemState() );
		PyObject method = pythonInterpreter.get( entryPointName );
		if( method == null )
			throw new NoSuchMethodException( entryPointName );
		try
		{
			PyObject[] pythonArguments = Py.javas2pys( arguments );
			PyObject r = method.__call__( pythonArguments );
			return r.__tojava__( Object.class );
		}
		catch( Exception x )
		{
			throw JythonAdapter.createExecutionException( executable.getDocumentName(), 0, x );
		}
		finally
		{
			flush( pythonInterpreter );
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

	/**
	 * Flush stdout and stderr.
	 * 
	 * @param pythonInterpreter
	 *        The Python interpreter
	 */
	protected static void flush( PythonInterpreter pythonInterpreter )
	{
		// pythonInterpreter.exec( "sys.stdout.flush();sys.stderr.flush()" );
		( (PyFileWriter) pythonInterpreter.getSystemState().stdout ).flush();
		( (PyFileWriter) pythonInterpreter.getSystemState().stderr ).flush();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * Shared system state. We need this ensure that code is only loaded once.
	 */
	private final PySystemState sharedSystemState;

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
		for( int i = 1, length = camelCase.length(); i < length; i++ )
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
