import java.awt.image.BufferedImage;


/**
 * Class that represents one DCT (Discrete Cosine Transform) block for a 
 * CompressedVideo class. DCTBlocks are used to convert the RGB channels
 * of the video into frequency channels. Quantization is achieved by 
 * dividing the frequency values by the quantization values and then rounding.
 * The original signal can then be reconstructed by taking the IDCT
 * (Inverse Discrete Cosine Transform) of the frequency data, and then rounded
 * to the nearest integer.
 * COPYRIGHT (C) 2017 John Leibowitz. All Rights Reserved.
 * @author John Leibowitz
 * @version 1.00
 */
class DCTBlock {

	/**
	 * 3D array that stores DCT coefficients for this DCTBlock. First input is for color channel 0-2 for RGB.
	 * Second input is for x coordinate, which becomes u coordinate when transformed to frequency.
	 * Third input is for y coordinate, which becomes v coordinate when transformed to frequency.
	 */
	float[][][] dctCoefficients;
	
	/**
	 * Main constructor
	 * @param parentVid parent CompressedVideo
	 * @param frameNum frame number
	 * @param topLeftRow top left y coordinate
	 * @param topLeftCol top left x coordinate
	 */
	DCTBlock(CompressedVideo parentVid, int frameNum, int topLeftRow, int topLeftCol) {
		dctCoefficients = getDCT(parentVid, frameNum, topLeftRow, topLeftCol);
	}
	
	/**
	 * Secondary constructor for temporary DCTBlock
	 * @param parentVid parent CompressedVideo
	 */
	DCTBlock(CompressedVideo parentVid) {
		dctCoefficients = new float[CompressedVideo.NUM_CHANNELS_RGB][parentVid.dctBlockSize][parentVid.dctBlockSize];
	}
	
	
	/**
	 * cosTable[u][x] for Math.cos((((2*x)+1)*u*Math.PI)/(2*dctBlockSize))
	 * 
	 */
	static float[][] initCosTable(int dctBlockSize) {
		float[][] cosTable = new float[dctBlockSize][dctBlockSize];
		
		for (int u = 0; u < dctBlockSize; u++) {
			for (int x = 0; x < dctBlockSize; x++) {
				cosTable[u][x] = (float) Math.cos( (((2 * x) + 1) * u * Math.PI) / (2 * dctBlockSize) );
			}
		}
		return cosTable;
	}

	
	static DCTBlock[] createDCTBlocksForFrame(CompressedVideo parentVideo, int frameNum) {
		DCTBlock[] oneFrameOfDCTBlocks = new DCTBlock[parentVideo.frameSizePadded / (parentVideo.dctBlockSize * parentVideo.dctBlockSize)];
		
		int row = 0;
		int col = 0;
		
		for (int i = 0; i < oneFrameOfDCTBlocks.length; i++) {
			oneFrameOfDCTBlocks[i] = new DCTBlock(parentVideo, frameNum, row, col);
			col += parentVideo.dctBlockSize;
			if (col >= (parentVideo.frameWidthPadded - 1)) {
				col = 0;
				row += parentVideo.dctBlockSize;
			}
		}
		
		return oneFrameOfDCTBlocks;
	}


	
	int getX(CompressedVideo parentVideo, int blockNum) {
		int numBlocksPerRow = parentVideo.frameWidthPadded / parentVideo.dctBlockSize;
		return ((blockNum % numBlocksPerRow) * parentVideo.dctBlockSize) + (parentVideo.dctBlockSize/2);
	}


	int getY(CompressedVideo parentVideo, int blockNum) {
		int numBlocksPerRow = parentVideo.frameWidthPadded / parentVideo.dctBlockSize;
		return ((blockNum / numBlocksPerRow) * parentVideo.dctBlockSize) + (parentVideo.dctBlockSize/2);
	}

	int getTopLeftX(CompressedVideo parentVideo, int blockNum) {
		int numBlocksPerRow = parentVideo.frameWidthPadded / parentVideo.dctBlockSize;
		return ((blockNum % numBlocksPerRow) * parentVideo.dctBlockSize);
	}

	int getTopLeftY(CompressedVideo parentVideo, int blockNum) {
		int numBlocksPerRow = parentVideo.frameWidthPadded / parentVideo.dctBlockSize;
		return ((blockNum / numBlocksPerRow) * parentVideo.dctBlockSize);
	}

