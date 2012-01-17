package GemIdentView; //5941 on 12/11/06

import javax.swing.AbstractAction;

/**
 * Wraps an AbstractAction
 * 
 * @author Adam Kapelner
 *
 */
public class ActionWrap{
	
	private AbstractAction action;

	public AbstractAction getAction(){
		return action;
	}
	public void setAction(AbstractAction action){
		this.action=action;
	}
}