/*
 *  Copyright 2004-2012 Robert Brandner (robert.brandner@gmail.com) 
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
package panama.heureka.media;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.grlea.log.SimpleLogger;

import com.gurge.gifencoder.GIFEncoder;

/**
 * @author Ridcully
 *
 */
public class MediaSupport {

	public final static String CONTENTTYPE_IMAGE_JPEG = "image/jpeg";
	public final static String CONTENTTYPE_IMAGE_GIF = "image/gif";
	public final static String CONTENTTYPE_IMAGE_PNG = "image/png";	
	
	/** Logging */
	protected static SimpleLogger log = new SimpleLogger(MediaSupport.class);
	
	/**
	 * Creates a different flavor of a given image.
	 * 
	 * @param imageData Document data as from upload.
	 * @return data for resized JPG image.
	 */
	public static synchronized byte[] createImageFlavor(byte[] imageData, String contentType, int width, int height, boolean keepRatio, boolean clipToFit, float jpegQuality) {
		try {
			InputStream is = new BufferedInputStream(new ByteArrayInputStream(imageData));
			Image src = ImageIO.read(is);
			int srcWidth = src.getWidth(null);
			int srcHeight = src.getHeight(null);
			
			// determine destination size
			int clipX = 0;
			int clipY = 0;
			int clipW = srcWidth;
			int clipH = srcHeight;
			int destWidth = width == -1 ? srcWidth : width;
			int destHeight = height == -1 ? srcHeight : height;
			if (keepRatio) {
				double destRatio = (double)destWidth / (double)destHeight;
				double srcRatio = (double)srcWidth / (double)srcHeight;
				if (destRatio < srcRatio) {
					if (clipToFit) { 
						clipW = (int)(srcHeight * destRatio);
						clipX = (srcWidth - clipW) / 2;
					} else { 
						destHeight = (int)(destWidth / srcRatio); 
					}
				} else {
					if (clipToFit) { 
						clipH = (int)(srcWidth / destRatio);
						clipY = (srcHeight - clipH) / 2;
					} else { 
						destWidth = (int)(destHeight * srcRatio); 
					}
				}
			}
			
			// draw original image to destination image object and
			// scale it to the new size on-the-fly
			BufferedImage destImage = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics2D = destImage.createGraphics();
			
			if (destWidth < srcWidth && destHeight < srcHeight) {	// if downsizing, use AreaAverage filter:
				if (clipX != 0 || clipY != 0) {						// apply clipping to src image
					BufferedImage clippedImage = new BufferedImage(clipW, clipH, BufferedImage.TYPE_INT_RGB);
					Graphics2D clipGfx = clippedImage.createGraphics();
					clipGfx.drawImage(src, 0, 0, clipW, clipH, clipX, clipY, clipX+clipW, clipY+clipH, null);
					src = clippedImage;
				}
				AreaAveragingScaleFilter scalefilter = new AreaAveragingScaleFilter(destWidth, destHeight);
				ImageProducer ip = new FilteredImageSource(src.getSource(), scalefilter);
				src = Toolkit.getDefaultToolkit().createImage(ip);
				clipX =	clipY = 0;
				clipW = destWidth;
				clipH = destHeight;
			}
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics2D.drawImage(src, 0, 0, destWidth, destHeight, clipX, clipY, clipX+clipW, clipY+clipH, null);
			
			// save destination image to byte array
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			BufferedOutputStream out = new BufferedOutputStream(outStream);
			
			if (CONTENTTYPE_IMAGE_JPEG.equals(contentType)) {
				// ImageIO.write(destImage, "jpg", out); <-- simple but cannot control quality
				// http://www.universalwebservices.net/web-programming-resources/java/adjust-jpeg-image-compression-quality-when-saving-images-in-java
				Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
				ImageWriter writer = iter.next();
				ImageWriteParam iwp = writer.getDefaultWriteParam();
				iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				iwp.setCompressionQuality(jpegQuality);   // a float between 0 and 1, 1 == best-quality
				ImageOutputStream imgOut = new MemoryCacheImageOutputStream(out);
				writer.setOutput(imgOut);
				IIOImage image = new IIOImage(destImage, null, null);
				writer.write(null, image, iwp);
				writer.dispose();
			} else if (CONTENTTYPE_IMAGE_GIF.equals(contentType)) {
				GIFEncoder encoder = new GIFEncoder(destImage);
				encoder.Write(out);        	
			} else if (CONTENTTYPE_IMAGE_PNG.equals(contentType)) {
				ImageIO.write(destImage, "png", out);
			} else {
				log.error("Content-Type '"+contentType+"' is not supported by MediaSupport. No image was created.");
			}
			out.close();
			return outStream.toByteArray();
		} catch (Exception e) {
			log.errorException(e);
			return null;
		}
	}
}
