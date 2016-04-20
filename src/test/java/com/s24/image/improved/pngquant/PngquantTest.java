package com.s24.image.improved.pngquant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;



public class PngquantTest {

	Pngquant pngquant;

	@Before
	public void setUp() throws Exception {
		pngquant = new Pngquant();
		pngquant.setQuality(30);
	}

	private InputStream getTestImageInputStream() {
		return PngquantTest.class.getClassLoader().getResourceAsStream("test.png");
	}
	
	@Test
	public void test() throws IOException {
		
		InputStream stream = pngquant.compress(getTestImageInputStream());
		Assert.assertNotNull(stream);

		File file = File.createTempFile("compress", ".png");
		file.deleteOnExit();
		FileOutputStream fos = new FileOutputStream(file);
		byte[] buffer = new byte[1024];
		int len = 0;
		while ((len = stream.read(buffer)) > 0) {
			fos.write(buffer, 0, len);
		}
		fos.flush();
		fos.close();

		BufferedImage bufferedImage = ImageIO.read(file);

		BufferedImage compressedImage = ImageIO.read(getTestImageInputStream());
		Assert.assertEquals(bufferedImage.getWidth(), compressedImage.getWidth());
		Assert.assertEquals(bufferedImage.getHeight(), compressedImage.getHeight());

	}

}
