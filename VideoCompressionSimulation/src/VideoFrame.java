import java.awt.image.BufferedImage;

/**
 * Class that represents one video frame for a CompressedVideo class. Contains
 * arrays of Macro Blocks as well as DCT Blocks for this frame. Has important
 * internal method assignLayers that chooses if a MacroBlock should be foreground
 * or background based on motion vectors and the SAD error related to the motion 
 * vector calculation
 * COPYRIGHT (C) 2017 John Leibowitz. All Rights Reserved.
 * @author John Leibowitz
 * @version 1.00
 */
class VideoFrame {

	MacroBlock[][] macroBlocks;
	DCTBlock[] dctBlocks;
		
	//************************************************************//
	//      INITIAL FOREGROUND/BACKGROUND ASSIGNEMENT VALUES      //
	//************************************************************//
	
	//SAD error must be greater than this value to be initially assigned to foreground
	private static final int FOREGROUND_SAD_LOWER_ERROR_THRESHOLD = 500; 
	
	//SAD error must be less than this value to be initially assigned to foreground
	private static final int FOREGROUND_SAD_UPPER_ERROR_THRESHOLD = 8000;
	
	//Sum of the absolute difference between the frame's average motion vectors and the individual
	//macroblock's motion vectors must be greater than this value to be initially assigned to foreground 
	private static final float MOTION_VECTOR_DIFFERENCE_THRESHOLD = 2.2f;
	
	//Minimum number of foreground macroblocks per frame (best attempt)
	private static final int MIN_NUM_BLOCKS = 7;
	
	//Value to increment MOTION_VECTOR_DIFFERENCE_THRESHOLD by
	private static final float MOTION_VECTOR_DIFFERENCE_INCREMENT = 0.33f;
	
	//Max number of times to run a loop
	private static final int MAX_NUM_RUNS = 5;
	
	//The number three
	private static final int THREE = 3;
	
	
	//************************************************************//
	//            FOREGROUND/BACKGROUND FILTER VALUES             //
	//************************************************************//
		

	//if a corner (corner of the frame) macroblock has a count of neighbor macroblocks that 
	//are assigned to the background, greater than or equal to this value, 
	//then the corner macroblock is assigned to the background, else assigned to the foreground
	private static final int CORNER_NEIGHBOR_BACKGROUND_THRESHOLD = 2;
	
	//if an edge (edge of the frame) macroblock has a count of neighbor macroblocks that 
	//are assigned to the background, greater than or equal to this value, 
	//then the edge macroblock is assigned to the background
	private static final int EDGE_BACKGROUND_THRESHOLD = 3;
	
	//if an edge (edge of the frame) macroblock has a count of neighbor macroblocks that 
	//are assigned to the background, less than or equal to this value, 
	//then the edge macroblock is assigned to the foreground
	private static final int EDGE_FOREGROUND_THRESHOLD = 1;
	
	//if a macroblock has a count of neighbor macroblocks that 
	//are assigned to the background, greater than or equal to this value, 
	//then the macroblock is assigned to the background
	private static final int STD_BACKGROUND_THRESHOLD = 6;
	
	//if a macroblock has a count of neighbor macroblocks that 
	//are assigned to the background, less than or equal to this value, 
	//then the macroblock is assigned to the foreground
	private static final int STD_FOREGROUND_THRESHOLD = 4;

	
	
