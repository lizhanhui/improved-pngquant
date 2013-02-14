package com.s24.image.improved.pngquant;
import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;



public class PngquantTest {

	Pngquant pngquant;
	InputStream imagestream;
	BufferedImage image;
	
	@Before
	public void setUp() throws Exception {
		imagestream = new FileInputStream(new File("src/test/resources/tasche.png"));
		image = ImageIO.read(new FileInputStream(new File("src/test/resources/tasche.png")));
		pngquant = new Pngquant();
	}
	
	@Test
	public void test() throws IOException {
		
		InputStream stream = pngquant.compress(imagestream);
		BufferedImage res = ImageIO.read(stream);
		
		assertEquals(image.getHeight(), res.getHeight());
	}

}
