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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.view.ViewToolManager;
import org.grlea.log.SimpleLogger;
import org.scannotation.AnnotationDB;
import org.scannotation.WarUrlFinder;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.exceptions.AuthorizationException;
import panama.exceptions.ForceTargetException;
import panama.exceptions.NoSuchActionException;
import panama.util.TestTimer;


/**
 * The main dispatcher, implemented as a filter.
 * 
 * @author Robert
 */
public class Dispatcher implements Filter {

	private static final long serialVersionUID = 1L;
	
	public final static String PREFIX = "panama";

	/* key for app name (i.e. filtername) in application context */
	public final static String APP_NAME_KEY = PREFIX+"_application_name";
	
	public final static String PARAM_LANGUAGES = PREFIX+".languages";
	public final static String PARAM_MAXFILEUPLOADSIZE = PREFIX+".maxfileuploadsize";

	/* keys for objects put in request context */
	public final static String CONTEXT_KEY = "context";
	public final static String ACTION_INVOCATION_MODE_KEY = PREFIX+"action_invocation_mode";
	
	/* values for ACTION_INVOCATION_MODE_KEY */
	public final static String ACTION_INVOCATION_PROGRAMATICALLY = "programatically";
	public final static String ACTION_INVOCATION_BY_URL = "by_url";
	
	private final static int CAN_HANDLE_REQUEST_NO = 0;
	private final static int CAN_HANDLE_REQUEST_YES = 1;
	private final static int CAN_HANDLE_REQUEST_REDIRECT = 2;
	
	/** Simply! Logging */
	protected static SimpleLogger log = new SimpleLogger(Dispatcher.class);

	/** Mapping controller alias or FQCN to Class itself */
	private Map<String, Class<? extends BaseController>> controllerClasses = new HashMap<String, Class<? extends BaseController>>();
	
	/** Mapping Controller-FQCN#actionName to Method */
	private Map<String, Method> actionMethods = new HashMap<String, Method>();
	
	/** Configuration (from FilterConfig.InitParameters) */
	private Map<String, String> initParams = new HashMap<String, String>();
	
	/** Application context (set in init() method) */
	private ServletContext applicationContext = null;
	
	/** Multilanguage support */
	protected List<String> supportedLanguages = null;
	
	/** Velocity */
	private VelocityEngine velocityEngine;
	private ViewToolManager velocityToolManager;
	
	/** Startup time */
	private Date startupAt = new Date();

	/**
	 * Gets init-params from web.xml
	 * This is called synchronized by the servlet container.
	 */
	public void init(FilterConfig filterConfig) {
		System.out.println(Version.LOGO_ASCIIART);		
		applicationContext = filterConfig.getServletContext();
		applicationContext.setAttribute(APP_NAME_KEY, filterConfig.getFilterName());
		
		@SuppressWarnings("rawtypes")
		Enumeration names = filterConfig.getInitParameterNames();
		while(names.hasMoreElements()) {
			String key = (String)names.nextElement();
			initParams.put(key, filterConfig.getInitParameter(key));
		}
		
		/* init velocity */
		try {
			ClassLoader cl = this.getClass().getClassLoader();
			Properties velocityProperties = new Properties();
			velocityProperties.load(cl.getResourceAsStream("velocity.properties"));
			velocityEngine = new VelocityEngine(velocityProperties);
			/* init velocity tool manager -- automatically finds all default-tools, the framework's tools as defined in tools.xml and all tools of the web-app specified in tools.xml at classpath-root. */
			velocityToolManager = new ViewToolManager(applicationContext, false, false);
			velocityToolManager.setVelocityEngine(velocityEngine);
			velocityToolManager.autoConfigure(true);
		} catch (Exception e) {
			log.fatal("velocity init failed!");
			log.fatalException(e);
		}
		
		String languages = getInitParam(PARAM_LANGUAGES);
		String[] supported;
		if (languages == null) {
			supported = new String[]{"en"};
		} else {
			supported = languages.split("\\s*,\\s*");
			if (supported == null || supported.length == 0) {
				supported = new String[]{"en"};	
			}
		}
		supportedLanguages = Arrays.asList(supported);
		
		try {
			collectControllers();
		} catch (Exception e) {
			log.error("Collecting Controllers and Actions failed.");
			log.errorException(e);
		}
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
		switch (canHandleRequest(req, res)) {
			case CAN_HANDLE_REQUEST_YES:
				{
				HttpServletRequest httpReq = new RequestWrapper((HttpServletRequest)req);
				HttpServletResponse httpRes = (HttpServletResponse)res;
				handleRequest(httpReq, httpRes);
				return;
				}
			case CAN_HANDLE_REQUEST_REDIRECT: 
				{
				HttpServletRequest httpReq = (HttpServletRequest)req;
				HttpServletResponse httpRes = (HttpServletResponse)res;
				String url = httpReq.getContextPath()+httpReq.getServletPath();
				url = url+"/";
				if (!StringUtils.isEmpty(httpReq.getQueryString())) {
					url = url+"?"+httpReq.getQueryString();
				}
				httpRes.sendRedirect(httpRes.encodeRedirectURL(url));
				return;
				}
			default:
				filterChain.doFilter(req, res);	// pass along
		}
	}	

