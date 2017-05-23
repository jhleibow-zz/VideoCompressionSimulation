
/**
 * Class that represents one macro block for a CompressedVideo class. 
 * MacroBlocks are used to calculate motion vectors of a group of pixels based
 * on least amount of error from the previous video frame using the 
 * Y (grayscale) channel. CompressedVideo's searchParamK variable specifies 
 * how large the search area is in the previous frame. Finally, a macro
 * block is assigned to either the background or foreground layer based
 * on macro block motion vectors relative to frame average motion vectors
 * and the error generated during the motion vector determination. 
 * COPYRIGHT (C) 2017 John Leibowitz. All Rights Reserved.
 * @author John Leibowitz
 * @version 1.00
 */
class MacroBlock {
	
	short xMotionVector;
	short yMotionVector;
	int errorSAD; // Error from SAD (Sum of Absolute Difference) during motion vector calculations
	boolean foreground;

	//TODO come back and make 1st frame motion blocks = 2nd frame
	MacroBlock(CompressedVideo parentVid, int frameNum, int yIndex, int xIndex) {
		//Calculates motion vectors based on prior frame using logarithmic search (except for first frame)
		int topLeftRow = yIndex * parentVid.macroBlockSize; 
		int topLeftCol = xIndex * parentVid.macroBlockSize;
		
		if (frameNum == 0) {
			xMotionVector = 0;
			yMotionVector = 0;
			return;
		}
		int searchParamK = parentVid.searchParamK;
		int curRunHomeTopLeftRow = topLeftRow;
		int curRunHomeTopLeftCol = topLeftCol;
		int nextRunHomeTopLeftRow = topLeftRow;
		int nextRunHomeTopLeftCol = topLeftCol;
		int tempError = 0;
		while(searchParamK > 1) {
			searchParamK /= 2;
			tempError = Integer.MAX_VALUE;
			int curError = 0;
			//search 9 positions +/- searchParamK (logarithmically shrinking)
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					int topLeftRowTarget = curRunHomeTopLeftRow + (i * searchParamK); 
					int topLeftColTarget = curRunHomeTopLeftCol + (j * searchParamK);
					if (blockInbound(topLeftRowTarget, topLeftColTarget, parentVid)) {
						curError = calcMacroBlockError(parentVid,frameNum,topLeftRow,topLeftCol,topLeftRowTarget,topLeftColTarget);
						if (i == 0 && j == 0 && curError <= tempError) {
							tempError = curError;
							nextRunHomeTopLeftRow = topLeftRowTarget;
							nextRunHomeTopLeftCol = topLeftColTarget;
						}
						else if (curError < tempError) {
							tempError = curError;
							nextRunHomeTopLeftRow = topLeftRowTarget;
							nextRunHomeTopLeftCol = topLeftColTarget;
						}
					}
				}
			}
			curRunHomeTopLeftRow = nextRunHomeTopLeftRow;
			curRunHomeTopLeftCol = nextRunHomeTopLeftCol;
		}
		xMotionVector = (short) (curRunHomeTopLeftCol - topLeftCol);
		yMotionVector = (short) (curRunHomeTopLeftRow - topLeftRow);
		errorSAD = tempError;
	}
	
	short getxMotionVector() {
		return xMotionVector;
	}

	short getyMotionVector() {
		return yMotionVector;
	}

	boolean isCorner(CompressedVideo parentVideo, int xIndex, int yIndex) {
		//if a corner
		if ((xIndex == 0 && yIndex == 0) || 
			(xIndex == 0 && yIndex >= (parentVideo.frameHeightPadded/parentVideo.macroBlockSize) - 1) ||
			(xIndex >= (parentVideo.frameWidthPadded/parentVideo.macroBlockSize) - 1 && yIndex == 0) ||
			(xIndex >= (parentVideo.frameWidthPadded/parentVideo.macroBlockSize) - 1 && 
			yIndex >= (parentVideo.frameHeightPadded/parentVideo.macroBlockSize) - 1)) {
			return true;
		}
		return false;
	}

	//note: returns true for corner
	boolean isEdge(CompressedVideo parentVideo,  int xIndex, int yIndex) {
		//if an edge
		if (xIndex == 0 || yIndex == 0 || xIndex >= (parentVideo.frameWidthPadded/parentVideo.macroBlockSize) - 1 ||
				yIndex >= (parentVideo.frameHeightPadded/parentVideo.macroBlockSize) - 1) {
			return true;
		}
		return false;
	}

	int getNumNeighborsBackground(CompressedVideo parentVid, VideoFrame vidFrame, int homeX, int homeY) {
			int result = 0;
	
			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					if (x != 0 || y !=0) {
						result += neighborIsBackground(parentVid, vidFrame, homeX + x, homeY + y);
					}
				}
			}
			
			return result;
		}

	static MacroBlock[][] createMacroBlocksForFrame(CompressedVideo parentVid, int frameNum) {
		MacroBlock[][] oneFrameOfMacroBlocks = 
				new MacroBlock[parentVid.frameWidthPadded / (parentVid.macroBlockSize)]
				[parentVid.frameHeightPadded / (parentVid.macroBlockSize)];
		
		for (int xIndex = 0; xIndex < parentVid.frameWidthPadded/parentVid.macroBlockSize; xIndex++) {
			for (int yIndex = 0; yIndex < parentVid.frameHeightPadded/parentVid.macroBlockSize; yIndex++) {
				oneFrameOfMacroBlocks[xIndex][yIndex] = new MacroBlock(parentVid, frameNum, yIndex, xIndex);
			}
		}
		
	
		return oneFrameOfMacroBlocks;
	}
	
	static int getMacroBlockIndexX(CompressedVideo parentVideo, int x) {
		return x / parentVideo.macroBlockSize;
	}

	static int getMacroBlockIndexY(CompressedVideo parentVideo, int y) {
		return y / parentVideo.macroBlockSize;
	}
	
	private int neighborIsBackground(CompressedVideo parentVideo, VideoFrame vidFrame, int targetX, int targetY) {
		
		//remove edge cases
		if (targetX < 0 || targetY < 0 || targetX >= (parentVideo.frameWidthPadded/parentVideo.macroBlockSize) 
				|| targetY >= (parentVideo.frameHeightPadded/parentVideo.macroBlockSize)) {
			return 0;
		}
		//return 0 for background
		if (vidFrame.macroBlocks[targetX][targetY].foreground) return 0;
		//else return 1
		return 1;
	}

	private boolean blockInbound(int topLeftRow, int topLeftCol, CompressedVideo parentVid) {
		if (topLeftRow < 0 || topLeftCol < 0) return false;
		if (topLeftRow + parentVid.macroBlockSize > parentVid.frameHeightPadded || 
				topLeftCol + parentVid.macroBlockSize > parentVid.frameWidthPadded) return false;
		return true;
	}

	/**
	 * Calculates potential macroblock motion vector error based on sum of absolute difference between pixels 
	 */
	private int calcMacroBlockError(CompressedVideo parentVid,
			int frameNum, 
			int topLeftRow, 
			int topLeftCol, 
			int topLeftRowTarget, 
			int topLeftColTarget) {
		
		int error = 0;
		for (int i = 0; i < parentVid.macroBlockSize; i++) {
			for (int j = 0; j < parentVid.macroBlockSize; j++) {
				error += Math.abs(parentVid.getOneByte(frameNum, CompressedVideo.Channel.GRAY, topLeftRow + i, topLeftCol + j) -
						parentVid.getOneByte(frameNum - 1, CompressedVideo.Channel.GRAY, topLeftRowTarget + i, topLeftColTarget + j));
			}
		}
		
		return error;
	}
}
