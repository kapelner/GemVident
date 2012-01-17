package GemVident;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import GemIdentTools.IOTools;
import GemIdentView.ScrollablePicture;

public class CropMovieFrames {

	private BufferedImage I;
	private ArrayList<Point> points;
	private boolean points_picked;

	public CropMovieFrames(BufferedImage I) {
		this.I = I;
		points = new ArrayList<Point>();
	}

	private void PickPoints() {
		final ScrollablePicture pic = IOTools.GenerateScrollablePicElement(I, "select the four points to crop at, close when finished");
		final int red = Color.RED.getRGB();
		pic.addMouseListener(
			new MouseListener(){
				public void mouseClicked(MouseEvent arg0){}
				public void mouseEntered(MouseEvent arg0){}
				public void mouseExited(MouseEvent arg0){}
				public void mousePressed(MouseEvent e){
					Point t = e.getPoint();
					points.add(t);
//					System.out.println("t:" + t.toString());
					//make a little square:
					for (int i = -3; i < 3; i++){
						for (int j = -3; j < 3; j++){
							try {
								I.setRGB(t.x + i, t.y + j, red);
							} catch (ArrayIndexOutOfBoundsException exc){} //who cares
						}
					}
				}
				public void mouseReleased(MouseEvent arg0){
					pic.repaint();
				}							
			}						
		);
		final JFrame frame = ((JFrame)(pic.getParent()).getParent().getParent().getParent().getParent().getParent());
		frame.addWindowListener(
				new WindowListener(){
					public void windowActivated(WindowEvent arg0) {}
					public void windowClosed(WindowEvent arg0) {}
					public void windowClosing(WindowEvent arg0) {
//						System.out.println("closing window");
						if (points.size() < 4){
							JOptionPane.showMessageDialog(arg0.getWindow(), "you must choose at least four points");
							frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
						}
						else {
							points_picked = true; //let getCoordinates finally return
							frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
						}						
					}
					public void windowDeactivated(WindowEvent arg0) {}
					public void windowDeiconified(WindowEvent arg0) {}
					public void windowIconified(WindowEvent arg0) {}
					public void windowOpened(WindowEvent arg0) {}					
				}
		);
	}

	public int[] getCoordinates() {
		PickPoints();
		while (!points_picked){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e){}
		}
		return ResolvePointsIntoMinsAndMaxs();
	}

	private int[] ResolvePointsIntoMinsAndMaxs() {		
		int xmin = Integer.MAX_VALUE;
		int xmax = Integer.MIN_VALUE;
		int ymin = Integer.MAX_VALUE;
		int ymax = Integer.MIN_VALUE;
		for (Point t : points){
			if (t.x < xmin){
				xmin = t.x;
			}
			if (t.x > xmax){
				xmax = t.x;
			}
			if (t.y < ymin){
				ymin = t.y;
			}
			if (t.y > ymax){
				ymax = t.y;
			}
		}
		int[] mins_and_maxs = {xmin, xmax, ymin, ymax};
		//System.out.println("coords: " + xmin + " " + xmax + " " + ymin + " " + ymax);
		return mins_and_maxs;
	}

}
