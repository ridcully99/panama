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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * @author Robert
 */
public class RedirectTarget extends Target {

	private String baseUrl;
	private Map<Object, Object> parameterMap = new HashMap<Object, Object>();
	private String anchor;
	
	public RedirectTarget(String url) {
		super();
		setBaseUrl(url);
	}
	
	/**
	 * Set parameters
	 * 
	 * @param parameterMap map of parameters
	 * @return the Target object, for fluid programming
	 */
	public RedirectTarget setParameters(Map<Object, Object> parameterMap) {
		this.parameterMap.clear();
		this.parameterMap.putAll(parameterMap);
		return this;
	}	
	
	/**
	 * Sets anchor part for the redirect target.
	 * @param anchor Anchor part, do not include the hash (#) symbol, it's added automatically
	 * @return
	 */
	public RedirectTarget withAnchor(String anchor) {
		this.anchor = anchor;
		return this;
	}
	
	public String getCompleteUrl() {
		String baseUrl = getBaseUrl();
		Context ctx = Context.getInstance();
		/* if baseUrl starts with / we prepend the contextPath (i.e. /<appname>) */
		if (baseUrl.startsWith("/")) {
			baseUrl = ctx.getRequest().getContextPath()+baseUrl;
		}
		StringBuilder urlBuilder = new StringBuilder(baseUrl);
		try {
			if (parameterMap != null && !parameterMap.isEmpty()) {
				boolean first = true;
				for (Iterator it = parameterMap.entrySet().iterator(); it.hasNext(); ) {
					urlBuilder.append(first ? "?" : "&");
					first = false;
					Map.Entry entry = (Map.Entry)it.next();
					urlBuilder.append(URLEncoder.encode(entry.getKey().toString(), "UTF-8"));
					urlBuilder.append("=");
					if (entry.getValue() != null) {
						urlBuilder.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			assert false : "No UTF-8 encoding supported?! Really?!";
		}
		if (!StringUtils.isEmpty(anchor)) {
			urlBuilder.append("#").append(anchor);
		}
		return urlBuilder.toString();
	}
	
	public void go() throws IOException {
		Context ctx = Context.getInstance();
		ctx.getResponse().sendRedirect(getCompleteUrl());
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}
	
	public void setBaseUrl(String url) {
		this.baseUrl = url;
	}
}