	/**
	 * Static method to create video frames. Creates MacroBlocks, assigns them
	 * to a layer, and creates DCTBlocks
	 * @param parentVid the parent CompressedVideo
	 * @return an array of VideoFrames
	 */
	static VideoFrame[] createFrames(CompressedVideo parentVid) {
		
		VideoFrame[] videoFrames = new VideoFrame[parentVid.numOfFrames];
		
		System.out.println("Frames to load: " + parentVid.numOfFrames);
		
		//for each frame create macroBlocks and DCTs
		for (int frameNum = 0; frameNum < parentVid.numOfFrames; frameNum++) {
			videoFrames[frameNum] = new VideoFrame();
			videoFrames[frameNum].macroBlocks = MacroBlock.createMacroBlocksForFrame(parentVid, frameNum);
			videoFrames[frameNum].assignLayers(parentVid);
			videoFrames[frameNum].dctBlocks = DCTBlock.createDCTBlocksForFrame(parentVid, frameNum);
			System.out.println("Loaded frame " + (frameNum+1) + "/" + parentVid.numOfFrames);
		}
		
		
	
		return videoFrames;
	}

	/**
	 * Method that creates one VideoFrame's image. The method loops through each DCTBlock
	 * of the frame, checks if it is in the foreground or background, divides the 
	 * DCT coefficients by the appropriate quantization value (or not if in gaze area), 
	 * rounds the value to the nearest integer, multiply by the quantization value again,
	 * take the inverse discrete cosine transform of the DCT coefficients, write those values
	 * to a buffered image, and return that image.
	 * @param video parent CompressedVideo
	 * @param gazeX x coordinate of mouse pointer, normalized for JFrame window
	 * @param gazeY y coordinate of mouse pointer, normalized for JFrame window
	 * @param gazeOn is the gaze control feature on
	 * @return current frame's image
	 */
	BufferedImage getFrameImage(CompressedVideo video, int gazeX, int gazeY, boolean gazeOn) {
	

		BufferedImage curFrameImage = new BufferedImage(video.frameWidth, video.frameHeight, BufferedImage.TYPE_INT_RGB);
			
		// loop through each DCTBlock in the frame
		for (int dctBlockNum = 0; dctBlockNum < dctBlocks.length; dctBlockNum++) {
			
			// current DCT block
			DCTBlock curDCTBlock = dctBlocks[dctBlockNum];
			// temporary DCT block used to hold temporary result
			DCTBlock tempResultDCTBlock = new DCTBlock(video); 
			
			
			
			// get quantization value
			int quant = getQuantizationValue(video, curDCTBlock, dctBlockNum, gazeX, gazeY);
			
			// divide coefficients by quant, round, and then multiply by quant, emulates compression
			tempResultDCTBlock.quantizeCoefficients(video, quant, curDCTBlock);
			

			// transform DCT coefficients back to pixel values using IDCT
			tempResultDCTBlock.dctToIDCT(video);
		
		
			// write all rgb vals of the DCT block to image
			tempResultDCTBlock.writeToImage(video, curDCTBlock, dctBlockNum, curFrameImage);
			
		}
	
		return curFrameImage;
	}


	private int getQuantizationValue(CompressedVideo video, DCTBlock curDCTBlock, int dctBlockNum, int gazeX, int gazeY) {
		int dctBlockX = curDCTBlock.getX(video, dctBlockNum);
		int dctBlockY = curDCTBlock.getY(video, dctBlockNum);
		int quant;
		if ((dctBlockX <= gazeX + (video.gazeSize / 2)) && (dctBlockX >= gazeX - (video.gazeSize / 2))
			&& (dctBlockY <= gazeY + (video.gazeSize / 2)) && (dctBlockY >= gazeY - (video.gazeSize / 2))) {
			quant = 1;
		}
		else if (macroBlocks[MacroBlock.getMacroBlockIndexX(video, dctBlockX)][MacroBlock.getMacroBlockIndexY(video, dctBlockY)].foreground) {
			quant = video.foregroundQuant;
		}
		else {
			quant = video.backgroundQuant;
		}
		return quant;
	}

