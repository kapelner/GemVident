package GemIdentView;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JSlider;

import GemIdentModel.Stain;
import GemIdentOperations.Run;

@SuppressWarnings("serial")
public class KBackgroundStainInfo extends KStainInfo {

	/** the slider indicating which the maximum log10 mahalanobis distance to be considered background */
	private JSlider mahalanobis_slider;
	
	public KBackgroundStainInfo(KImageBackgroundColorTrainPanel imageTrainPanel, String name, SelectionEmulator owner) {
		super(imageTrainPanel, name, owner);
	}

	public KBackgroundStainInfo(KImageBackgroundColorTrainPanel imageTrainPanel, Stain trainer, SelectionEmulator owner) {
		super(imageTrainPanel, trainer, owner);
	}
	
	private static final int MAX_MAHALANOBIS_DISTANCE_MULTIPLE = 100;
	private static final int MAX_LOG_10_MAHALANOBIS_DISTANCE = 4 * MAX_MAHALANOBIS_DISTANCE_MULTIPLE; //10^4
	/** appends the color-specific controls to the Western region of the class info */
	protected void EditWestBox() {
		CreateMahalanobisSlider();
		westBox.add(new JLabel("Threshold:", JLabel.RIGHT));
		westBox.add(mahalanobis_slider);
		super.EditWestBox();
		//if user presses compute, let's disable button
		compute_button.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					mahalanobis_slider.setEnabled(false);
				}
			}
		);		
	}

	private void CreateMahalanobisSlider() {
		mahalanobis_slider = new JSlider(JSlider.HORIZONTAL, 0, MAX_LOG_10_MAHALANOBIS_DISTANCE, 0);
		mahalanobis_slider.setEnabled(isComputed());
		mahalanobis_slider.setPreferredSize(new Dimension(50, 10));
		mahalanobis_slider.addMouseListener(
			new MouseListener(){
				public void mouseClicked(MouseEvent arg0) {}
				public void mouseEntered(MouseEvent arg0) {}
				public void mouseExited(MouseEvent arg0) {}
				public void mousePressed(MouseEvent arg0) {}
				public void mouseReleased(MouseEvent arg0) {
					//set the correct value with log adjustment
					trainer.setBackground_mahalanobis_distance(mahalanobisSliderValueToTrueMahalanobisDistance());
					//recreate the background image, redraw, and repaint
					((KImageBackgroundColorTrainPanel)imageTrainPanel).recreateBooleanBackgroundForIndividualStainRedrawAndRepaint((Stain)trainer);
					//now we just changed the value, so we mark the gui as dirty
					Run.it.GUIsetDirty(true);
				}
				
			}
		);
		//we should now set the correct slider value
		mahalanobis_slider.setValue(trueMahalanobisDistanceToMahalanobisSliderValue());
	}
	
	private int mahalanobisSliderValueToTrueMahalanobisDistance(){
		return (int)Math.round(Math.pow(10, mahalanobis_slider.getValue() / (double)MAX_MAHALANOBIS_DISTANCE_MULTIPLE));
	}
	
	private int trueMahalanobisDistanceToMahalanobisSliderValue(){
		return (int)Math.round(MAX_MAHALANOBIS_DISTANCE_MULTIPLE * Math.log10(trainer.getBackground_mahalanobis_distance()));
	}	

	protected void FinishedComputing() {
		super.FinishedComputing();
		mahalanobis_slider.setEnabled(true);
	}
}
