package GemIdentView;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * The class used to generate the cursors.
 * Eventually cursors will be replaced with image icons.
 * The library is not documented.
 * 
 * @author Kyle Woodward
 *
 */
public class CursorFactory{
  	
  	public static final int CIRCLE_V2 = 1;
  	private static Map<Integer,Cursor> cursor_map = new HashMap<Integer,Cursor>();
  	
  	
  	
  	public static Cursor getCursor( int cid ) throws Exception {
  		if ( !CursorFactory.cursor_map.containsKey(cid) ) {
  			CursorFactory.makeCursor(cid);
  		}
  		
  		return CursorFactory.cursor_map.get(cid);
  	}
  	
  	
  	private static void makeCursor( int cid ) throws Exception {
  		switch ( cid ) {
  			case CursorFactory.CIRCLE_V2:
  				CursorFactory.cursor_map.put(cid,CursorFactory.createCircleCursor());
  				return;
  		}
  		
  		throw new Exception("CursorIndexOutOfBoundsException");
  	}
  	
  	
  	
  	private static Cursor createCircleCursor() {
  		int radius = 6;
  		
  		BufferedImage cursor = new BufferedImage(32,32,BufferedImage.TYPE_4BYTE_ABGR);
  		Graphics g = cursor.getGraphics();
  		g.setColor(Color.BLACK);
  		
  		int h_x = cursor.getWidth()/2;
  		int h_y = cursor.getHeight()/2;
  		for ( int x = 0; x < cursor.getWidth(); ++x ) {
  			for ( int y = 0; y < cursor.getHeight(); ++y ) {
  				int sq_dist = (x-h_x)*(x-h_x)+(y-h_y)*(y-h_y);
  				double dist = Math.sqrt(sq_dist);
  				
  				double left = Math.abs(radius-dist);
  				if ( left <= 1.0d ) {
  					float alpha = new Float(left);
  					g.setColor(CursorFactory.makeBlackAlpha(alpha));
  					g.drawLine(x,y,x,y);
  				}
  			}
  		}
  		
  		return Toolkit.getDefaultToolkit().createCustomCursor(cursor,new Point(15,15),"ModCircle");
  	}
  	
  	
  	
  	private static Color makeBlackAlpha( float alpha ) {
  		return new Color(0,0,0,alpha);
  	}
}