	/**
	 * Checks if there is a controller and action to handle the path.
	 * This should be very fast, as it may have to check lots of URLs.
	 * Criteria:
	 * - must be HttpServletRequest and HttpServletResponse
	 * - Request-URL must specify existing Controller
	 * - Request-URL must specify an action or specified controller must have a default action
	 * 
	 * @param req
	 * @param res
	 * @return wether we can handle this request, a redirect is required or it should be passed along the filter chain.
	 */
	private int canHandleRequest(ServletRequest req, ServletResponse res) {
		if (!(req instanceof HttpServletRequest && res instanceof HttpServletResponse)) {
			return CAN_HANDLE_REQUEST_NO;
		}
		String path = ((HttpServletRequest)req).getServletPath();	
		String[] ca = extractControllerAndActionNames(path);
		String ctrlName = ca[0];
		if (!controllerClasses.containsKey(ctrlName)) {
			return CAN_HANDLE_REQUEST_NO;
		}
		String actionName = ca[1];
		if (StringUtils.isEmpty(actionName)) {
			if (StringUtils.isEmpty(getDefaultActionName(controllerClasses.get(ctrlName)))) {
				return CAN_HANDLE_REQUEST_NO;
			} else if (path.trim().endsWith("/")) {
				return CAN_HANDLE_REQUEST_YES;
			} else {
				return CAN_HANDLE_REQUEST_REDIRECT;
			}
		} else {
			StringBuffer actionKey = new StringBuffer(controllerClasses.get(ctrlName).getName()).append("#").append(actionName);
			Method method = actionMethods.get(actionKey.toString());
			return (method != null) ? CAN_HANDLE_REQUEST_YES : CAN_HANDLE_REQUEST_NO;
		}
	}	
	
	/**
	 * This is the main dispatching method, handling all requests.
	 * It parses request, extracts controller and action information,
	 * invokes action and forwards to rendering the template.
	 */	
	public void handleRequest(HttpServletRequest req, HttpServletResponse res) {

		Context ctx = null;
		try {
			log.debug("about to handle request "+req.getServletPath());
			/* Convert to multipart request if it has multipart content (fileuploads). */
			if (HttpMultipartServletRequest.isMultipartContent(req)) {
				/* maxFileSize is defined in mega-bytes */
				int maxFileSize = 1024 * 1024 * (new Integer(getInitParam(PARAM_MAXFILEUPLOADSIZE, "1")).intValue());
				HttpMultipartServletRequest mreq = new HttpMultipartServletRequest(req, maxFileSize, -1);
				req = mreq;
			}
			req.setCharacterEncoding("UTF-8");															// must set this before getParameter() to get the correct encoding
			/* create a context for the controller */
			HttpSession session = req.getSession(true);													// get session, create one if none exists
			Locale defaultLocale = computeDefaultLocale(supportedLanguages, req.getLocales());
			ctx = Context.createInstance(this, session, req, res, defaultLocale); 						// create context instance
			ctx.put(CONTEXT_KEY, ctx);																	// put context into itself - some tools may need a context passed to them
			Target target = handleAction(ctx, req.getServletPath(), ACTION_INVOCATION_BY_URL);
			if (target != null) {
				log.debug("about to go to target "+target);
				TestTimer targetTimer = new TestTimer("target");
				target.go();
				targetTimer.done();
				log.debug("returned from target");
			}
		} catch (Throwable e) {	// catch all sorts of exceptions
			log.fatalException(e);
			try {
				res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				log.fatalException(e);
			}
		} finally {	
		}
	}
	
