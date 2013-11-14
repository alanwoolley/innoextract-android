package uk.co.armedpineapple.innoextract;

import java.io.IOException;
import java.io.InputStream;

public class NativeInputStream extends InputStream {
	   public static final int STDOUT = 1;
	   public static final int STDERR = 2;
	 
	   public NativeInputStream(int nativeFileNo) throws IOException   {
	      init(nativeFileNo);
	   }
	 
	   /** Create a pipe to capture output from native stdout file descriptor into this InputStream */
	   private native void init(int nativeFileNo) throws IOException;
	 
	   @Override
	   public native int read() throws IOException;
	 
	   @Override
	   public native void close() throws IOException;
	}