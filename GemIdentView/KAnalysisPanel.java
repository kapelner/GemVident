
package GemIdentView;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JPanel;

import GemIdentAnalysisConsole.BuildGlobals;
import GemIdentAnalysisConsole.ConsoleParser;
import GemIdentTools.Matrices.BoolMatrix;

/**
 * Controls and houses the data analysis panel. For discussion
 * on data analysis in the context of the <b>GemIdent</b> package,
 * please consult section 6 of the manual.
 * 
 * @author Adam Kapelner
 * 
 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
 */
@SuppressWarnings("serial")
public class KAnalysisPanel extends JPanel {

	/** the output console as well as the command text box where users type commands */
	private Console console;
	/** the panel on the left that displays icons of images either in memory or on the hard disk */
	private ElementView elementView;
	/** the controller object that parses the user's commands, and executes the function */
	private ConsoleParser parser;
	/** a mapping between an image name and the image in this workspace */
	private HashMap<String,BoolMatrix> images;

	/** initializes the components of the panel and orders them on the screen */
	public KAnalysisPanel(){
		super();
		super.setLayout(new BorderLayout());
		
		images=new HashMap<String,BoolMatrix>();		
		elementView=new ElementView();
		parser=new ConsoleParser(images,elementView);
		console=new Console(parser);
		elementView.setConsole(console);
		
		add(elementView,BorderLayout.WEST);		
		add(console.getInnards(),BorderLayout.CENTER);
	}
	/** adds images in the workspace to the analysis page */
	public void addImageIcons() {
		ArrayList<String> list=BuildGlobals.GetGlobalFilenames();
		for (String file:list)
			images.put(file,null);
		elementView.SetUpGlobalImagesAfterProjectOpen(list);	
		this.repaint();
	}
	/** focus on the command textbox - puts the cursor there */
	public void FocusEnter(){
		console.FocusEnter();
	}
	/** run a script file */
	public void RunScript(File file){
		parser.RunScript(file);
	}
	public ConsoleParser getParser(){
		return parser;
	}
}