	/**
	 * Scan WEB-INF/lib and WEB-INF/classes for Controller classes and puts all in controllerClasses map.
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void collectControllers() throws IOException {
		AnnotationDB adb = new AnnotationDB();
		adb.setScanClassAnnotations(true);
		adb.setScanFieldAnnotations(false);
		adb.setScanMethodAnnotations(false);
		adb.setScanParameterAnnotations(false);
		adb.scanArchives(WarUrlFinder.findWebInfClassesPath(applicationContext));
		adb.scanArchives(WarUrlFinder.findWebInfLibClasspaths(applicationContext));

		Set<String> ctrls = adb.getAnnotationIndex().get(panama.annotations.Controller.class.getName());
		
		for (String n : ctrls) {
			Class<? extends BaseController> c = null;
			try {
				c = (Class<? extends BaseController>) Class.forName(n);
			} catch (Exception e) {
				log.warn("Class "+n+" not found or not derived from Controller class. Ignoring it.");
				continue;
			}
			controllerClasses.put(n, c);
			log.debug(c+" -> "+c);
			collectActions(c);
			String alias = c.getAnnotation(panama.annotations.Controller.class).alias();
			if 	(StringUtils.isNotEmpty(alias)) {
				alias = alias.trim();
				if (alias.contains("/")) {
					log.error("Controller alias '"+alias+"' contains illegal characters. Ignoring alias.");
					continue;
				}
				if (controllerClasses.containsKey(alias)) {
					log.error("Duplicate Controller alias '"+alias+"' detected. Used by "+n+
							" and "+controllerClasses.get(alias).getName()+". Using alias only for the latter.");
					continue;
				}
				controllerClasses.put(alias, c);
				log.debug(alias+" -> "+n);
			}
		}
	}	
	
	/**
	 * Collect all actions of specified controller and put them in actionMethods map.
	 * Uses classname and method-name for key. If alias is provided a second entry using alias in the key is made.
	 * If controller has a default action specified use empty string for method part of key.
	 *  
	 * @param controllerName
	 * @param clazz
	 */
	private void collectActions(Class<? extends BaseController> clazz) {
		String defaultActionName = getDefaultActionName(clazz);
		String defaultActionKey = clazz.getName()+"#";
		
		for (Method m : clazz.getMethods()) {
			if (m.isAnnotationPresent(Action.class) && m.getParameterTypes().length == 0) {
				Class<?> returnType = m.getReturnType();
				while (returnType != null) {
					if (returnType.equals(Target.class)) {
						String key = clazz.getName()+"#"+m.getName();
						actionMethods.put(key, m);
						log.debug(key+" -> "+m.getName());
						if (m.getName().equals(defaultActionName)) {
							actionMethods.put(defaultActionKey, m);
							log.debug(defaultActionKey+" -> "+m.getName()+" (default action)");
						}
						String alias = m.getAnnotation(Action.class).alias();
						if (!StringUtils.isEmpty(alias)) {
							key = clazz.getName()+"#"+alias;
							actionMethods.put(key, m);	
							log.debug(key+" -> "+m.getName());
							if (alias.equals(defaultActionName)) {
								actionMethods.put(defaultActionKey, m);
								log.debug(defaultActionKey+" -> "+alias+" (default action)");
							}
						}
						break;
					}
					returnType = returnType.getSuperclass();
				}
			}
		}
	}

	private String getDefaultActionName(Class<? extends BaseController> clazz) {
		Controller ctrlAnnotation = clazz.getAnnotation(Controller.class);
		return ctrlAnnotation != null ? ctrlAnnotation.defaultAction() : null;
	}

