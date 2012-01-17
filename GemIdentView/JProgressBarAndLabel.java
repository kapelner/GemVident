package GemIdentView;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeListener;

/**
 * Very simple class that combines a progress bar
 * and a label together. Undocumented due to simplicity.
 * 
 * @author Adam Kapelner
 */
public class JProgressBarAndLabel {

	private JProgressBar bar;
	private JLabel label;
	private String title;
	private final Box box;
	
	public JProgressBarAndLabel(int a,int b,String title){
		this.title=title;
		bar=new JProgressBar(a,b);
		bar.setStringPainted(true); 
		label=new JLabel(title);
		
		box=Box.createVerticalBox();
		box.add(label);
		box.add(bar);
	}
	public void AppendTitle(String append){
		label.setText(title+append);
	}
	public Box getBox(){
		return box;
	}
	public void addChangeListener(ChangeListener listener) {
		bar.addChangeListener(listener);		
	}
	public int getValue() {
		return bar.getValue();
	}
	public void setValue(int v){
		bar.setValue(v);		
	}
	public void setText(String text){
		label.setText(text);
	}
	
	public void Disable() {
		bar.setEnabled(false);
		bar.setFocusable(false);
		label.setEnabled(false);
		label.setFocusable(false);
	}
	public void setVisible(boolean b) {
		bar.setVisible(b);
		label.setVisible(b);
	}
	public int getMaximum(){
	  return bar.getMaximum();	
	}
}