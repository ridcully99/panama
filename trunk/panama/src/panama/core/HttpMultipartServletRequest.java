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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.lang.StringUtils;
import org.grlea.log.SimpleLogger;

/**
 * A MultiPartServletRequest using commons-fileupload API.
 * @author Ridcully
 *
 */
public class HttpMultipartServletRequest implements HttpServletRequest {

	protected HttpServletRequest request;
	protected Map parameters = new HashMap();
	protected Map fileItems = new HashMap();
	protected static SimpleLogger log = new SimpleLogger(HttpMultipartServletRequest.class);

	private ServletFileUpload fileUpload = new ServletFileUpload();

	/**
	 * Extracts files from MultipartRequests.
	 * Files larger than sizeThreshold are stored in temporary files in System.getProperty("java.io.tmpdir")
	 * @param request
	 * @param sizeMax maximum allowed file size in bytes, -1 for no maximum.
	 * @param sizeThreshold maximum file size in bytes for keeping files in memory (-1 for default of 10KB)
	 */
	public HttpMultipartServletRequest(HttpServletRequest request, int sizeMax, int sizeThreshold) {
		this.request = request;
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(sizeThreshold == -1 ? 1024 * 10 : sizeThreshold);
		fileUpload.setFileItemFactory(factory);
		fileUpload.setSizeMax(sizeMax);
		try {
			List items = fileUpload.parseRequest(request);
			// Process the uploaded items
			if (items != null) {
				for (Iterator it = items.iterator(); it.hasNext(); ) {
					FileItem item = (FileItem)it.next();
					String fieldName = item.getFieldName();
					if (item.isFormField()) {
						// process form field
						List values = (List)parameters.get(fieldName);
						if (values == null) {
							values = new ArrayList();
							parameters.put(fieldName, values);
						}
						try {
							values.add(item.getString(request.getCharacterEncoding()));
						} catch (UnsupportedEncodingException e) {
							values.add(item.getString());
						}
					} else {
						// process uploaded file
						// As we also get items when no file was selected in file-input field, we check if the file has a name.
						// (size is 0 but this could also be an empty file, thus we check the name)
						if (!StringUtils.isEmpty(item.getName())) {
							List values = (List)fileItems.get(fieldName);
							if (values == null) {
								values = new ArrayList();
								fileItems.put(fieldName, values);
							}
							values.add(item);
						}
					}
				}
			}
			listsToArrays(parameters);
			listsToFileItemArrays(fileItems);
		} catch (FileUploadException e) {
			log.error(e.getMessage());
			log.errorException(e);
		}
	}

	/**
	 * Utility method that determines whether the request contains multipart content.
	 * @param request
	 * @return whether the request contains multipart content
	 */
	public static boolean isMultipartContent(HttpServletRequest request) {
		return ServletFileUpload.isMultipartContent(new ServletRequestContext(request));
	}

	/**
	 * Returns the value of a request parameter as a String, or null if the
	 * parameter does not exist. Request parameters are extra information sent
	 *  with the request. For HTTP servlets, parameters are contained in the
	 * query string or posted form data.
	 * <p>
	 * You should only use this method when you are sure the parameter has only
	 * one value. If the parameter might have more than one value, use
	 * <code>getParameterValues(java.lang.String).</code>
	 * <p>
	 * If you use this method with a multivalued parameter, the value returned
	 * is equal to the first value in the array returned by
	 * <code>getParameterValues</code>.
	 * <p>
	 * If the parameter data was sent in the request body, such as occurs with
	 * an HTTP POST request, then reading the body directly via
	 * <code>getInputStream()</code> or <code>getReader()</code> can interfere
	 * with the execution of this method.
	 * @param name - a String specifying the name of the parameter
	 * @return a String representing the single value of the parameter
	 */
	public String getParameter(String name) {
		String[] values = (String[])parameters.get(name);
		return values != null ? (String)values[0] : null;
	}

	/**
	 * Returns an Enumeration of String  objects containing the names of the
	 * parameters contained in this request. If the request has no parameters,
	 * the method returns an empty Enumeration.
	 * @return an Enumeration of String objects, each String containing the
	 * name of a request parameter; or an empty Enumeration if the request
	 * has no parameters
	 */
	public Enumeration getParameterNames() {
		return Collections.enumeration(parameters.keySet());
	}

	/**
	 * Returns an array of <code>String</code> objects containing all of the
	 * values the given request parameter has, or <code>null</code> if the
	 * parameter does not exist.
	 * <p>
	 * If the parameter has a single value, the array has a length of 1.
	 * @param name - a String containing the name of the parameter whose value
	 * is requested
	 * @return an array of <code>String</code> objects containing the
	 * parameter's values
	 */
	public String[] getParameterValues(String name) {
		return (String[])parameters.get(name);
	}

	/**
	 * Returns a <code>java.util.Map</code> of the parameters of this request.
	 * Request parameters are extra information sent with the request. For HTTP
	 * servlets, parameters are contained in the query string or posted form data.
	 * @return an immutable <code>java.util.Map</code> containing parameter
	 * names as keys and parameter values as map values. The keys in the
	 * parameter map are of type String. The values in the parameter map are
	 * of type Object array.
	 */
	public Map getParameterMap() {
		return Collections.unmodifiableMap(parameters);
	}

	/**
	 * Like getParameter but for <code>FileItem</code>s.
	 * @return a FileItem
	 */
	public FileItem getFileItem(String name) {
		FileItem[] values = (FileItem[])fileItems.get(name);
		return values != null ? values[0] : null;
	}

