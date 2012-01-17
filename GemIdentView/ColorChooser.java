package GemIdentView;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;

/** 
 * a window that allows the user to choose 
 * a display color for the training points 
 */
@SuppressWarnings("serial")
public class ColorChooser extends JFrame {
	
	/** the built-in Java class does all the work for us */ 
	private JColorChooser color_chooser;
	private JButton button_ok;
	
	/** sets the title, adds "okay" and "cancel" buttons, and determines what to do upon submission */
	public ColorChooser() {
		super();
		setTitle("Select display color");
		color_chooser = new JColorChooser();
		setVisible(true);
		Container frame_contents = Box.createVerticalBox();
		frame_contents.add(color_chooser);
		
		button_ok = new JButton("OK");		
		button_ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);					
			}
		});
		JButton button_cancel = new JButton("Cancel");
		button_cancel.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				setVisible(false);
			}
		});
		Container button_row = Box.createHorizontalBox();
		button_row.add(button_ok);
		button_row.add(button_cancel);
		frame_contents.add(button_row);
		
		add(frame_contents);
		pack();
	}
	
	public void addOkayListener(ActionListener okaylistener) {
		button_ok.addActionListener(okaylistener);
	}
	
	public Color getChosenColor(){
		return color_chooser.getColor();
	}
}