
package GemIdentView;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import GemIdentImageSets.ImageSetInterfaceWithUserColors;
import GemIdentModel.Stain;
import GemIdentOperations.Run;
import GemIdentOperations.StainMaker;
import GemIdentTools.IOTools;

/**
 * A type of {@link KClassInfo class info} that's specially designed
 * to represent a {@link GemIdentModel.Stain color} and provide
 * support for {@link GemIdentOperations.StainMaker computing
 * Mahalanobis cubes}
 * 
 * @author Adam Kapelner
 *
 */
@SuppressWarnings("serial")
public class KStainInfo extends KClassInfo {
	
	/** the user chooses to {@link GemIdentOperations.StainMaker compute a Mahalanobis cube} for this color */
	protected JButton compute_button;
	/** the progress bar that updates the user of the cube's construction */
	protected JProgressBar progress;

	/**
	 * Instantiates a new color info
	 * 
	 * @param imageTrainPanel		the image panel the info is connected to
	 * @param name					the name of the new info
	 * @param owner					simplifies selection of infos
	 */
	public KStainInfo(KImageTrainPanel imageTrainPanel,String name,SelectionEmulator owner ) {
		super(imageTrainPanel,owner);
		
		trainer=new Stain();
		trainer.setName(name);
		nameField.setText(name);
		
		EditWestBox();
		SetUpListeners();
	}
	/**
	 * Instantiates a new color info from an existing {@link GemIdentModel.Stain stain}
	 * 
	 * @param imageTrainPanel		the image panel the info is connected to
	 * @param trainer				the {@link GemIdentModel.Stain stain} model to build the info according to
	 * @param owner					simplifies selection of infos
	 */
	public KStainInfo(KImageTrainPanel imageTrainPanel, Stain trainer, SelectionEmulator owner ){
		super(imageTrainPanel,owner);
		
		this.trainer=trainer;
		this.nameField.setText(trainer.getName());
		this.rminSpinner.setValue(trainer.getRmin());
		colorChooseButton.repaint();
		EditWestBox();
		SetUpListeners();
		UnSelect();
	}


	/** appends the color-specific controls to the Western region of the class info */
	protected void EditWestBox() {
		Box makeColorBox = Box.createHorizontalBox();
		
		compute_button = new JButton("Compute");
		
		makeColorBox.add(compute_button);
		
		west_side.add(makeColorBox);
		west_side.add(Box.createVerticalStrut(3));
	}
	
	/** the name of the "background" color */
	public static final String backgroundStainName = "background"; 
	
	/** 
	 * sets up the listeners for the components that change the underlying model.
	 * For discussion on the creation of color info via the compute button, consult
	 * section 3.1.4 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	protected void SetUpListeners() {
		nameField.addKeyListener(
			new KeyListener(){
				public void keyPressed(KeyEvent e){}
				public void keyReleased(KeyEvent e){
					String previous=trainer.getName();
					String present=nameField.getText().trim();
//					System.out.println("prev:"+previous+" present:"+present);
//					System.out.println("keycode:"+e.getKeyCode()+" enter:"+KeyEvent.VK_ENTER);
					if (!previous.equals(present)){
						Run.it.getUserColorsImageset().DeleteStain(previous);
						trainer.setName(present);
//						JOptionPane.showMessageDialog(Run.it.getGUI(),"Stain renamed from \""+previous+"\" to \""+present+"\"");
						Run.it.getUserColorsImageset().AddStain((Stain)trainer);
					}
					
				}
				public void keyTyped(KeyEvent e){}
			}
		);
		rminSpinner.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					trainer.setRmin((Integer)rminSpinner.getValue());	
				}
			}
		); 
		
		compute_button.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (isComputed()){
						int result=JOptionPane.showConfirmDialog(Run.it.getGUI(),
								"Are you sure you want to recompute the color information and replace the pre-existing file?",
								"Warning",
								JOptionPane.YES_NO_OPTION);
						
						if ( result == JOptionPane.NO_OPTION ) return;				
					}
					if (trainer.getTotalPoints() < ImageSetInterfaceWithUserColors.MinimumColorPointsNeeded)
						JOptionPane.showMessageDialog(Run.it.getGUI(),"Need at least " + ImageSetInterfaceWithUserColors.MinimumColorPointsNeeded + " training points");
					else
						MakeCube();	
				}
				
				private void MakeCube(){
					progress=new JProgressBar(0,100);
					Run.it.getUserColorsImageset().addToCubeComputePool(((Stain)trainer).getStainMaker(progress,null));					
					MakeProgressBar();					
				}
			}
		);
	}
	/** when the user clicks "Compute," the {@link #progress progress bar} that reflects the construction
	 * of the cube is added to the info. The other controls are disabled at this time.
	 * When the computation is done, the bar is removed.
	 */
	private void MakeProgressBar() {
		progress.setBorderPainted(true); 
		progress.setStringPainted(true); 
		//lets first disable all other components:		
		nameField.setEnabled(false);
		colorChooseButton.setEnabled(false);
		rminSpinner.setEnabled(false);
		identifierButton.setEnabled(false);
		//add listener
		progress.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					if (progress.getValue() == 100)
						FinishedComputing();
				}
			}			
		);
		
		Dimension button_size = this.compute_button.getSize();
		Insets button_insets = this.compute_button.getMargin();
		
		button_size.width -= button_insets.left + button_insets.right;
		button_size.height -= button_insets.top + button_insets.bottom;
		
		this.progress.setPreferredSize(button_size);
		compute_button.add(progress);
		compute_button.setEnabled(false);
		repaint();
		Run.it.FrameRepaint();
	}
	
	protected void FinishedComputing() {
		DestroyProgressBar();
	}
	
	/** removes the {@link #progress progress bar} and reenables the other controls */
	protected void DestroyProgressBar() {
		compute_button.remove(progress);
		compute_button.setEnabled(true);
//		setPreferredSize(new Dimension(classInfoSize.width,classInfoSize.height));
		repaint();
		//re-enable components
		nameField.setEnabled(true); //we don't want this one re-enabled
		colorChooseButton.setEnabled(true);
		rminSpinner.setEnabled(true);
		identifierButton.setEnabled(true);
		Run.it.FrameRepaint();
	}	
	/** gets the underlying Stain */
	public Stain getStain() {
		return ((Stain)trainer);
	}
	/** all color info's identifier images are {@link KClassInfo#buildExampleImageFromColors() built from colors} */	
	public void buildExampleImage() {
		buildExampleImageFromColors();
	}
	/** is the Mahalanobis cube corresponding to this color computed? */
	protected boolean isComputed(){
		return IOTools.FileExists(StainMaker.colorSubDir + File.separator + trainer.getName());
	}
}