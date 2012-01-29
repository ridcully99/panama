/**
 * 
 */
package panama.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

/**
 * @author robert.brandner
 *
 */
public class MyServletOutputStream extends ServletOutputStream {

	ByteArrayOutputStream bos;
	
	public MyServletOutputStream() {
		bos = new ByteArrayOutputStream();
	}
	
	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		bos.write(b);
	}
	
	@Override
	public void close() throws IOException {
		super.close();
		bos.close();
	}

	@Override
	public void flush() throws IOException {
		super.flush();
		bos.flush();
	}
	
	public byte[] getBytes() {
		return bos.toByteArray();
	}
}
