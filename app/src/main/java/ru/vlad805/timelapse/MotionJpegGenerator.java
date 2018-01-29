package ru.vlad805.timelapse;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

public class MotionJpegGenerator {

	FileChannel aviChannel = null;
	private File aviFile = null;
	long aviMovieOffset = 0;
	FileOutputStream aviOutput = null;
	double framerate = 0.0d;
	int height = 0;
	AVIIndexList indexlist = null;
	int numFrames = 0;
	long riffOffset = 0;
	int width = 0;

	private class AVIIndex {
		public int dwFlags = 16;
		public int dwOffset = 0;
		public int dwSize = 0;
		public byte[] fcc = new byte[]{(byte) 48, (byte) 48, (byte) 100, (byte) 98};

		public AVIIndex(int dwOffset, int dwSize) {
			this.dwOffset = dwOffset;
			this.dwSize = dwSize;
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(this.fcc);
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwFlags)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwOffset)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwSize)));
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIIndexList {
		public int cb = 0;
		public byte[] fcc = new byte[]{(byte) 105, (byte) 100, (byte) 120, (byte) 49};
		public ArrayList<AVIIndex> ind = new ArrayList<>();

		public void addAVIIndex(int dwOffset, int dwSize) {
			this.ind.add(new AVIIndex(dwOffset, dwSize));
		}

		public byte[] toBytes() throws Exception {
			this.cb = this.ind.size() * 16;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(this.fcc);
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.cb)));
			for (int i = 0; i < this.ind.size(); i++) {
				baos.write(((AVIIndex) this.ind.get(i)).toBytes());
			}
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIJunk
	{
		public byte[] fcc = new byte[]{'J','U','N','K'};
		public int size = 1808;
		public byte[] data = new byte[size];

		public AVIJunk()
		{
			Arrays.fill(data,(byte)0);
		}

		public byte[] toBytes() throws Exception
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(size)));
			baos.write(data);
			baos.close();

			return baos.toByteArray();
		}
	}

	private class AVIMainHeader {
		public int cb = 56;
		public int dwFlags = 65552;
		public int dwHeight = 0;
		public int dwInitialFrames = 0;
		public int dwMaxBytesPerSec = 10000000;
		public int dwMicroSecPerFrame = 0;
		public int dwPaddingGranularity = 0;
		public int[] dwReserved = new int[4];
		public int dwStreams = 1;
		public int dwSuggestedBufferSize = 0;
		public int dwTotalFrames = 0;
		public int dwWidth = 0;
		public byte[] fcc = new byte[]{(byte) 97, (byte) 118, (byte) 105, (byte) 104};

		public AVIMainHeader() {
			this.dwMicroSecPerFrame = (int) ((1.0d / MotionJpegGenerator.this.framerate) * 1000000.0d);
			this.dwWidth = MotionJpegGenerator.this.width;
			this.dwHeight = MotionJpegGenerator.this.height;
			this.dwTotalFrames = MotionJpegGenerator.this.numFrames;
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(this.fcc);
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.cb)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwMicroSecPerFrame)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwMaxBytesPerSec)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwPaddingGranularity)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwFlags)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwTotalFrames)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwInitialFrames)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwStreams)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwSuggestedBufferSize)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwWidth)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwHeight)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwReserved[0])));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwReserved[1])));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwReserved[2])));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwReserved[3])));
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIMovieList {
		public byte[] fcc = new byte[]{(byte) 76, (byte) 73, (byte) 83, (byte) 84};
		public byte[] fcc2 = new byte[]{(byte) 109, (byte) 111, (byte) 118, (byte) 105};
		public int listSize = 0;

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(this.fcc);
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.listSize)));
			baos.write(this.fcc2);
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIStreamFormat {
		public short biBitCount = (short) 24;
		public int biClrImportant = 0;
		public int biClrUsed = 0;
		public byte[] biCompression = new byte[]{(byte) 77, (byte) 74, (byte) 80, (byte) 71};
		public int biHeight = 0;
		public short biPlanes = (short) 1;
		public int biSize = 40;
		public int biSizeImage = 0;
		public int biWidth = 0;
		public int biXPelsPerMeter = 0;
		public int biYPelsPerMeter = 0;
		public int cb = 40;
		public byte[] fcc = new byte[]{(byte) 115, (byte) 116, (byte) 114, (byte) 102};

		public AVIStreamFormat() {
			this.biWidth = MotionJpegGenerator.this.width;
			this.biHeight = MotionJpegGenerator.this.height;
			this.biSizeImage = MotionJpegGenerator.this.width * MotionJpegGenerator.this.height;
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(this.fcc);
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.cb)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.biSize)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.biWidth)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.biHeight)));
			baos.write(MotionJpegGenerator.shortBytes(MotionJpegGenerator.swapShort(this.biPlanes)));
			baos.write(MotionJpegGenerator.shortBytes(MotionJpegGenerator.swapShort(this.biBitCount)));
			baos.write(this.biCompression);
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.biSizeImage)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.biXPelsPerMeter)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.biYPelsPerMeter)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.biClrUsed)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.biClrImportant)));
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIStreamHeader {
		public int bottom = 0;
		public int cb = 64;
		public int dwFlags = 0;
		public int dwInitialFrames = 0;
		public int dwLength = 0;
		public int dwQuality = -1;
		public int dwRate = 1000000;
		public int dwSampleSize = 0;
		public int dwScale = 0;
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
			this.dwScale = (int) ((1.0d / MotionJpegGenerator.this.framerate) * 1000000.0d);
			this.dwLength = MotionJpegGenerator.this.numFrames;
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(this.fcc);
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.cb)));
			baos.write(this.fccType);
			baos.write(this.fccHandler);
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwFlags)));
			baos.write(MotionJpegGenerator.shortBytes(MotionJpegGenerator.swapShort(this.wPriority)));
			baos.write(MotionJpegGenerator.shortBytes(MotionJpegGenerator.swapShort(this.wLanguage)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwInitialFrames)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwScale)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwRate)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwStart)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwLength)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwSuggestedBufferSize)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwQuality)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.dwSampleSize)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.left)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.top)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.right)));
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.bottom)));
			baos.close();
			return baos.toByteArray();
		}
	}

	private class AVIStreamList {
		public byte[] fcc = new byte[]{(byte) 76, (byte) 73, (byte) 83, (byte) 84};
		public byte[] fcc2 = new byte[]{(byte) 115, (byte) 116, (byte) 114, (byte) 108};
		public int size = 124;

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(this.fcc);
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.size)));
			baos.write(this.fcc2);
			baos.close();
			return baos.toByteArray();
		}
	}

	public class RIFFHeader {
		public byte[] fcc = new byte[]{(byte) 82, (byte) 73, (byte) 70, (byte) 70};
		public byte[] fcc2 = new byte[]{(byte) 65, (byte) 86, (byte) 73, (byte) 32};
		public byte[] fcc3 = new byte[]{(byte) 76, (byte) 73, (byte) 83, (byte) 84};
		public byte[] fcc4 = new byte[]{(byte) 104, (byte) 100, (byte) 114, (byte) 108};
		public int fileSize = 0;
		public int listSize = 200;

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(this.fcc);
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.fileSize)));
			baos.write(this.fcc2);
			baos.write(this.fcc3);
			baos.write(MotionJpegGenerator.intBytes(MotionJpegGenerator.swapInt(this.listSize)));
			baos.write(this.fcc4);
			baos.close();
			return baos.toByteArray();
		}
	}

	public MotionJpegGenerator(File aviFile, int width, int height, double framerate, int numFrames) throws Exception {
		this.aviFile = aviFile;
		this.width = width;
		this.height = height;
		this.framerate = framerate;
		this.numFrames = numFrames;
		this.aviOutput = new FileOutputStream(aviFile);
		this.aviChannel = this.aviOutput.getChannel();
		this.aviOutput.write(new RIFFHeader().toBytes());
		this.aviOutput.write(new AVIMainHeader().toBytes());
		this.aviOutput.write(new AVIStreamList().toBytes());
		this.aviOutput.write(new AVIStreamHeader().toBytes());
		this.aviOutput.write(new AVIStreamFormat().toBytes());
		this.aviOutput.write(new AVIJunk().toBytes());
		this.aviMovieOffset = this.aviChannel.position();
		this.aviOutput.write(new AVIMovieList().toBytes());
		this.indexlist = new AVIIndexList();
	}

	public void addFrame(byte[] b) throws Exception {
		byte[] fcc = new byte[]{(byte) 48, (byte) 48, (byte) 100, (byte) 98};
		byte[] imagedata = b;
		int useLength = imagedata.length;
		long position = this.aviChannel.position();
		int extra = (((int) position) + useLength) % 4;
		if (extra > 0) {
			useLength += extra;
		}
		this.indexlist.addAVIIndex((int) position, useLength);
		this.aviOutput.write(fcc);
		this.aviOutput.write(intBytes(swapInt(useLength)));
		this.aviOutput.write(imagedata);
		if (extra > 0) {
			for (int i = 0; i < extra; i++) {
				this.aviOutput.write(0);
			}
		}
		imagedata = null;
	}

	public File getFile() {
		return aviFile;
	}

	public void finishAVI() throws Exception {
		byte[] indexlistBytes = this.indexlist.toBytes();
		this.aviOutput.write(indexlistBytes);
		this.aviOutput.close();
		long size = this.aviFile.length();
		RandomAccessFile raf = new RandomAccessFile(this.aviFile, "rw");
		raf.seek(4);
		raf.write(intBytes(swapInt(((int) size) - 8)));
		raf.seek(this.aviMovieOffset + 4);
		raf.write(intBytes(swapInt((int) (((size - 8) - this.aviMovieOffset) - ((long) indexlistBytes.length)))));
		raf.close();
	}

	public void fixAVI(int frameCount) throws Exception {
		this.numFrames = frameCount;
		RandomAccessFile raf = new RandomAccessFile(this.aviFile, "rw");
		RIFFHeader rh = new RIFFHeader();
		raf.seek(0);
		raf.write(rh.toBytes());
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
