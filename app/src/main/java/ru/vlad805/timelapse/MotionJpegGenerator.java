package ru.vlad805.timelapse;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

public class MotionJpegGenerator {

	private File mAviFile;
	private FileChannel mAviChannel;
	private long mAviMovieOffset;
	private FileOutputStream mAviOutput;
	private double mFrameRate;
	private AVIIndexList mIndexList;
	private int mFramesCount;
	private int mWidth;
	private int mHeight;

	private class AVIIndex {
		private int dwFlags = 16;
		private int dwOffset;
		private int dwSize;
		private byte[] fcc = new byte[]{(byte) 48, (byte) 48, (byte) 100, (byte) 98};

		public AVIIndex(int dwOffset, int dwSize) {
			this.dwOffset = dwOffset;
			this.dwSize = dwSize;
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(dwFlags)));
			baos.write(intBytes(swapInt(dwOffset)));
			baos.write(intBytes(swapInt(dwSize)));
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIIndexList {
		public int cb = 0;
		public byte[] fcc = new byte[]{(byte) 105, (byte) 100, (byte) 120, (byte) 49};
		public ArrayList<AVIIndex> ind = new ArrayList<>();

		public void addAVIIndex(int dwOffset, int dwSize) {
			ind.add(new AVIIndex(dwOffset, dwSize));
		}

		public byte[] toBytes() throws Exception {
			cb = ind.size() * 16;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(MotionJpegGenerator.intBytes(swapInt(cb)));
			for (AVIIndex anInd : ind) {
				baos.write(anInd.toBytes());
			}
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIJunk {
		private int size = 1808;
		private byte[] data = new byte[size];

		public AVIJunk() {
			Arrays.fill(data, (byte) 0);
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(new byte[]{'J', 'U', 'N', 'K'});
			baos.write(intBytes(swapInt(size)));
			baos.write(data);
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIMainHeader {
		public int cb = 56;
		public int dwFlags = 65552;
		public int dwHeight;
		public int dwInitialFrames = 0;
		public int dwMaxBytesPerSec = 10000000;
		public int dwMicroSecPerFrame;
		public int dwPaddingGranularity = 0;
		public int[] dwReserved = new int[4];
		public int dwStreams = 1;
		public int dwSuggestedBufferSize = 0;
		public int dwTotalFrames;
		public int dwWidth;
		public byte[] fcc = new byte[]{(byte) 97, (byte) 118, (byte) 105, (byte) 104};

		public AVIMainHeader() {
			dwMicroSecPerFrame = (int) ((1.0d / mFrameRate) * 1000000.0d);
			dwWidth = mWidth;
			dwHeight = mHeight;
			dwTotalFrames = mFramesCount;
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(cb)));
			baos.write(intBytes(swapInt(dwMicroSecPerFrame)));
			baos.write(intBytes(swapInt(dwMaxBytesPerSec)));
			baos.write(intBytes(swapInt(dwPaddingGranularity)));
			baos.write(intBytes(swapInt(dwFlags)));
			baos.write(intBytes(swapInt(dwTotalFrames)));
			baos.write(intBytes(swapInt(dwInitialFrames)));
			baos.write(intBytes(swapInt(dwStreams)));
			baos.write(intBytes(swapInt(dwSuggestedBufferSize)));
			baos.write(intBytes(swapInt(dwWidth)));
			baos.write(intBytes(swapInt(dwHeight)));
			baos.write(intBytes(swapInt(dwReserved[0])));
			baos.write(intBytes(swapInt(dwReserved[1])));
			baos.write(intBytes(swapInt(dwReserved[2])));
			baos.write(intBytes(swapInt(dwReserved[3])));
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIMovieList {
		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(new byte[]{(byte) 76, (byte) 73, (byte) 83, (byte) 84});
			baos.write(intBytes(swapInt(0)));
			baos.write(new byte[]{(byte) 109, (byte) 111, (byte) 118, (byte) 105});
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIStreamFormat {
		public short biBitCount = (short) 24;
		public int biClrImportant = 0;
		public int biClrUsed = 0;
		public byte[] biCompression = new byte[]{(byte) 77, (byte) 74, (byte) 80, (byte) 71};
		public int biHeight;
		public short biPlanes = (short) 1;
		public int biSize = 40;
		public int biSizeImage;
		public int biWidth;
		public int biXPelsPerMeter = 0;
		public int biYPelsPerMeter = 0;
		public int cb = 40;
		public byte[] fcc = new byte[]{(byte) 115, (byte) 116, (byte) 114, (byte) 102};

		public AVIStreamFormat() {
			biWidth = mWidth;
			biHeight = mHeight;
			biSizeImage = mWidth * mHeight;
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(cb)));
			baos.write(intBytes(swapInt(biSize)));
			baos.write(intBytes(swapInt(biWidth)));
			baos.write(intBytes(swapInt(biHeight)));
			baos.write(shortBytes(swapShort(biPlanes)));
			baos.write(shortBytes(swapShort(biBitCount)));
			baos.write(biCompression);
			baos.write(intBytes(swapInt(biSizeImage)));
			baos.write(intBytes(swapInt(biXPelsPerMeter)));
			baos.write(intBytes(swapInt(biYPelsPerMeter)));
			baos.write(intBytes(swapInt(biClrUsed)));
			baos.write(intBytes(swapInt(biClrImportant)));
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIStreamHeader {
		public int bottom = 0;
		public int cb = 64;
		public int dwFlags = 0;
		public int dwInitialFrames = 0;
		public int dwLength;
		public int dwQuality = -1;
		public int dwRate = 1000000;
		public int dwSampleSize = 0;
		public int dwScale;
		public int dwStart = 0;
		public int dwSuggestedBufferSize = 0;
		public byte[] fcc = new byte[]{(byte) 115, (byte) 116, (byte) 114, (byte) 104};
		public byte[] fccHandler = new byte[]{(byte) 77, (byte) 74, (byte) 80, (byte) 71};
		public byte[] fccType = new byte[]{(byte) 118, (byte) 105, (byte) 100, (byte) 115};
		public int left = 0;
		public int right = 0;
		public int top = 0;
		public short wLanguage = (short) 0;
		public short wPriority = (short) 0;

		public AVIStreamHeader() {
			dwScale = (int) ((1.0d / mFrameRate) * 1000000.0d);
			dwLength = mFramesCount;
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(cb)));
			baos.write(fccType);
			baos.write(fccHandler);
			baos.write(intBytes(swapInt(dwFlags)));
			baos.write(shortBytes(swapShort(wPriority)));
			baos.write(shortBytes(swapShort(wLanguage)));
			baos.write(intBytes(swapInt(dwInitialFrames)));
			baos.write(intBytes(swapInt(dwScale)));
			baos.write(intBytes(swapInt(dwRate)));
			baos.write(intBytes(swapInt(dwStart)));
			baos.write(intBytes(swapInt(dwLength)));
			baos.write(intBytes(swapInt(dwSuggestedBufferSize)));
			baos.write(intBytes(swapInt(dwQuality)));
			baos.write(intBytes(swapInt(dwSampleSize)));
			baos.write(intBytes(swapInt(left)));
			baos.write(intBytes(swapInt(top)));
			baos.write(intBytes(swapInt(right)));
			baos.write(intBytes(swapInt(bottom)));
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIStreamList {
		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(new byte[]{(byte) 76, (byte) 73, (byte) 83, (byte) 84});
			baos.write(intBytes(swapInt(124)));
			baos.write(new byte[]{(byte) 115, (byte) 116, (byte) 114, (byte) 108});
			baos.close();
			return baos.toByteArray();
		}
	}

	public class RIFFHeader {
		private final byte[] fcc = new byte[]{(byte) 82, (byte) 73, (byte) 70, (byte) 70};
		private final byte[] fcc2 = new byte[]{(byte) 65, (byte) 86, (byte) 73, (byte) 32};
		private final byte[] fcc3 = new byte[]{(byte) 76, (byte) 73, (byte) 83, (byte) 84};
		private final byte[] fcc4 = new byte[]{(byte) 104, (byte) 100, (byte) 114, (byte) 108};
		private int fileSize = 0;
		private int listSize = 200;

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(fileSize)));
			baos.write(fcc2);
			baos.write(fcc3);
			baos.write(intBytes(swapInt(listSize)));
			baos.write(fcc4);
			baos.close();
			return baos.toByteArray();
		}
	}

	public MotionJpegGenerator(File aviFile, int width, int height, double frameRate, int lcNumFrames) throws Exception {
		mAviFile = aviFile;
		mWidth = width;
		mHeight = height;
		mFrameRate = frameRate;
		mFramesCount = lcNumFrames;
		mAviOutput = new FileOutputStream(aviFile);
		mAviChannel = mAviOutput.getChannel();
		mAviOutput.write(new RIFFHeader().toBytes());
		mAviOutput.write(new AVIMainHeader().toBytes());
		mAviOutput.write(new AVIStreamList().toBytes());
		mAviOutput.write(new AVIStreamHeader().toBytes());
		mAviOutput.write(new AVIStreamFormat().toBytes());
		mAviOutput.write(new AVIJunk().toBytes());
		mAviMovieOffset = mAviChannel.position();
		mAviOutput.write(new AVIMovieList().toBytes());
		mIndexList = new AVIIndexList();
	}

	public void addFrame(byte[] b) throws Exception {
		int useLength = b.length;
		long position = mAviChannel.position();
		int extra = (((int) position) + useLength) % 4;
		if (extra > 0) {
			useLength += extra;
		}
		mIndexList.addAVIIndex((int) position, useLength);
		mAviOutput.write(new byte[]{(byte) 48, (byte) 48, (byte) 100, (byte) 98});
		mAviOutput.write(intBytes(swapInt(useLength)));
		mAviOutput.write(b);
		if (extra > 0) {
			for (int i = 0; i < extra; i++) {
				mAviOutput.write(0);
			}
		}
	}

	public File getFile() {
		return mAviFile;
	}

	public void finishAVI() throws Exception {
		byte[] indexListBytes = mIndexList.toBytes();
		mAviOutput.write(indexListBytes);
		mAviOutput.close();
		long size = mAviFile.length();
		RandomAccessFile raf = new RandomAccessFile(mAviFile, "rw");
		raf.seek(4);
		raf.write(intBytes(swapInt(((int) size) - 8)));
		raf.seek(mAviMovieOffset + 4);
		raf.write(intBytes(swapInt((int) (((size - 8) - mAviMovieOffset) - ((long) indexListBytes.length)))));
		raf.close();
	}

	public void fixAVI(int frameCount) throws Exception {
		mFramesCount = frameCount;
		RandomAccessFile raf = new RandomAccessFile(mAviFile, "rw");
		raf.seek(0);
		raf.write(new RIFFHeader().toBytes());
		raf.write(new AVIMainHeader().toBytes());
		raf.write(new AVIStreamList().toBytes());
		raf.write(new AVIStreamHeader().toBytes());
		raf.close();
	}

	public static int swapInt(int v) {
		return (((v >>> 24) | (v << 24)) | ((v << 8) & 16711680)) | ((v >> 8) & 65280);
	}

	public static short swapShort(short v) {
		return (short) ((v >>> 8) | (v << 8));
	}

	public static byte[] intBytes(int i) {
		return new byte[]{(byte) (i >>> 24), (byte) ((i >>> 16) & 255), (byte) ((i >>> 8) & 255), (byte) (i & 255)};
	}

	public static byte[] shortBytes(short i) {
		return new byte[]{(byte) (i >>> 8), (byte) (i & 255)};
	}
}
