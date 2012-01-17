package GemIdentCentroidFinding.ViaSegmentation;

import java.io.Serializable;

import javax.swing.JFrame;

import GemIdentView.KPreProcessPanel;

public class StandAloneEditor implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7143582341343606254L;

    public static void main(String[] args){
    	KPreProcessPanel preProcessPanel = new KPreProcessPanel();
    	JFrame editFrame = new JFrame();
		editFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	editFrame.setTitle("GemIdent Editor");
    	editFrame.add(preProcessPanel);
    	editFrame.pack();
    	editFrame.setVisible(true);
    }
	
}
