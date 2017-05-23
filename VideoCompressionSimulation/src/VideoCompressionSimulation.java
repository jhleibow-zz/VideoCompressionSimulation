import java.io.File;

/**
 * Class used to read a video file in an .rgb format, divide each video frame
 * into background and foreground macro blocks, encode the data using DCT 
 * (Discrete Cosine Transform),and then use background and foreground 
 * compression parameters to play back the video in a manner that simulates 
 * differing levels of compression for the foreground vs the background.
 * Also, the mouse pointer can be used to simulate gaze control if that feature 
 * is turned on.
 * COPYRIGHT (C) 2017 John Leibowitz. All Rights Reserved.
 * @author John Leibowitz
 * @version 1.00
 */
public class VideoCompressionSimulation {

	//constants
	static final int MACRO_BLOCK_SIZE = 16; //see MacroBlock class
	static final int DCT_BLOCK_SIZE = 8; //see DCTBlock class
	static final int FRAME_HEIGHT = 540; //video frame height
	static final int FRAME_WIDTH = 960; //video frame width
	static final int SEARCH_PARAM = 16; //motion search range (in number of pixels) for MacroBlocks  
	static final int GAZE_SIZE = 64;  //size of square in pixels used to represent gaze window if feature is turned on
	

	/**
	 * Runs video compression simulation
	 * @param args[0] filename to be read, must be .rgb format
	 * @param args[1] foreground quantization parameter, must be integer >= 1
	 * @param args[2] background quantization parameter, must be integer >= 1
	 * @param args[3] gaze control 1 for on, or 0 for off 
	 */
	public static void main(String[] args) {
		String inputFilename = args[0];
		int foreQuant = Integer.parseInt(args[1]);
		int backQuant = Integer.parseInt(args[2]);
		boolean gazeControlOn = (Integer.parseInt(args[3]) == 1) ? true : false;
		
		CompressedVideo video = new CompressedVideo(new File(inputFilename), 
				MACRO_BLOCK_SIZE, 
				DCT_BLOCK_SIZE, 
				FRAME_HEIGHT, 
				FRAME_WIDTH, 
				SEARCH_PARAM, 
				GAZE_SIZE,
				foreQuant, 
				backQuant,				
				gazeControlOn);
		try {
			video.playVideo(); //plays video compression simulation
		} catch (InterruptedException e) {
			System.err.println("Caught InterruptedException: " +  e.getMessage());
		}
	}
	

}