	private void assignLayers(CompressedVideo parentVid) {
		

		
		// get frame average motion vectors
		float xAvg = getMotionVectorAverageX(parentVid);
		float yAvg = getMotionVectorAverageY(parentVid);

		
		int numOfForegroundBlocks = 0;
		int numOfRuns = 0;
		int lowerMotionVectorThreshold = 0;
		
		// main algorithm for initial assignment of macroblocks to foreground or background, 
		// runs until enough blocks are brought to foreground or too many runs happen
		while (numOfForegroundBlocks < MIN_NUM_BLOCKS && numOfRuns < MAX_NUM_RUNS) {		
			numOfForegroundBlocks = initialLayerAssignment(parentVid, xAvg, yAvg, lowerMotionVectorThreshold);
			lowerMotionVectorThreshold += MOTION_VECTOR_DIFFERENCE_INCREMENT;
			numOfRuns++;
		}
		
			
		// filter foreground/background assignment by comparing with neighbors and reassign as needed
		numOfForegroundBlocks += filterLayerAssignment(parentVid);
		
		
		//expand foreground by one macroBlock one or more times
		numOfRuns = 0;
		if (numOfForegroundBlocks < parentVid.numOfMacroBlocksPerFrame/THREE) {
			numOfForegroundBlocks = expandForegroundByOne(parentVid);
		}
		while (numOfForegroundBlocks < MIN_NUM_BLOCKS * THREE && numOfRuns < MAX_NUM_RUNS) {
			numOfForegroundBlocks = expandForegroundByOne(parentVid);
			numOfRuns++;
		}
		
	}



	private int expandForegroundByOne(CompressedVideo parentVid) {
		int foregroundCount = 0;
		boolean[][] foreground = new boolean[parentVid.frameWidthPadded/parentVid.macroBlockSize][parentVid.frameHeightPadded/parentVid.macroBlockSize];

		for (int xIndex = 0; xIndex < parentVid.frameWidthPadded/parentVid.macroBlockSize; xIndex++) {
			for (int yIndex = 0; yIndex < parentVid.frameHeightPadded/parentVid.macroBlockSize; yIndex++) {
				if (macroBlocks[xIndex][yIndex].foreground) {
					foreground[xIndex][yIndex] = true;	
					if (inbounds(xIndex - 1, yIndex, parentVid)) foreground[xIndex - 1][yIndex] = true;
					if (inbounds(xIndex + 1, yIndex, parentVid)) foreground[xIndex + 1][yIndex] = true;
					if (inbounds(xIndex, yIndex - 1, parentVid)) foreground[xIndex][yIndex - 1] = true;
					if (inbounds(xIndex, yIndex + 1, parentVid)) foreground[xIndex][yIndex + 1] = true;
				}
				else {
					foreground[xIndex][yIndex] = false;
				}
			}
		}
		
		for (int xIndex = 0; xIndex < parentVid.frameWidthPadded/parentVid.macroBlockSize; xIndex++) {
			for (int yIndex = 0; yIndex < parentVid.frameHeightPadded/parentVid.macroBlockSize; yIndex++) {
				if (foreground[xIndex][yIndex]) {
					macroBlocks[xIndex][yIndex].foreground = true;
					foregroundCount++;
				}
				else {
					macroBlocks[xIndex][yIndex].foreground = false;
				}
			}
		}

		return foregroundCount;
	}

