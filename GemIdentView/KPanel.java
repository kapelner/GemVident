
package GemIdentView;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Box;
import javax.swing.JPanel;

/**
 * The super class of all panels where the central display is
 * an image in an {@link KImagePanel image panel}, the east side
 * is the image's {@link KMagnify magnifier} and the south may or
 * may not display thumbnail views. The class is abstract because
 * it provides common functionality; but it, itself is never instantiated
 * 
 * @author Adam Kapelner
 *
 */
@SuppressWarnings("serial")
public abstract class KPanel extends JPanel {

	/** the image panel in the center of the panel */
	protected KImagePanel imagePanel;
	/** the Eastern region of the panel (to be populated by a {@link KMagnify magnifier} */
	protected Box eastBox;
	/** the Central region of the panel (to be populated by a {@link KImagePanel image panel} */
	protected Box centerBox;
	/** the Southern region of the panel (may be populated by a {@link KThumbnailPane thumbnail view} */
	protected Box southBox;	
	

	/** default constructor */
	public KPanel(){		
		super();
		setLayout(new BorderLayout());		
	}
	
	/** resets the image panel in the Central region with a new component (to be overridden) */
	protected void setImagePanel(KImagePanel imagePanel){
		this.imagePanel=imagePanel;
		centerBox=Box.createVerticalBox();		
		centerBox.add(imagePanel.getScrollPane());
		add(centerBox,BorderLayout.CENTER);
		AppendEast();
		add(eastBox,BorderLayout.EAST);
	}
	/** sets the image panel's magnifier in the Eastern region (to be overridden) */
	protected void AppendEast(){		
		eastBox=Box.createVerticalBox();		
		eastBox.add(imagePanel.getMagnifier());		
	}
	/** resets the Southern region (to be overridden) */
	protected void EditSouth(){
		southBox=Box.createVerticalBox();
		add(southBox,BorderLayout.SOUTH);	
	}	
	public KImagePanel getImagePanel() {
		return imagePanel;
	}
	/** resets the Eastern region with a new component (to be overridden) */
	public void appendToEast(Component comp){
		remove(eastBox);
		eastBox.add(comp);
		add(eastBox,BorderLayout.EAST);
		repaint();
	}
}