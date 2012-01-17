package GemIdentView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.Box;
import javax.swing.JPanel;

/**
 * Frames components together and displays a frame title. 
 * Not documented.
 * 
 * @author Kyle Woodward
 *
 */
@SuppressWarnings("serial")
public class GUIFrame extends JPanel {

	private class PointRect {
		public int x1;
		public int x2;
		public int y1;
		public int y2;
		
		public PointRect( int x1, int y1, int x2, int y2 ) {
			this.x1 = x1;
			this.x2 = x2;
			this.y1 = y1;
			this.y2 = y2;
		}
	}
	private String frame_title;
	
	public GUIFrame( String s ) {
		frame_title = s;
		
		this.setLayout(new BorderLayout());
		this.add(Box.createHorizontalStrut(10),BorderLayout.WEST);
		this.add(Box.createHorizontalStrut(10),BorderLayout.EAST);
		this.add(Box.createVerticalStrut(15),BorderLayout.NORTH);
		this.add(Box.createVerticalStrut(13),BorderLayout.SOUTH);
	}
	
	public GUIFrame() {
		new GUIFrame("");
	}
	
	public Component add( Component c ) {
		this.add(c,BorderLayout.CENTER);
		return c;
	}
	
	public void paintComponent( Graphics g ) {
		super.paintComponent(g);
		
		Rectangle r = this.getBounds();
		PointRect s = new PointRect(xLeft(r),yTop(r),xRight(r),yBottom(r));
		FontMetrics fm = g.getFontMetrics();
		
		int string_length = (int)fm.getStringBounds(frame_title,g).getWidth();
		
		g.setColor(Color.BLACK);
		g.drawLine(s.x2,s.y1,s.x2,s.y2);
		g.drawLine(s.x1,s.y2,s.x2,s.y2);
		g.drawLine(s.x1,s.y1,s.x1,s.y2);
		g.drawLine(s.x1,s.y1,11,8);
		g.drawLine(string_length+17,s.y1,s.x2,s.y1);
		g.drawString(frame_title,15,11);
	}
	
	public void setTitle( String title ) {
		this.frame_title = "Page inspector - "+title;
	}
	
	private int xLeft( Rectangle r ) {
		return 5;
	}
	
	private int xRight( Rectangle r ) {
		return 5+r.width-10;
	}
	
	private int yBottom( Rectangle r ) {
		return r.height-7;
	}
	
	private int yTop( Rectangle r ) {
		return 8;
	}
}