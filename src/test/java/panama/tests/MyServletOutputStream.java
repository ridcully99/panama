/*
 *  Copyright 2004-2016 Robert Brandner (robert.brandner@gmail.com) 
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License. 
 *  You may obtain a copy of the License at 
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *  
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
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