	void dctToIDCT(CompressedVideo parentVid) {
		float[][][] tempDCTCoefTable = new float[CompressedVideo.NUM_CHANNELS_RGB][parentVid.dctBlockSize][parentVid.dctBlockSize]; 
		final float scaleFactor = (2f / parentVid.dctBlockSize);
		final float ZERO_INDEX_FACTOR = (float) (1 / Math.sqrt(2));
		
		for (int channelNum = 0; channelNum < CompressedVideo.NUM_CHANNELS_RGB; channelNum++) {
			for (int x = 0; x < parentVid.dctBlockSize; x++) {
				for (int y = 0; y < parentVid.dctBlockSize; y++) {
					float result = 0;
					for (int u = 0; u < parentVid.dctBlockSize; u++) {
						for (int v = 0; v < parentVid.dctBlockSize; v++) {
							float fuvVal = dctCoefficients[channelNum][u][v];
							float partialResult = (fuvVal * parentVid.cosTable[u][x] * parentVid.cosTable[v][y]);
							if (u == 0) {
								partialResult *= ZERO_INDEX_FACTOR;
							}
							if (v == 0) {
								partialResult *= ZERO_INDEX_FACTOR;
							}							
							result += partialResult;
						}
					}
					result *= scaleFactor;
					if (result > 255) result = 255;
					if (result < 0) result = 0;
					tempDCTCoefTable[channelNum][x][y] = result;
				}
			}
		}
		dctCoefficients = tempDCTCoefTable;
		
	}

	
	void quantizeCoefficients(CompressedVideo video, int quant, DCTBlock curDCTBlock) {
		for (int channel = 0; channel < CompressedVideo.NUM_CHANNELS_RGB; channel++) {
			for (int j = 0; j < video.dctBlockSize; j++) {
				for (int k = 0; k < video.dctBlockSize; k++) {
					// this next statement emulates DCT quantization
					dctCoefficients[channel][j][k] = 
							Math.round(curDCTBlock.dctCoefficients[channel][j][k] / quant) * quant;
				}
			}
		}
		
	}

	void writeToImage(CompressedVideo video, DCTBlock curDCTBlock,
		int dctBlockNum, BufferedImage curFrameImage) {
	
		int topLeftCornerX = curDCTBlock.getTopLeftX(video, dctBlockNum);
		int topLeftCornerY = curDCTBlock.getTopLeftY(video, dctBlockNum);
		for (int x = 0; x < video.dctBlockSize; x++) {
			for (int y = 0; y < video.dctBlockSize; y++) {
			
				if (topLeftCornerX + x < video.frameWidth && topLeftCornerY + y < video.frameHeight) {
					byte r = (byte) dctCoefficients[CompressedVideo.Channel.RED.getColorNum()][x][y];
					byte g = (byte) dctCoefficients[CompressedVideo.Channel.GREEN.getColorNum()][x][y];
					byte b = (byte) dctCoefficients[CompressedVideo.Channel.BLUE.getColorNum()][x][y];
	
					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					curFrameImage.setRGB(topLeftCornerX + x, topLeftCornerY + y, pix);
				}
			}
		}
		
	}

	private float[][][] getDCT(CompressedVideo parentVid, int frameNum, int topLeftRow, int topLeftCol) {
		
		float[][][] dctCoefTable = new float[CompressedVideo.NUM_CHANNELS_RGB][parentVid.dctBlockSize][parentVid.dctBlockSize]; 
		final float scaleFactor = (2f / parentVid.dctBlockSize);
		final float ZERO_INDEX_FACTOR = (float) (1 / Math.sqrt(2));
		
		for (int channelNum = 0; channelNum < CompressedVideo.NUM_CHANNELS_RGB; channelNum++) {
			CompressedVideo.Channel channel = CompressedVideo.Channel.getChannel(channelNum);
			for (int u = 0; u < parentVid.dctBlockSize; u++) {
				for (int v = 0; v < parentVid.dctBlockSize; v++) {
					float result = 0;
					for (int x = 0; x < parentVid.dctBlockSize; x++) {
						for (int y = 0; y < parentVid.dctBlockSize; y++) {
							byte fxyVal = parentVid.getOneByte(frameNum, channel, topLeftRow + y, topLeftCol + x);
							result += ((fxyVal & 0xff) * parentVid.cosTable[u][x] * parentVid.cosTable[v][y]);
						}
					}
					if (u == 0) {
						result *= ZERO_INDEX_FACTOR;
					}
					if (v == 0) {
						result *= ZERO_INDEX_FACTOR;
					}
					result *= scaleFactor;
					dctCoefTable[channelNum][u][v] = result;
				}
			}
		}
		
		return dctCoefTable;
	}

	
	
}

