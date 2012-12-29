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

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.velocity.tools.generic.ResourceTool;

/**
 * Wrapper for easy fix of character encoding and for extended Locale support.
 * By putting the extended Locale support here, we can use standard tools like the {@link ResourceTool}.
 * @author ridcully
 *
 */
public class RequestWrapper extends HttpServletRequestWrapper implements HttpServletRequest {

	public RequestWrapper(HttpServletRequest request) {
		super(request);
	}

	@Override
	public String getCharacterEncoding() {
		String enc = super.getCharacterEncoding();
		return enc != null ? enc : "UTF-8";
	}
	
	/**
	 * Gets preferred locale to use. If context object available, returns the locale from there,
	 * otherwise super.getLocale()
	 */
	@Override
	public Locale getLocale() {
		Object o = getAttribute(Dispatcher.CONTEXT_KEY);
		if (o != null && o instanceof Context) {
			return ((Context)o).getLocale();
		} else {
			return super.getLocale();
		}
	}
}
