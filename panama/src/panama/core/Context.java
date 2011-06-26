/*
 *  Copyright 2004-2010 Robert Brandner (robert.brandner@gmail.com) 
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

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.grlea.log.SimpleLogger;

/**
 * Provides methods to access application scope, session scope, request scope and parameters.
 * Wrapper for Velocity.Context and more.
 * 
 * Do not use all uppercase keys, they should be reserved for predefined content
 * 
 * @author Robert
 */
public class Context {
	
	private final static String LOCALE_KEY = Dispatcher.PREFIX + "_locale";	// access via getLocale / setLocale
	private final static String TOKEN_VALUE_KEY = Dispatcher.PREFIX + "_token_values";
	
	/*
	 * public wrappers for easier access and more readable code
	 */
	public SessionWrapper session = new SessionWrapper();
	public ApplicationWrapper application = new ApplicationWrapper();
	public TokenWrapper tokens = new TokenWrapper();
	
	/*
	 * getters for wrappers to have the same effect in velocity templates
	 */
	public SessionWrapper getSession() {
		return session;
	}

	public ApplicationWrapper getApplication() {
		return application;
	}
	
	public TokenWrapper getTokens() {
		return tokens;
	}

	/**
	 * Logging
	 */
	protected static SimpleLogger log = new SimpleLogger(Dispatcher.class);

	/**
	 * Current context for current thread.
	 */
	private static ThreadLocal<Context> contextHolder = new ThreadLocal<Context>();
	private Dispatcher core;
	private HttpSession httpSession;
	
	private HttpServletRequest request;
	private HttpMultipartServletRequest multipartRequest;
	private HttpServletResponse response;
	
	private Map parameterMap;
	private Locale defaultLocale;	// locale as derived from supported languages and accepted languages from browser - this is the default as long as no setLocale() has set an explicit locale in the session.

	/**
	 * Static creator - created Context is stored in threadlocal variable and
	 * may be accessed via getInstance()
	 * @param s Servlet
	 * @param sess Session
	 * @param req Request
	 * @param res Response
	 * @param defaultLocale Locale derived vom supported and accepted locales (app res. browser)
	 * @return newly created instance.
	 * @throws Exception if creating the hibernateSupportClass
	 */
	public static Context createInstance(Dispatcher core, HttpSession sess, HttpServletRequest req, HttpServletResponse res, Locale defaultLocale) throws Exception {
		Context ctx = new Context(core, sess, req, res, defaultLocale);
		contextHolder.set(ctx);
		log.debug("createInstance in ThreadLocal "+contextHolder);
		return ctx;
	}
	
	public static Context getInstance() {
		log.debug("getInstance from "+contextHolder);
		return contextHolder == null ? null : (Context)contextHolder.get();
	}	
	
	public static void destroyInstance() {
		contextHolder.remove();
	}
	
	public Context() {		
	}
	
	private Context(Dispatcher core, HttpSession sess, HttpServletRequest req, HttpServletResponse res, Locale defaultLocale) {
		setCore(core);
		setHttpSession(sess);
		setResponse(res);
		if (req != null) {
			setRequest(req);
		}
		this.defaultLocale = defaultLocale;
	}
	
