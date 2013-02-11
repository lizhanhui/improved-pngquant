import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class Pngquant {
	
	static {
	     System.loadLibrary("pngquant");
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
	 * @param true if succeeded and q is between 0 and 100
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
