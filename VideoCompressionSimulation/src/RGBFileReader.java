import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


/**
 * Class used to read a video file in an .rgb format, and outputs file into
 * a byte array format which includes a computed grayscale channel y.
 * COPYRIGHT (C) 2017 John Leibowitz. All Rights Reserved.
 * @author John Leibowitz
 * @version 1.00
 */
class RGBFileReader {
	
	private CompressedVideo video;
	
	/**
	 * RGB to Gray weights from: https://en.wikipedia.org/wiki/Grayscale#Converting_color_to_grayscale
	 */
	private static final float R_TO_GRAY_WEIGHT = 0.299f;
	private static final float G_TO_GRAY_WEIGHT = 0.587f;
	private static final float B_TO_GRAY_WEIGHT = 0.114f;
	
	RGBFileReader(CompressedVideo video) {
		this.video = video;
	}
	
	/**
	 * The skeleton of this method was provided by instructor and then modified 
	 * by myself to include padding and Y channel
	 */
	byte[] getBytes(File file){
		
		System.out.println("Loading file...");
		
		byte[] bytes = null; 
		InputStream inputStream = null;
		
		try {
			inputStream = new FileInputStream(file);
			//making room for any needed padding to fit whole macro blocks, 
			//also making room for additional Y channel
			long numOfBytes = video.frameHeightPadded * video.frameWidthPadded * video.numOfFrames * CompressedVideo.NUM_CHANNELS_RGBY;
			bytes = new byte[(int)numOfBytes];
	
			int offset = 0;
			int numRead = 0;
		
			//read bytes one frame at time, one row at a time
			for (int curChannelFrame = 0; curChannelFrame < video.numOfFrames * CompressedVideo.NUM_CHANNELS_RGB; curChannelFrame++) {
				for (int curRow = 0; curRow < video.frameHeight; curRow++) {
					//read row
					numRead = 0;
					while (numRead < video.frameWidth - 1) {
						numRead += inputStream.read(bytes, offset + numRead, video.frameWidth - numRead);	
					}
					offset += numRead;
					//pad end columns of row if needed
					if (video.frameWidthPadded > video.frameWidth) {
						for (int i = 0; i < video.frameWidthPadded - video.frameWidth; i ++) {
							bytes[offset + i] = bytes[offset - 1];
						}
						offset += video.frameWidthPadded - video.frameWidth;
					}
				}
				//pad last rows if needed with a copy of the last row
				if (video.frameHeightPadded > video.frameHeight) {
					for (int i = 0; i < video.frameHeightPadded - video.frameHeight; i ++) {
						for (int j = 0; j < video.frameWidthPadded; j++) {
							bytes[offset + (i * video.frameWidthPadded) + j] = bytes[offset - video.frameWidthPadded + j];
						}
					}
					offset += video.frameWidthPadded * (video.frameHeightPadded - video.frameHeight);
				}
				
				//call method to calculate Y channel and then blur
				if ((curChannelFrame + 1) % 3 == 0) {
					writeOneGrayFrame(bytes, offset);
					blurOneGrayFrame(bytes, offset);
					offset += video.frameSizePadded;
				}
				
			}
	
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return bytes;
	}

	private void blurOneGrayFrame(byte[] bytes, int offset) {
		byte[] oneFrameTempBytes = new byte[video.frameSizePadded];
		System.arraycopy(bytes, offset, oneFrameTempBytes, 0, video.frameSizePadded);
		oneFrameTempBytes = getAvgBytes(oneFrameTempBytes);
		System.arraycopy(oneFrameTempBytes, 0, bytes, offset, video.frameSizePadded);
	}
	
	/**
	 *  This weighted average implementation idea came from article at: http://www.devx.com/dotnet/Article/45039
	 * 	gets weight based on location
	 *  dir map:
	 *  0|1|2
	 *  3|4|5
	 *  6|7|8	
	 */
	private int getWeight(int dir) {
		if (dir==4) return 4;
		if (dir==0 || dir==2 || dir==6 || dir==8) return 1;
		if (dir==1 || dir==3 || dir==5 || dir==7) return 2;
		return 0;
	}

	private byte[] getAvgBytes(byte[] oldBytes) {
		byte[] newBytes = new byte[oldBytes.length];
		int index = 0;
		int sum = 0;
		int numPix = 0;
		int tempIndex = 0;
		final int NINE_POS = 9;
		
		while (index < oldBytes.length) {
			sum = 0;
			numPix = 0;
			for (int i = 0; i < NINE_POS; i++) {
				tempIndex = getAdjIndex(index, i);
				if (tempIndex >= 0) {
					sum += getWeight(i) * (oldBytes[tempIndex] & 0xff);
					numPix += getWeight(i);
				}
			}

			newBytes[index] = (byte) (sum/numPix);
			index++;
		}
		
		return newBytes;
	}
	
	private int getAdjIndex(int index, int dir) {
		if (dir==4) return index;
		
		int dif = 0;
		int rowVal = video.frameWidthPadded;
				
		if (dir==0 || dir==3 || dir==6) {
			if (checkLeftEdge(index)) {
				return -1;
			}
			dif--;
		}

		if (dir==0 || dir==1 || dir==2) {
			if (checkTopEdge(index)) {
				return -1;
			}
			dif -= rowVal;
		}
		
		if (dir==2 || dir==5 || dir==8) {
			if (checkRightEdge(index)) {
				return -1;
			}
			dif++;
		}
		
		if (dir==6 || dir==7 || dir==8) {
			if (checkBottomEdge(index)) {
				return -1;
			}
			dif += rowVal;
		}
		
		return index + dif;
		
	}
	
	private boolean checkLeftEdge(int index) {
		int width = video.frameWidthPadded;
		index %= width;
		return (index == 0);
	}
	
	private boolean checkRightEdge(int index) {
		int width = video.frameWidthPadded;
		index %= width;
		return (index == width - 1);
	}
	
	private boolean checkTopEdge(int index) {
		int width = video.frameWidthPadded;
		int height = video.frameHeightPadded;
		int colorSize = height * width;
		index %= colorSize;
		return (index >= 0 && index < width);
	}
	
	private boolean checkBottomEdge(int index) {
		int width = video.frameWidthPadded;
		int height = video.frameHeightPadded;
		int colorSize = height * width;
		index %= colorSize;
		return (index >= colorSize - width);
	}
	
	/**
	 * Helper method for getBytes to write one Gray (Y channel) frame at a time
	 */
	private void writeOneGrayFrame(byte[] bytes, int offset) {
		int startPos = offset - (video.frameSizePadded * CompressedVideo.NUM_CHANNELS_RGB);
		for (int i = 0; i < video.frameSizePadded; i++) {
			int tempR = (int) ((bytes[startPos + i] & 0xff) * R_TO_GRAY_WEIGHT);
			int tempG = (int) ((bytes[startPos + i + video.frameSizePadded] & 0xff) * G_TO_GRAY_WEIGHT);
			int tempB = (int) ((bytes[startPos + i + (video.frameSizePadded * 2)] & 0xff) * B_TO_GRAY_WEIGHT);
			int tempGray = (tempR + tempG + tempB > 255) ? 255 : tempR + tempG + tempB;   
			if (tempGray < 0) tempGray = 0;
			bytes[offset + i] = (byte) tempGray;
		}
		
	}
}
