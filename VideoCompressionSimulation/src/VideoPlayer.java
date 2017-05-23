import java.awt.Button;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Class used to create JFrame for displaying the CompressedVideo video 
 * COPYRIGHT (C) 2017 John Leibowitz. All Rights Reserved.
 * @author John Leibowitz
 * @version 1.00
 */
class VideoPlayer implements ActionListener{

	private JFrame frame;
	private JLabel imageLabel;
	private BufferedImage curFrameImage;
	private JLabel videoHeaderText;
	private CompressedVideo video;
	
	/**
	 * Creates a VideoPlayer using a CompressedVideo as input
	 * @param video parent video
	 */
	VideoPlayer(CompressedVideo video) {
		this.video = video;
		curFrameImage = new BufferedImage(video.frameWidth, video.frameHeight, BufferedImage.TYPE_INT_RGB);
		createFrame();		
	}

	/**
	 * Pauses or un-pauses video based on button click
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		video.togglePause();		
	}

	/**
	 * Updates JFrame curFrameImage repetitively to play video as a sequence
	 * of images.
	 * @param frameNum current frame number of the video
	 * @param gazeX the x value of the mouse pointer, normalized for the Jframe window,
	 * @param gazeY the y value of the mouse pointer, normalized for the Jframe window.
	 * @param gazeOn true if mouse pointer is used to simulate gaze.
	 */
	void updateFrameImg(int frameNum, boolean gazeOn) {
		Point curMousePoint; 
		int mouseX; 
		int mouseY;
		curMousePoint = MouseInfo.getPointerInfo().getLocation();
		SwingUtilities.convertPointFromScreen(curMousePoint, imageLabel);
		if (gazeOn) {
			mouseX = curMousePoint.x;
			mouseY = curMousePoint.y;
		}
		else {
			mouseX = -video.frameWidthPadded;
			mouseY = -video.frameHeightPadded;
		}

		curFrameImage = video.videoFrames[frameNum].getFrameImage(video, mouseX, mouseY, gazeOn);
		imageLabel.setIcon(new ImageIcon(curFrameImage)); 
		updateVideoHeaderText(frameNum);
	}
	
	private void createFrame(){
		// create Jframe
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
		String result = " ";
		videoHeaderText = new JLabel(result);
		videoHeaderText.setHorizontalAlignment(SwingConstants.CENTER);
		imageLabel = new JLabel(new ImageIcon(curFrameImage));
		
		// add video header text
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(videoHeaderText, c);
	
		// add image
		c.gridy = 1;
		frame.getContentPane().add(imageLabel, c);
	
		// add pause/play button
		c.gridy = 2;
		c.anchor = GridBagConstraints.CENTER;
		Button pausePlay = new Button("Pause/Play");
		pausePlay.addActionListener(this);
		frame.getContentPane().add(pausePlay, c);
		
		// finish
		frame.pack();
		frame.setVisible(true);
		frame.toFront();
	}

	private void updateVideoHeaderText(int frameNum) {
		videoHeaderText.setText("Foreground Quantization Parameter: " + video.foregroundQuant + 
				"  Background Quantization Parameter: " + video.backgroundQuant + 
				"  Gaze On: " + video.gazeOn + 
				"  Current Frame: " + frameNum + "/" + video.numOfFrames);
	}
	
}
