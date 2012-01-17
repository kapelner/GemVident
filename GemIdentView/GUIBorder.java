package GemIdentView;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.border.AbstractBorder;

/**
 * Creates a titled border around a GUI component
 * 
 * @author Kyle Woodward
 *
 */
@SuppressWarnings("serial")
public class GUIBorder extends AbstractBorder {
	
	/** the title of the component being bordered */
	private String title;

	/** default constructor */
	public GUIBorder( String title ) {
		this.title = title;
	}
	
	/** paints the border around a component */
	public void paintBorder( Component c, Graphics g, int x, int y, int width, int height ) {
		int x_0 = x+5;
		int x_1 = x+width-5;
		int y_0 = y+7;
		int y_1 = y+height-5;
		
		FontMetrics fm = g.getFontMetrics();
		
		//g.drawLine(x_0,y_0,x_1,y_0);
		g.drawLine(x_0,y_0,x_0+6,y_0);
		g.drawLine(x_0+(int)fm.getStringBounds(title,g).getWidth()+12,y_0,x_1,y_0);
		g.drawLine(x_1,y_0,x_1,y_1);
		g.drawLine(x_1,y_1,x_0,y_1);
		g.drawLine(x_0,y_1,x_0,y_0);
		
		g.drawString(title,x+15,y+11);
	}
}