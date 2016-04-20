package com.s24.image.improved.pngquant;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class Pngquant {

	private static final String LINUX_JNI_SO = "libpngquant.so";

	private static final String MACOSX_JNI_SO = "libpngquant.dylib";

	static {
		File file = deployJNI();
		if (null != file) {
			try {
				System.load(file.getCanonicalPath());
			} catch (IOException ignore) {
			}
		}
	}

	private static File deployJNI() {
		String osName = System.getProperty("os.name");
		String osArch = System.getProperty("os.arch");

		if ("Mac OS X".equals(osName) && "x86_64".equals(osArch)) {
			return copyFile(MACOSX_JNI_SO);
		}

		if ("Linux".equals(osName) && "amd64".equals(osArch)) {
			return copyFile(LINUX_JNI_SO);
		}

		throw new RuntimeException("Unsupported Operating System");
	}

	private static File copyFile(String fileName) {
		if (null == fileName || fileName.trim().length() == 0) {
			throw new IllegalArgumentException("fileName expected");
		}

		String[] segments = fileName.split("\\.");

		BufferedOutputStream bufferedOutputStream = null;
		BufferedInputStream bufferedInputStream = null;
		File file = null;
		try {
			file = File.createTempFile(segments[0], "." + segments[1]);
			file.deleteOnExit();
			InputStream inputStream = Pngquant.class.getClassLoader().getResourceAsStream(fileName);
			bufferedInputStream = new BufferedInputStream(inputStream);
			bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
			byte[] buffer = new byte[1024];
			int len = 0;
			while ((len = bufferedInputStream.read(buffer)) > 0) {
				bufferedOutputStream.write(buffer, 0, len);
			}
			bufferedOutputStream.flush();
		} catch (IOException e) {
			return null;
		} finally {
			if (null != bufferedInputStream) {
				try {
					bufferedInputStream.close();
				} catch (IOException ignore) {
				}
			}

			if (null != bufferedOutputStream) {
				try {
					bufferedOutputStream.close();
				} catch (IOException ignore) {
				}
			}
		}
		return file;
	}
	
	private native byte[] compress(byte[] in);
	
	public native void verbose(boolean enabled);
	
	private native void quality(int min, int max);

	
	public InputStream compress(InputStream in) throws IOException
	{		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = in.read(data, 0, data.length)) != -1) {
		  buffer.write(data, 0, nRead);
		}

		buffer.flush();
		
		
		return new ByteArrayInputStream(compress(buffer.toByteArray()));
	}
	
	
	/**
	 * print status messages
	 */
	public void verbose()
	{
		verbose(true);
	}
	
	/**
	 * don't save below min, use less colors below max (0-100)
	 * 
	 * @param min
	 * @param max
	 * 
	 * @return true if succeeded and min/max are between 0 and 100
	 */
	public boolean setQuality(int min, int max)
	{
		if (min >= 0 && min <= 100 && max >= 0 && max <= 100)
		{
			quality(min, max);
			
			return true;
		}
	
		return false;
	}
	
	/**
	 * use less colors below q
	 * don't save below automatic min
	 * 
	 * @param q if succeeded and q is between 0 and 100
	 */
	public boolean setQuality(int q)
	{
		if (q >= 0 && q <= 100)
		{
			quality(q, q*9/10);
			
			return true;
		}
		
		return false;
		
	}
	
}
