package GemIdentAnalysisConsole;


/**
 * The framework that every function in the {@link ConsoleParser ConsoleParser}
 * is built upon. If the user's text matches one of the function names (see the {@link
 * ConsoleParser#Parse(String) Parse} function}, {@link #ParseFunctionParamsAndReturn(String, String) 
 * ParseFunctionParamsAndReturn} is called which further parses the user's input. If 
 * an error is found it is returned and the function is not called.
 * 
 * @author Adam Kapelner
 */
public abstract class RunnableFunction implements Cloneable{

	/** the return value specified by the user */
	protected String returnValue;
	/** the array of parameters specified by the user */
	protected String[] params;
	/** the text the user entered originally */
	protected String text;
	
	/**
	 * Parses the user's input further, {@link #AnalyzeParameters(String, String) 
	 * finding the parameters}, finding the return value, and checking if
	 * it's well formed
	 * 
	 * @param text				the text the user entered originally
	 * @param function			the name of the function the user is trying to invoke
	 * @return					and error message (null if there is none)
	 */
	public String ParseFunctionParamsAndReturn(String text,String function){ //returns null if okay or error message if not
		this.text=text;
		String message=null;
		String[] equationPieces=text.split("=");

		if (text.charAt(text.length()-1) != ')')
			message="\""+text+"\" is not well formed";
		else if (equationPieces.length == 0)
			message=AnalyzeParameters(text,function);
		else if (equationPieces.length == 1)
			message=AnalyzeParameters(equationPieces[0],function);
		else if (equationPieces.length == 2){
			returnValue=equationPieces[0];
			message=AnalyzeParameters(equationPieces[1],function);			
		}
		else
			message="\""+text+"\" is not well formed";
		
		if (message == null)
			return checkIfOkay();
		else
			return message;
	}
	/**
	 * Finds the parameters by checking the after the "(" in 
	 * the function invokation
	 * 
	 * @param piece			a segment of the user's text to be checked
	 * @param function		the name of the function the user is trying to invoke
	 * @return				an error message (null if none)
	 */
	private String AnalyzeParameters(String piece,String function) {
		String[] paramSplit=piece.split("\\(");
//		System.out.println("AnalyzeParameters text:"+this.text);
//		for (String param:paramSplit){
//			System.out.println("param:"+param+" len:"+paramSplit.length);		
//		}
		if (paramSplit.length != 2)
			return "\""+piece+"\" is not a well formed function call";
		if (!(paramSplit[0].toLowerCase()).equals(function.toLowerCase()))
			return "\""+paramSplit[0]+"\" is spelled incorrectly (did you mean \"" + function + "\"?)";
		String paramText=paramSplit[1].substring(0,paramSplit[1].length()-1);
//		System.out.println("paramText:"+paramText);
		
		String[] parameters=paramText.split(",");
//		for (String param:parameters){
//			System.out.println("comma split, param:"+param+" len:"+parameters.length);
//		}
		if (parameters.length == 1){
			if (paramText.equals(""))
				return "no parameters given";
			else if (",".equals(paramText.substring(paramText.length()-1,paramText.length()-1)))
				return "\""+paramText+"\" is an invalid parameter set";
			params=new String[1];
			params[0]=parameters[0];
		}
		else
			params=parameters;
		
		return null;	
	}
	/**
	 * After the parameters / return value are parsed successfully,
	 * the specific function must check if the parameters / return
	 * value are appropriate. If so, the function will execute, if not
	 * the user will receive an error message.
	 * 
	 * @return				an error message (null if none)
	 */
	protected abstract String checkIfOkay();
	/**
	 * If there are no errors, the function executes.
	 * The function instructions are enclosed in a {@link java.lang.Runnable
	 * Runnable} object so it can be executed in parallel in 
	 * a {@link java.util.concurrent.ExecutorService thread pool}
	 * 
	 * @return		the function executable as a Runnable
	 */
	public abstract Runnable getOperation();
	/**
	 * When the user invokes the system command "help,"
	 * help is displayed for all functions. Each function has a
	 * specific help message defined in this method
	 * 
	 * @return		the help message specific to this function
	 */
	public abstract String HelpMessage();
	/** Clones the current function */
	public abstract RunnableFunction clone();
}