	/**
	 * Creates a new instance for the controller specified by nameOrAlias
	 * 
	 * @param nameOrAlias FQCN or alias of a controller class.
	 * @return Controller object or null
	 */
	public BaseController getController(String nameOrAlias) {
		BaseController ctrl = null;
		Class<? extends BaseController> clazz = controllerClasses.get(nameOrAlias);
		if (clazz == null) {
			log.error("No controller class found for name or alias '"+nameOrAlias+"'");
			return null;
		} else {
			try {
				ctrl = clazz.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				log.error("Error creating controller instance");
				log.errorException(e);
			}
		}
		return ctrl;
	}	
	
	/**
	 * Executes action derived from specified path.
	 * @see #handleAction(Context, String)
	 * @param ctx A context
	 * @param path A path to an action
	 * @param invocationMode One of ACTION_INVOKATION_XXX - During execution, the specified value can be accessed
	 * 			in request context using the ACTION_INVOKATION_MODE_KEY. This may come handy for those who
	 * 			need to know if an action was invoked by an url or programatically
	 * @return The target returned by the action or some error target in case of an error.
	 * @throws IOException 
	 */
	protected Target handleAction(Context ctx, String path, String invocationMode) throws IOException {
	
		/* put invokation mode in request context */
		ctx.put(ACTION_INVOCATION_MODE_KEY, invocationMode);
		
		String[] ca = extractControllerAndActionNames(path);
		String ctrlName = ca[0];
		String actionName = ca[1];
		
		/* create new instance of controller for specified ctrlName */
		BaseController ctrl = getController(ctrlName);
		assert ctrl != null : "No controller found, means canHandleRequest() is wrong!";
			
		/* put controller object into context - so it's accessible from template! 
		 * always use Alias (if available) AND FQCN
		 * replace . with _ for sake of velocity
		 */
		String alias = ctrl.getClass().getAnnotation(panama.annotations.Controller.class).alias();
		if (!StringUtils.isEmpty(alias)) {
			ctx.put(alias.replace(".", "_"), ctrl);					/* Alias */
		}
		ctx.put(ctrl.getClass().getName().replace(".", "_"), ctrl);	/* FQCN */
		
		TestTimer timer = new TestTimer("execute");
		try {
			return executeAction(ctx, ctrl, actionName);
		} catch (NoSuchActionException nme) {
			log.warn("no such action: \""+ctrl.getClass().getName()+"/"+actionName+"\"");			
			ctx.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
			return null;
		} catch (AuthorizationException ae) {
			log.warn("no authorization for action: \""+ctrl.getClass().getName()+"/"+actionName+"\"");			
			ctx.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
			return null;
		} catch (Exception e1) {
			String msg = "error doing action \""+actionName+"\"";
			log.fatal(msg);
			log.fatalException(e1);
			ctx.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}	
		finally {
			timer.done();
		}
	}

	/** 
	 * Extract controller and action from servlet-path
	 * @return String Array containing ctrlName and actionName (both may be null)
	 */
	private String[] extractControllerAndActionNames(String path) {
		path = path.replaceFirst("^/+", "");	// remove leading slashs
		String ctrlName = null;
		String actionName = null;
		if (path.length() > 0) {
			int divider = path.lastIndexOf("/");
			if (divider == -1) {
				ctrlName = path;
			} else {
				ctrlName = path.substring(0, divider).trim();
				if (divider+1 < path.length()) {
					actionName = path.substring(divider+1).trim();
				}
			}
		}
		return new String[] {ctrlName, actionName};
	}

	/**
	 * Executes action derived from specified path.
	 * The path is supposed to look like that:
	 * [/.../]<controller name or alias>[/<action name or alias>]
	 * If no action part can be found, the default action of the controller will be tried.
	 * @param ctx A context
	 * @param path A path to an action. See description for details.
	 * @return The target returned by the action or some error target in case of an error.
	 * @throws IOException 
	 */	
	public Target handleAction(Context ctx, String path) throws IOException {
		return handleAction(ctx, path, ACTION_INVOCATION_PROGRAMATICALLY);
	}

