
public class Pngquant {
	
	static {
	     System.loadLibrary("pngquant");
	}
	
	public native int compress(String in, String out);
	
	public native void verbose(boolean enabled);
	
	public native void force(boolean enabled);
	
	private native void quality(int min, int max);
	
	
	/**
	 * print status messages
	 */
	public void verbose()
	{
		verbose(true);
	}
	
	/**
	 * overwrite existing output files
	 */
	public void force()
	{
		force(true);
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