	private int filterLayerAssignment(CompressedVideo parentVid) {
		int countAddedToForeground = 0; //can be negative
		
		for (int xIndex = 0; xIndex < parentVid.frameWidthPadded/parentVid.macroBlockSize; xIndex++) {
			for (int yIndex = 0; yIndex < parentVid.frameHeightPadded/parentVid.macroBlockSize; yIndex++) {
				int numNeighborsAreBackground = macroBlocks[xIndex][yIndex].getNumNeighborsBackground(parentVid, this, xIndex, yIndex);
				
				//has 3 neighbors
				if (macroBlocks[xIndex][yIndex].isCorner(parentVid, xIndex, yIndex)) {
					if (numNeighborsAreBackground >= CORNER_NEIGHBOR_BACKGROUND_THRESHOLD) {
						macroBlocks[xIndex][yIndex].foreground = false;
						countAddedToForeground--;
					}
					else {
						macroBlocks[xIndex][yIndex].foreground = true;
						countAddedToForeground++;
					}
				}
				//has 5 neighbors
				else if (macroBlocks[xIndex][yIndex].isEdge(parentVid, xIndex, yIndex)) {
					if (numNeighborsAreBackground >= EDGE_BACKGROUND_THRESHOLD) {
						macroBlocks[xIndex][yIndex].foreground = false;
						countAddedToForeground--;
					}
					else if(numNeighborsAreBackground <= EDGE_FOREGROUND_THRESHOLD) {
						macroBlocks[xIndex][yIndex].foreground = true;
						countAddedToForeground++;
					}
				}
				// has 8 neighbors
				else {
					if (numNeighborsAreBackground >= STD_BACKGROUND_THRESHOLD) {
						macroBlocks[xIndex][yIndex].foreground = false;
						countAddedToForeground--;
					}
					if (numNeighborsAreBackground <= STD_FOREGROUND_THRESHOLD) {
						macroBlocks[xIndex][yIndex].foreground = true;
						countAddedToForeground++;
					}
				}
			}
		}
		

		return countAddedToForeground;
	}

	private int initialLayerAssignment(CompressedVideo parentVid, float xAvg, float yAvg, int lowerMotionVectorThreshold) {
		int foregroundCount = 0;
		
		for (int xIndex = 0; xIndex < parentVid.frameWidthPadded/parentVid.macroBlockSize; xIndex++) {
			for (int yIndex = 0; yIndex < parentVid.frameHeightPadded/parentVid.macroBlockSize; yIndex++) {
				if (macroBlocks[xIndex][yIndex].errorSAD < FOREGROUND_SAD_UPPER_ERROR_THRESHOLD && 
					macroBlocks[xIndex][yIndex].errorSAD > FOREGROUND_SAD_LOWER_ERROR_THRESHOLD &&
					(Math.abs(xAvg - macroBlocks[xIndex][yIndex].getxMotionVector()) + Math.abs(yAvg - macroBlocks[xIndex][yIndex].getyMotionVector()) > 
					(MOTION_VECTOR_DIFFERENCE_THRESHOLD - lowerMotionVectorThreshold))) {
					
					macroBlocks[xIndex][yIndex].foreground = true;
					foregroundCount++;
				}
				else {
					macroBlocks[xIndex][yIndex].foreground = false;
				}
		
			}
		}

		return foregroundCount;
	}

	private float getMotionVectorAverageX(CompressedVideo parentVid) {
		float xAvg = 0;
		
		for (int x = 0; x < parentVid.frameWidthPadded/parentVid.macroBlockSize; x++) {
			for (int y = 0; y < parentVid.frameHeightPadded/parentVid.macroBlockSize; y++) {
				xAvg += macroBlocks[x][y].getxMotionVector();	
			}
		}
		 
		xAvg /= parentVid.numOfMacroBlocksPerFrame;
		return xAvg;
	}
	
	private float getMotionVectorAverageY(CompressedVideo parentVid) {
		float yAvg = 0;
		
		for (int x = 0; x < parentVid.frameWidthPadded/parentVid.macroBlockSize; x++) {
			for (int y = 0; y < parentVid.frameHeightPadded/parentVid.macroBlockSize; y++) {
				yAvg += macroBlocks[x][y].getyMotionVector();	
			}
		}
		 
		yAvg /= parentVid.numOfMacroBlocksPerFrame;
		return yAvg;
	}

	private boolean inbounds(int xIndex, int yIndex, CompressedVideo parentVid) {
		if (xIndex < 0 || yIndex < 0) return false;
		if (xIndex >= parentVid.frameWidthPadded/parentVid.macroBlockSize ||
			yIndex >= parentVid.frameHeightPadded/parentVid.macroBlockSize) return false;
		return true;

	}
	

}
