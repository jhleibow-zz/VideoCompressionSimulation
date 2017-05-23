import java.io.File;


/**
 * Class that represents a compressed video used by the
 * VideoCompressionSimulation class. This class contains all important
 * metadata and data structures associated with the VideoCompressionSimulation
 * class.
 * COPYRIGHT (C) 2017 John Leibowitz. All Rights Reserved.
 * @author John Leibowitz
 * @version 1.00
 */
class CompressedVideo {
	
	final int macroBlockSize;
	final int dctBlockSize;
	final int frameHeight;
	final int frameWidth;
	final int frameHeightPadded;
	final int frameWidthPadded;
	final int frameSizePadded;
	final int numOfMacroBlocksPerFrame;
	final int numOfFrames;
	final int searchParamK;
	final int foregroundQuant;
	final int backgroundQuant;
	final int gazeSize;
	
	/** 
	 * Byte array that is read from the input file with one frame's worth of r (red), b (blue), and g(green) bytes.
	 * Y (grayscale channel) bytes are computed during reading of input file 
	 */	
	byte[] rgbyInput; 

	/** 
	 * Array of video frames, where each frame can be thought of as a picture in time, when shown rapidly enough,
	 * one after the other, creates a movie. Video frame is an important class that contains all of the data for
	 * the post-processed video (rgbyInput is the pre-processed video)
	 */
	VideoFrame[] videoFrames;
	
	/**
	 * Simple class that contains the JFrame and action listeners in order to display, play, and pause a video.
	 */
	VideoPlayer player;
	
	/**
	 * Used for DCT calculations, only needs to be derived once per video to save computation, see DCTBlock class
	 */
	float[][] cosTable; 
	
	/** 
	 * Instance variables for turning gaze simulation on (with mouse pointer) and pausing the playback of the video 
	 */
	boolean gazeOn;
	private boolean pause;
		
	static final int NUM_CHANNELS_RGB = 3;
	static final int NUM_CHANNELS_RGBY = 4;

	

	CompressedVideo(File inputFile, 
			int macroBlockSize, 
			int dctBlockSize, 
			int frameHeight, 
			int frameWidth,
			int searchParamK,
			int gazeSize,
			int foregroundQuant,
			int backgroundQuant,			
			boolean gazeControlOn) {
		
		this.macroBlockSize = macroBlockSize;
		this.dctBlockSize = dctBlockSize;
		this.frameHeight = frameHeight;
		this.frameWidth = frameWidth;
		this.searchParamK = searchParamK;
		this.gazeSize = gazeSize;
		this.foregroundQuant = foregroundQuant;
		this.backgroundQuant = backgroundQuant;
		this.gazeOn = gazeControlOn;
		frameHeightPadded = (frameHeight % macroBlockSize != 0) ? ((frameHeight/macroBlockSize) + 1) * macroBlockSize : frameHeight;
		frameWidthPadded = (frameWidth % macroBlockSize != 0) ? ((frameWidth/macroBlockSize) + 1) * macroBlockSize : frameWidth;
		frameSizePadded = frameHeightPadded * frameWidthPadded;
		numOfMacroBlocksPerFrame = (frameHeightPadded * frameWidthPadded) / (macroBlockSize * macroBlockSize);
		numOfFrames = (int) (inputFile.length()/(frameHeight * frameWidth * NUM_CHANNELS_RGB));
		rgbyInput = (new RGBFileReader(this)).getBytes(inputFile); //reads input file using RGBFileReader instance which also creates Y channel
		cosTable = DCTBlock.initCosTable(dctBlockSize); //creates cosine table used later for DCT computation
		videoFrames = VideoFrame.createFrames(this);  
		player = new VideoPlayer(this);  
		pause = false;
	}


	/**
	 * Plays compressed video in a looped manner and checks for pause flag
	 */
	void playVideo() throws InterruptedException {
		System.out.println("Video now playing...");
		for (int frameNum = 0; frameNum < numOfFrames; frameNum++) {
			//pause if true
			while (pause) {
				Thread.sleep(50);
			}
			
			//update frame
			player.updateFrameImg(frameNum, gazeOn);
			
			//loop video
			if (frameNum == numOfFrames - 1) {
				frameNum = 0;
			}
			
		}
	}


	/**
	 * Method to get one byte of data from input video
	 */
	byte getOneByte(int frameNum, Channel channel, int row, int column) {
		return rgbyInput[(frameNum * frameSizePadded * NUM_CHANNELS_RGBY) +
		                 (channel.getColorNum() * frameSizePadded) +
		                 (row * frameWidthPadded) + 
		                 column]; 
	}
	
	/**
	 * toggles boolean pause instance variable
	 */
	void togglePause() {
		if (pause) {
			pause = false;
		}
		else {
			pause = true;
		}
	}
	
	boolean getPause() {
		return pause;
	}
	

	/**
	 * Enum to specify integer for RGBY channels (Y=Gray)
	 */
	enum Channel {
		RED(0), GREEN(1), BLUE(2), GRAY(3);
		private int colorNum;
		private Channel(int cNum) {
			colorNum = cNum;
		}
		int getColorNum() {
			return colorNum;
		}
		public static Channel getChannel(int cNum) {
			for (Channel c : Channel.values()) {
				if (c.colorNum == cNum) {
					return c;
				}
			}
			return null;
		}
	}


}