	/**
	 * Like getParameterNames but for <code>FileItem</code>s.
	 */
	public Enumeration getFileItemNames() {
		return Collections.enumeration(fileItems.keySet());
	}

	/**
	 * Like getParameterValues but for <code>FileItem</code>s.
	 */
	public FileItem[] getFileItemValues(String name) {
		return (FileItem[])fileItems.get(name);
	}

	/**
	 * Like getParameterMap but for <code>FileItem</code>s.
	 */
	public Map getFileItemMap() {
		return Collections.unmodifiableMap(fileItems);
	}

	/**
	 * Perhaps could return request.getReader()?
	 */
	public BufferedReader getReader() throws IOException {
		throw new IllegalStateException("Cannot get Reader for multipart/form-data type request");
	}

	// ========================================================================
	// Direct Proxy of HttpServletRequest Methods
	// ========================================================================

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getAuthType() {
		return request.getAuthType();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public Cookie[] getCookies() {
		return request.getCookies();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public long getDateHeader(String name) {
		return request.getDateHeader(name);
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getHeader(String name) {
		return request.getHeader(name);
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public Enumeration getHeaders(String name) {
		return request.getHeaders(name);
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public Enumeration getHeaderNames() {
		return request.getHeaderNames();
	}


	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public int getIntHeader(String name) {
		return request.getIntHeader(name);
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getMethod() {
		return request.getMethod();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getPathInfo() {
		return request.getPathInfo();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getPathTranslated() {
		return request.getPathTranslated();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getContextPath() {
		return request.getContextPath();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getQueryString() {
		return request.getQueryString();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getRemoteUser() {
		return request.getRemoteUser();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public boolean isUserInRole(String role) {
		return request.isUserInRole(role);
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public Principal getUserPrincipal() {
		return request.getUserPrincipal();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getRequestedSessionId() {
		return request.getRequestedSessionId();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getRequestURI() {
		return request.getRequestURI();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getServletPath() {
		return request.getServletPath();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public HttpSession getSession(boolean create) {
		return request.getSession(create);
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public HttpSession getSession() {
		return request.getSession();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public boolean isRequestedSessionIdValid() {
		return request.isRequestedSessionIdValid();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public boolean isRequestedSessionIdFromCookie() {
		return request.isRequestedSessionIdFromCookie();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public boolean isRequestedSessionIdFromURL() {
		return request.isRequestedSessionIdFromURL();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 * @deprecated
	 */
	public boolean isRequestedSessionIdFromUrl() {
		return request.isRequestedSessionIdFromUrl();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public Object getAttribute(String name) {
		return request.getAttribute(name);
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public Enumeration getAttributeNames() {
		return request.getAttributeNames();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getCharacterEncoding() {
		return request.getCharacterEncoding();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public int getContentLength() {
		return request.getContentLength();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getContentType() {
		return request.getContentType();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getProtocol() {
		return request.getProtocol();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getScheme() {
		return request.getScheme();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getServerName() {
		return request.getServerName();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public int getServerPort() {
		return request.getServerPort();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public ServletInputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getRemoteAddr() {
		return request.getRemoteAddr();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public String getRemoteHost() {
		return request.getRemoteHost();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public void setAttribute(String name, Object o) {
		request.setAttribute(name, o);
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public void removeAttribute(String name) {
		request.removeAttribute(name);
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public Locale getLocale() {
		return request.getLocale();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public Enumeration getLocales() {
		return request.getLocales();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public boolean isSecure() {
		return request.isSecure();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public RequestDispatcher getRequestDispatcher(String path) {
		return request.getRequestDispatcher(path);
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 * @deprecated
	 */
	public String getRealPath(String path) {
		return request.getRealPath(path);
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public StringBuffer getRequestURL() {
		return request.getRequestURL();
	}

	/**
	 * Calls same method on the underlying HttpServletRequest.
	 * Refer to Servlet API for information about this method.
	 */
	public void setCharacterEncoding(String encoding)
			throws java.io.UnsupportedEncodingException {
		request.setCharacterEncoding(encoding);
	}

	/**
	 * Helper function to convert the values of the map which are all lists to arrays.
	 * @param map
	 * @return a Map<String, String[]>
	 */
	private Map listsToArrays(Map map) {
		for (Iterator it = map.keySet().iterator(); it.hasNext(); ) {
			String key = (String)it.next();
			map.put(key, listToArray((List)map.get(key)));
		}
		return map;
	}

	/**
	 * Helper function to convert a list to a string array.
	 * @param list a list of strings
	 * @return an array containing the elements of the specified list.
	 */
	private String[] listToArray(List list) {
		String[] result = new String[list.size()];
		list.toArray(result);
		return result;
	}

	/**
	 * Helper function to convert the values of the map which are all lists to arrays.
	 * @param map
	 * @return a Map<String, FileItem[]>
	 */
	private Map listsToFileItemArrays(Map map) {
		for (Iterator it = map.keySet().iterator(); it.hasNext(); ) {
			String key = (String)it.next();
			map.put(key, listToFileItemArray((List)map.get(key)));
		}
		return map;
	}

	/**
	 * Helper function to convert a list to a FileItem array.
	 * @param list a list of <code>FileItem</code>s
	 * @return an array containing the elements of the specified list.
	 */
	private FileItem[] listToFileItemArray(List list) {
		FileItem[] result = new FileItem[list.size()];
		list.toArray(result);
		return result;
	}

	public String getLocalAddr() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getLocalName() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getLocalPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getRemotePort() {
		// TODO Auto-generated method stub
		return 0;
	}
}