	public Dispatcher getCore() {
		return core;
	}
	public void setCore(Dispatcher core) {
		this.core = core;
	}
	public ServletContext getApplicationContext() {
		return core != null ? core.getApplicationContext() : null;
	}
	public HttpServletRequest getRequest() {
		return request;
	}
	public HttpMultipartServletRequest getMultipartRequest() {
		return multipartRequest;
	}
	public void setRequest(HttpServletRequest request) {
		this.request = request;
		if (HttpMultipartServletRequest.isMultipartContent(request)) {
			this.multipartRequest = (HttpMultipartServletRequest)request;
		}
	}
	public HttpServletResponse getResponse() {
		return response;
	}
	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}
	public HttpSession getHttpSession() {
		return httpSession;
	}
	public void setHttpSession(HttpSession session) {
		this.httpSession = session;
	}

	// -------------------------------------------------------------------------------------
	// L10n methods
	// -------------------------------------------------------------------------------------

	/**
	 * Gets Locale to use. If one is explicity set via {@link #setLocale(Locale)} it is returned,
	 * otherwise a default Locale derived from the supported-languages setting and the accepted languages (and it's order) from the user's browser is returned.
	 * @return a Locale that matches best what the user wants and the application can provide.
	 */
	public Locale getLocale() {
		Locale locale = (Locale)session.get(LOCALE_KEY);
		if (locale == null) {
			locale = defaultLocale;
		}
		return locale;
	}

	/**
	 * Explizitly set a Locale to use.
	 * @param locale The locale to use.
	 */
	public void setLocale(Locale locale) {
		session.put(LOCALE_KEY, locale);
	}
	
	/**
	 * Gets localized string for specified key and optional args, 
	 * based on default resource bundle (resources.properties and it's variations for 
	 * other languages like resources_de.properties) and current locale.
	 * 
	 * e.g. hello = Hello {1}!
	 * getResource("hello", "World") -> Hello World!
	 * 
	 * @param key
	 * @param args values for placesholders in resource value.
	 * @return the formatted string
	 */
	public String getLocalizedString(String key, Object... args) {
		return getLocalizedString("resources", key, args);
	}
		
	/**
	 * Gets localized string for specified key and optional args, based on specified resource bundle and current locale.
	 * @param bundleName name of resource bundle to use
	 * @param key
	 * @param args values for placesholders in resource value.
	 * @return the formatted string
	 */
	public String getLocalizedString(String bundleName, String key, Object... args) {
		try {
			ResourceBundle bundle = ResourceBundle.getBundle(bundleName, getLocale());
			if (bundle.containsKey(key)) {
				return MessageFormat.format(bundle.getString(key), args);
			} else {
				return "???"+key+"???";
			}
		} catch (Exception e) {
			log.errorException(e);
			return "??!"+key+"??!";
		}
	}	
	
	// -------------------------------------------------------------------------------------
	// Parameter methods
	// -------------------------------------------------------------------------------------
	
	public void setParameterMap(Map parameterMap) {
		this.parameterMap = new HashMap();
		this.parameterMap.putAll(parameterMap);
	}
	
	public Map getParameterMap() {
		if (parameterMap == null) {
			// get parameterMap from request lazily, so client apps can also read the raw request data if needed.
			setParameterMap(request.getParameterMap());
		}
		return parameterMap;
	}
	
	public String getParameter(String key) {
		String[] values = getParameterValues(key);
		return values != null ? values[0] : null;
	}
		
	public void setParameter(String key, String value) {
		getParameterMap().put(key, new Object[]{value});
	}
	
	public void setParameter(String key, Object[] values) {
		getParameterMap().put(key, values);
	}
	
	public String[] getParameterValues(String key) {
		// some effort to return a String array always
		// values from request-parameters are always string arrays but
		// userdefined parameters might be everything
		Object o = getParameterMap().get(key);
		if (o == null) {
			return null;
		}
		else if (o instanceof String[]) {
			return (String[])o;
		} else if (o instanceof Array || o instanceof Collection || o instanceof Map) {
			Collection col;
			if (o instanceof Array) {
				col = Arrays.asList((Object[])o);
			} else if (o instanceof Collection) {
				col = (Collection)o;
			} else {
				col = ((Map)o).values();
			}
			String[] res = new String[col.size()];
			int i=0;
			for (Iterator it = col.iterator(); it.hasNext(); i++) {
				res[i] = it.next().toString();
			}
			return res;
		} else if (o instanceof Object[]) {
			Object[] arr = (Object[])o; 
			String[] res = new String[arr.length];
			for (int i=0; i<arr.length; i++) {
				res[i] = arr[i] != null ? arr[i].toString() : null;
			}
			return res;
		} else {
			String[] res = new String[1];
			res[0] = o.toString();
			return res;
		}			
	}
	
	public Integer getIntParameter(String key) {
		try {
			return new Integer(getParameter(key));
		} catch (Exception e) {
			return null;
		}
	}

	public Long getLongParameter(String key) {
		try {
			return new Long(getParameter(key));
		} catch (Exception e) {
			return null;
		}
	}
	
	// -------------------------------------------------------------------------------------
	// FileItem methods (return null for non-MultipartServletRequests)
	// Normally you'd not use these methods directly, but use the FileItemField class
	// -------------------------------------------------------------------------------------
	
	public boolean hasFileItems() {
		return getFileItemMap() != null;
	}
	
	public FileItem getFileItem(String name) {
		if (multipartRequest != null) {
			return multipartRequest.getFileItem(name);
		} else {
			return null;
		}
	}
	
	public FileItem[] getFileItemValues(String name) {
		if (multipartRequest != null) {
			return multipartRequest.getFileItemValues(name);
		} else {
			return null;
		}
	}

	public Map getFileItemMap() {
		if (multipartRequest != null) {
			return multipartRequest.getFileItemMap();
		} else {
			return null;
		}
	}

	// -------------------------------------------------------------------------------------
	// Some shortcuts for often used request methods
	// -------------------------------------------------------------------------------------
	
	public void put(String key, Object value) {
		request.setAttribute(key, value);
	}
	
	public Object get(String key) {
		return request.getAttribute(key);
	}	
	
	// -------------------------------------------------------------------------------------
	// internal wrapper classes for shorter access, making code more readable
	// -------------------------------------------------------------------------------------

	public class ApplicationWrapper {	
		
		public Object get(String key) {
			return getApplicationContext().getAttribute(key);
		}
		
		public void put(String key, Object value) {
			getApplicationContext().setAttribute(key, value);
		}
	}
	
	public class SessionWrapper {
		
		public Object get(String key) {
			return httpSession.getAttribute(key);
		}
		
		public void put(String key, Object value) {
			httpSession.setAttribute(key, value);
		}

	}

	public class TokenWrapper {
		
		/**
		 * Creates a token. To be used from within some action code.
		 * @param name name of token
		 */
		public void create(String name) {
			String value = new Date().getTime()+"."+Math.random();
			getTokenValues().put(name, value);
		}
		
		/**
		 * Gets current value of token. To be used from within template.
		 * @param name
		 * @return value of token 'name' or null.
		 */
		public String get(String name) {
			return getTokenValues().get(name);
		}
		
		/**
		 * Checks whether token is valid. To be used from within some action code.
		 * @param name	name of token
		 * @param value supposed value of token (normally from request's parametermap)
		 * @return whether token is valid, i.e. stored value matches provided value.
		 */
		public boolean verify(String name, String value) {
			String intern = get(name);
			return intern != null && intern.equals(value);
		}

		/**
		 * Invalidates (removes) a token. To be used from within some action code.
		 * @param name name of token
		 */
		public void invalidate(String name) {
			getTokenValues().remove(name);
		}
		
		@SuppressWarnings("unchecked")
		private Map<String, String> getTokenValues() {
			Map<String, String> values = (Map<String, String>)session.get(TOKEN_VALUE_KEY);
			if (values == null) {
				values = new HashMap<String, String>();
				session.put(TOKEN_VALUE_KEY, values);
			}
			return values;
		}
	}
}
