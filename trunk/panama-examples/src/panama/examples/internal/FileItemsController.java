/*
a *  Copyright 2004-2010 Robert Brandner (robert.brandner@gmail.com)
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
package panama.examples.internal;

import java.io.IOException;
import java.io.OutputStream;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;

import org.apache.commons.fileupload.FileItem;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.core.BaseController;
import panama.core.Target;
import panama.form.FileItemField;
import panama.form.Form;
import panama.form.FormData;
import panama.form.StringField;
//import panama.heureka.media.MediaSupport;

/**
 * @author ridcully
 *
 */
@Controller(alias="fileitems_internal", defaultAction="view")
public class FileItemsController extends BaseController {

	private final static Form form = new Form(
			new StringField("msg"),
			new FileItemField("attachment")
	);

	@Action
	public Target view() {
		return render("fileitems_view.vm");
	}

//	@Action
//	public Target save() throws MagicParseException, MagicMatchNotFoundException, MagicException {
//		if (context.getParameter("submit") != null) {
//			FormData fd = new FormData(form).withDataFromRequest(context);
//			context.put("msg", fd.getString("msg"));
//			context.put("attachment", fd.getFileItem("attachment"));
//			context.put("showresults", Boolean.TRUE);
//			FileItem it = fd.getFileItem("attachment");
//			byte[] originalData = it.get();
//			String srcMimeType = Magic.getMagicMatch(originalData).getMimeType();
//			byte[] data = MediaSupport.createImageFlavor(it.get(), srcMimeType, MediaSupport.CONTENTTYPE_IMAGE_JPEG, 180, 180, true, false, 0.75f);
//
//			OutputStream out;
//			context.getResponse().setContentType(MediaSupport.CONTENTTYPE_IMAGE_JPEG);
//			context.getResponse().setContentLength(data.length);
//			try {
//				out = context.getResponse().getOutputStream();
//				out.write(data);
//				out.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		return null;
//		//return view();
//	}
}
