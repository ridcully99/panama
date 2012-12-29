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
package panama.core;

import java.io.IOException;

/**
 * @author Robert
 */
public class RedirectTarget extends Target {

	private String url;
	
	public RedirectTarget(String url) {
		super();
		setUrl(url);
	}
	
	public void go() throws IOException {
		String url = getUrl();
		Context ctx = Context.getInstance();
		/* if url starts with / we prepend the contextPath (i.e. /<appname>) */
		if (url.startsWith("/")) {
			url = ctx.getRequest().getContextPath()+url;
		}		
		ctx.getResponse().sendRedirect(ctx.getResponse().encodeRedirectURL(url));
	}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
}
