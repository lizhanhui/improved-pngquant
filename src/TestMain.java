import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class TestMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Pngquant png = new Pngquant();
		png.setQuality(20);
		
		try {
			
			InputStream stream = png.compress(new FileInputStream(new File("in.png")));		
			OutputStream out = new FileOutputStream(new File("out.png"));
			 
			int read = 0;
			byte[] bytes = new byte[1024];
		 
			while ((read = stream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			
			
			stream.close();
			out.flush();
			out.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