	public Target executeAction(Context ctx, BaseController ctrl, String actionName) throws NoSuchActionException {
		if (StringUtils.isEmpty(actionName)) {
			actionName = getDefaultActionName(ctrl.getClass());
		}
		try {
			/* allow pre-processing */						
			ctrl.beforeAction(actionName);				
			
			/* find and invoke action */
			Target target = null;
			StringBuffer actionKey = new StringBuffer(ctrl.getClass().getName()).append("#").append(actionName);
			Method method = actionMethods.get(actionKey.toString());
			if (method == null) {
				throw new NoSuchActionException(actionName);
			} else {
				target = executeActionMethod(ctrl, method);
			}
			/* allow post-processing */
			ctrl.afterAction(actionName, target);

			target = target != null ? target : new NullTarget();
			return target;
		} catch (ForceTargetException fte) { // action may throw this exception to force a target to be rendered
			return fte.getTarget();
		}
	}

	/**
	 * Execute action method
	 * @param controller
	 * @param method
	 * @return a target to continue with
	 */
	private Target executeActionMethod(BaseController controller, Method method) {
		try {
			return (Target)method.invoke(controller);
		} catch (ForceTargetException e) {
			throw(e);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof ForceTargetException) {
				throw (ForceTargetException)(e.getTargetException());
			} else {
				log.error("could not invoke method '"+method.getName()+"'");
				log.errorException(e);
			}
		} catch (Throwable e) {
			log.error("could not invoke method '"+method.getName()+"'");
			log.errorException(e);
		}
		return new NullTarget();		
	}

	/**
	 * Gets application context.
	 * @return The application context
	 */
	public ServletContext getApplicationContext() {
		return applicationContext;
	}
	
	/**
	 * Gets date/time when Application was started. Useful to calculate up-time.
	 * @return startup date/time
	 */
	public Date getStartupAt() {
		return startupAt;
	}
	
	/**
	 * Gets velocity engine
	 * @return The engine
	 */
	public VelocityEngine getVelocityEngine() {
		return velocityEngine;
	}	
	
	/**
	 * Gets velocity tool manager
	 * @return The toolmanager
	 */
	public ViewToolManager getVelocityToolManager() {
		return velocityToolManager;
	}
	
	/**
	 * Gets value of init-parameter specified in filter declaration in web.xml
	 * @param key the parameter key
	 * @return the parameter value or null if no such parameter
	 */
	public String getInitParam(String key) {
		return initParams.get(key);
	}
	
	/**
	 * Gets value of init-parameter specified in filter declaration in web.xml
	 * @param key the parameter key
	 * @param defaultValue
	 * @return the parameter value or the defaultValue if no such parameter is specified.
	 */
	public String getInitParam(String key, String defaultValue) {
		String s = getInitParam(key);
		return s == null ? defaultValue : s;
	}	
	
	public void destroy() {
		System.out.println("Good Bye and Good Luck. "+applicationContext.getAttribute(APP_NAME_KEY)+" was up for "+getFormattedUptime()+".");
	}
	
	/**
	 * Finds best fitting locale from supported and accepted locales.
	 * @param supported ISO languages codes of locales accepted by the application
	 * @param accepted locales accepted by the browser
	 * @return the best fitting locale
	 */
	@SuppressWarnings("rawtypes")
	protected Locale computeDefaultLocale(List<String> supported, Enumeration accepted) {
		if (supported == null) {
			return null;
		}
		while (accepted.hasMoreElements()) {
			Locale l = (Locale)accepted.nextElement();
			if (supported.contains(l.getLanguage())) {
				return l;
			}
		}
		return new Locale((String)supportedLanguages.get(0));
	}

	public String getFormattedUptime() {
		Date now = new Date();
		long seconds = (now.getTime() - getStartupAt().getTime()) / 1000;
		long days = seconds / (24*3600);
		seconds = seconds % (24*3600);
		long hours = seconds / 3600;
		seconds = seconds % (3600);
		long minutes = seconds / 60;
		seconds = seconds % 60;
		StringBuffer s = new StringBuffer();
		if (days > 0) { s.append(days).append("d "); }
		s.append(hours<10?"0":"").append(hours).append(":");
		s.append(minutes<10?"0":"").append(minutes).append(":");
		s.append(seconds<10?"0":"").append(seconds);
		return s.toString();
	}
}
