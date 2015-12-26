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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
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
import org.scannotation.AnnotationDB;
import org.scannotation.WarUrlFinder;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.annotations.ContextParam;
import panama.exceptions.ForceTargetException;
import panama.exceptions.HttpErrorException;
import panama.exceptions.NoSuchActionException;
import panama.log.DebugLevel;
import panama.log.SimpleLogger;
import panama.util.Configuration;
import panama.util.DynaBeanUtils;
import panama.util.TestTimer;

import com.avaje.ebeaninternal.server.lib.ShutdownManager;


/**
 * The main dispatcher, implemented as a filter.
 *
 * @author Robert
 */
public class Dispatcher implements Filter {

	public final static String PREFIX = "panama";

	/* key for app name (i.e. context name without leading slash) in application context */
	public final static String APP_NAME_KEY = PREFIX+"_application_name";

	public final static String PARAM_LANGUAGES = PREFIX+".languages";
	public final static String PARAM_MAXFILEUPLOADSIZE = PREFIX+".maxfileuploadsize";

	private static final String SYSTEM_PROPERTY_VELOCITY_PROPS_SUFFIX = ".velocity.configuration";

	/** Key for default controller class (if specified) in controllerClasses -- value is crafted in a way never to collide with a normal alias or FQCN */
	private final static String DEFAULT_CONTROLLER_KEY = "/default/controller/class/";

	/* keys for objects put in request context */
	public final static String CONTEXT_KEY = "context";
	public final static String ACTION_INVOCATION_MODE_KEY = PREFIX+"action_invocation_mode";

	/* values for ACTION_INVOCATION_MODE_KEY */
	public final static String ACTION_INVOCATION_PROGRAMATICALLY = "programatically";
	public final static String ACTION_INVOCATION_BY_URL = "by_url";

	private final static int CAN_HANDLE_REQUEST_NO = 0;
	private final static int CAN_HANDLE_REQUEST_YES = 1;
	private final static int CAN_HANDLE_REQUEST_REDIRECT = 2;
	private final static int CAN_HANDLE_REQUEST_REDIRECT_TO_DEFAULT_CONTROLLER = 3;

	/** Simply! Logging */
	protected static SimpleLogger log = new SimpleLogger(Dispatcher.class);

	// ------------------------------------------------------------------------------ Inner classes


	class ActionInfo {

		class Parameter {
			Class<?> type;
			String contextParamName;
		}

		Method method;
		Parameter[] parameters;

		public ActionInfo(Method method) {
			this.method = method;
			Class<?>[] parameterTypes = method.getParameterTypes(); // never null
			Annotation[][] annotations = method.getParameterAnnotations(); // never null
			parameters = new Parameter[parameterTypes.length];
			for (int i = 0; i < parameters.length; i++) {
				Parameter arg = new Parameter();
				arg.type = parameterTypes[i];
				arg.contextParamName = getContextParamAnnotationValue(annotations[i]);
				parameters[i] = arg;
			}
		}

		/**
		 * Finds {@link ContextParam} annotation in given annotations and returns its value,
		 * which is the name of the parameter.
		 *
		 * @param annotations
		 * @return value or null
		 */
		private String getContextParamAnnotationValue(Annotation[] annotations) {
			if (annotations == null || annotations.length == 0) return null;
			for (Annotation a : annotations) {
				if (a instanceof ContextParam) {
					return ((ContextParam)a).value();
				}
			}
			return null;
		}
	}

	class ControllerInfo {
		Class<? extends BaseController> controllerClass;
		String defaultActionName;
		Map<String, ActionInfo> actions = new HashMap<>();
	}

	/** Mapping controller alias and FQCN to same ControllerInfo instance; filled in init(), later on only read from */
	private Map<String, ControllerInfo> controllers = new HashMap<>();

//	/** Mapping Controller-FQCN#actionName to Method; ; filled in init(), later on only read from */
//	private Map<String, Method> actionMethods = new HashMap<String, Method>();

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
		String appName = applicationContext.getContextPath().replace("/", "");
		applicationContext.setAttribute(APP_NAME_KEY, appName);

		Configuration.init(appName);

		@SuppressWarnings("rawtypes")
		Enumeration names = filterConfig.getInitParameterNames();
		while(names.hasMoreElements()) {
			String key = (String)names.nextElement();
			initParams.put(key, filterConfig.getInitParameter(key));
		}

		/* init velocity
		 * name of properties file is determined from
		 * a) System Property <contextname>.velocity.configuration
		 * b) System Property <PREFIX>.velocity.configuration
		 * c) velocity.properties
		 * d) default-fallback-velocity.properties
		 */
		try {
			ClassLoader cl = this.getClass().getClassLoader();
			Properties velocityProperties = Dispatcher.readProperties(
					System.getProperty(appName + SYSTEM_PROPERTY_VELOCITY_PROPS_SUFFIX),
					System.getProperty(PREFIX + SYSTEM_PROPERTY_VELOCITY_PROPS_SUFFIX),
					"/velocity.properties");
			if (velocityProperties.isEmpty()) {
				velocityProperties = new Properties();
				velocityProperties.load(cl.getResourceAsStream("default-fallback-velocity.properties"));
			}
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
				url = url + "/";
				if (!StringUtils.isEmpty(httpReq.getQueryString())) {
					url = url + "?" + httpReq.getQueryString();
				}
				httpRes.sendRedirect(httpRes.encodeRedirectURL(url));
				return;
				}
			case CAN_HANDLE_REQUEST_REDIRECT_TO_DEFAULT_CONTROLLER:
				{
				Class<?> controllerClass = controllers.get(DEFAULT_CONTROLLER_KEY).controllerClass;
				Controller annotation = controllerClass.getAnnotation(Controller.class);
				String ctrlName = annotation != null && !StringUtils.isEmpty(annotation.alias()) ? annotation.alias() : controllerClass.getName();
				String url = ctrlName + "/";	// relative url; by appending the '/' this targets the default action for the controller; otherwise we'd get another redirect that adds just the / (see above)
				HttpServletResponse httpRes = (HttpServletResponse)res;
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
	 * @return whether we can handle this request, a redirect is required or it should be passed along the filter chain.
	 */
	private int canHandleRequest(ServletRequest req, ServletResponse res) {
		if (!(req instanceof HttpServletRequest && res instanceof HttpServletResponse)) {
			return CAN_HANDLE_REQUEST_NO;
		}
		String path = ((HttpServletRequest)req).getServletPath();
		if ((path == null || path.length() == 0 || path.equals("/")) && controllers.containsKey(DEFAULT_CONTROLLER_KEY)) {
			return CAN_HANDLE_REQUEST_REDIRECT_TO_DEFAULT_CONTROLLER;
		}
		int result = CAN_HANDLE_REQUEST_NO;
		String[] ca = extractControllerAndActionNames(path);
		String ctrlName = ca[0];
		String actionName = ca[1];
		ControllerInfo controllerInfo = controllers.get(ctrlName);
		if (controllerInfo == null) {
			result = CAN_HANDLE_REQUEST_NO;
		} else {
			if (StringUtils.isEmpty(actionName)) {
				if (StringUtils.isEmpty(controllerInfo.defaultActionName)) {
					result = CAN_HANDLE_REQUEST_NO;
				} else if (path.trim().endsWith("/")) {
					result = CAN_HANDLE_REQUEST_YES;
				} else {
					result = CAN_HANDLE_REQUEST_REDIRECT;
				}
			} else {
				result = controllerInfo.actions.containsKey(actionName) ? CAN_HANDLE_REQUEST_YES : CAN_HANDLE_REQUEST_NO;
			}
		}
		return result;
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
			Context.destroyInstance();
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

		Set<String> ctrls = adb.getAnnotationIndex().get(Controller.class.getName());

		for (String fqcn : ctrls) {
			Class<? extends BaseController> clazz = null;
			try {
				clazz = (Class<? extends BaseController>) Class.forName(fqcn);
			} catch (Exception e) {
				log.warn("Class "+fqcn+" not found or not derived from Controller class. Ignoring it.");
				continue;
			}
			Controller controllerAnnotation = clazz.getAnnotation(Controller.class);

			ControllerInfo controllerInfo = new ControllerInfo();
			controllerInfo.controllerClass = clazz;
			controllerInfo.defaultActionName = controllerAnnotation.defaultAction();

			controllers.put(fqcn, controllerInfo);
			boolean isDefaultController = clazz.getAnnotation(Controller.class).isDefaultController();
			if (isDefaultController) {
				controllers.put(DEFAULT_CONTROLLER_KEY, controllerInfo);
			}

			String alias = controllerAnnotation.alias();
			if 	(StringUtils.isNotEmpty(alias)) {
				alias = alias.trim();
				if (alias.contains("/")) {
					log.warn("Controller alias '"+alias+"' contains illegal characters. Ignoring alias.");
					alias = null;
				} else if (controllers.containsKey(alias)) {
					log.warn("Duplicate Controller alias '"+alias+"' detected. Used by "+fqcn+
							" and " + controllers.get(alias).controllerClass.getName() + ". Using alias only for the latter.");
					alias = null;
				} else {
					controllers.put(alias, controllerInfo);
				}
			}
			log.debug((StringUtils.isNotEmpty(alias) ? alias + ", " : "") + fqcn + " -> " + fqcn + (isDefaultController ? " (default controller)" : ""));

			collectActions(controllerInfo);
		}
	}

	/**
	 * Collects all actions of specified controller.
	 *
	 * @param controllerInfo
	 */
	private void collectActions(ControllerInfo controllerInfo) {
		String defaultActionName = controllerInfo.defaultActionName;

		for (Method method : controllerInfo.controllerClass.getMethods()) {
			if (method.isAnnotationPresent(Action.class)) {
				Class<?> returnType = method.getReturnType();
				while (returnType != null) {
					if (returnType.equals(Target.class)) {
						ActionInfo actionInfo = new ActionInfo(method);
						String methodName = method.getName();
						controllerInfo.actions.put(methodName, actionInfo);
						String alias = method.getAnnotation(Action.class).alias();
						if (!StringUtils.isEmpty(alias)) {
							alias = alias.trim();
							if (alias.contains("/")) {
								log.warn("Action alias '"+alias+"' in " + controllerInfo.controllerClass.getName() + " contains illegal character '/'. Ignoring alias.");
								alias = null;
							} else {
								// same ActionInfo instance
								controllerInfo.actions.put(alias, actionInfo);
							}
						}
						log.debug("    "
								+ (!StringUtils.isEmpty(alias) ? alias + ", " : "") + methodName
								+ " -> "
								+ method.getName() + "()"
								+ ((alias != null && alias.equals(defaultActionName)) || methodName.equals(defaultActionName) ? " (defaultAction)" : ""));
						break;
					}
					returnType = returnType.getSuperclass();
				}
			}
		}
	}


	/**
	 * Creates a new instance for the controller specified by nameOrAlias
	 *
	 * @param nameOrAlias FQCN or alias of a controller class.
	 * @return Controller object or null
	 */
	public BaseController getController(String nameOrAlias) {
		BaseController ctrl = null;
		ControllerInfo controllerInfo = controllers.get(nameOrAlias);

		if (controllerInfo == null) {
			throw new RuntimeException("No controller class found for name or alias '"+nameOrAlias+"'");
		} else {
			try {
				ctrl = controllerInfo.controllerClass.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Error creating instance of controller class "
						+ controllerInfo.controllerClass.getName(),
						e);
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

		TestTimer timer = new TestTimer("execute");
		try {
			return executeAction(ctx, ctrlName, actionName);
		} catch (NoSuchActionException nme) {
			log.warn(nme.getMessage());
			ctx.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
			return null;
		} catch (HttpErrorException hee) {
			log.warn("HttpErrorException " + hee.getStatusCode() + " :\"" + ctrlName + "/" + actionName + "\"");
			ctx.getResponse().sendError(hee.getStatusCode());
			return null;
		} catch (Exception e) {
			String msg = "error doing action \"" + actionName + "\"";
			log.fatal(msg);
			log.fatalException(e);
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

	/**
	 * Executes action.
	 * @param ctx
	 * @param ctrlName
	 * @param actionName
	 * @return
	 * @throws NoSuchActionException
	 */
	public Target executeAction(Context ctx, String ctrlName, String actionName) throws NoSuchActionException {

		ControllerInfo controllerInfo = controllers.get(ctrlName);
		if (controllerInfo == null) {
			throw new NoSuchActionException(ctrlName + "/" + actionName);
		}

		/* create new instance of controller for specified ctrlName */
		BaseController ctrl = getController(ctrlName);

		if (StringUtils.isEmpty(actionName)) {
			actionName = controllerInfo.defaultActionName;
		}
		try {
			/* allow pre-processing */
			ctrl.beforeAction(actionName);

			/* find and invoke action */
			Target target = null;
			ActionInfo actionInfo = controllerInfo.actions.get(actionName);
			if (actionInfo == null) {
				throw new NoSuchActionException(ctrlName + "/" + actionName);
			}
			target = executeActionMethod(ctrl, actionInfo);

			/* allow post-processing */
			ctrl.afterAction(actionName, target);

			target = target != null ? target : new NullTarget();
			return target;
		} catch (ForceTargetException fte) { // action may throw this exception to force a target to be rendered
			return fte.getTarget();
		}
	}

	/**
	 * Executes action method
	 * @param controller
	 * @param actionInfo
	 * @return a target to continue with
	 */
	private Target executeActionMethod(BaseController controller, ActionInfo actionInfo) {
		Method method = actionInfo.method;
		try {
			Object[] values = buildArguments(actionInfo, controller.context);
			return (Target)method.invoke(controller, values);
		} catch (ForceTargetException e) {
			throw(e);
		} catch (HttpErrorException e) {
			throw(e);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof ForceTargetException) {
				throw (ForceTargetException)(e.getTargetException());
			} else if (e.getTargetException() instanceof HttpErrorException) {
				throw (HttpErrorException)(e.getTargetException());
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
	 * Builds arguments for all parameters of given actionInfo.
	 * For parameters annotated with {@link ContextParam} tries to find value from context's parameters.
	 * For all other parameters safe null values are used.
	 *
	 * @param actionInfo
	 * @param context
	 * @return
	 */
	private Object[] buildArguments(ActionInfo actionInfo, Context context) {
		Object[] values = new Object[actionInfo.parameters.length];
		for (int i = 0; i < actionInfo.parameters.length; i++) {
			ActionInfo.Parameter param = actionInfo.parameters[i];
			Object value = null;
			if (param.contextParamName != null) {
				// use contexts paramConvertUtil instance to be sure to be thread safe
				if (param.type.isArray()) {
					String[] paramValues = context.getParameterValues(param.contextParamName);
					value = context.paramConvertUtil.convert(paramValues, param.type);
				} else {
					String paramValue = context.getParameter(param.contextParamName);
					value = context.paramConvertUtil.convert(paramValue, param.type);
				}
			}
			// if (still) null and a primitive, apply safe primitive 'null' value
			if (value == null && param.type.isPrimitive()) {
				value = DynaBeanUtils.getNullValueForPrimitive(param.type);
			}
			values[i] = value;
		}
		return values;
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

	@Override
	public void destroy() {
		System.out.println("Good Bye and Good Luck. "+applicationContext.getAttribute(APP_NAME_KEY)+" was up for "+getFormattedUptime()+".");
		ShutdownManager.shutdown();	// Automatic shutdown of Ebean sometimes throws NPE when invoked too late, so we do it here explicitly.
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

	/**
	 * Tries to read properties from given fileNameOptions in given order from filesystem and as resource.
	 * As soon as properties could be read for a filename, those are returned.
	 * If there are no properties for any of the fileNameOptions, null is returned.
	 *
	 * @param fileNameOptions a list of filenames to be tried in given order
	 * @return Properties (will be empty if none of the filenames could be read)
	 */
	public static Properties readProperties(String... fileNameOptions) {
		Properties props = new Properties();
		for (String fileName : fileNameOptions) {
			if (fileName == null) continue;
			InputStream is = null;
			try {
				is = Dispatcher.class.getResourceAsStream(fileName);
				if (is == null) {	// no resource file, try as regular file
					log.info("could not read "+fileName+" as resource, going to try via file system now.");
					try {
						is = new FileInputStream(new File(fileName));
					} catch (Exception e) {
						log.warn("Error accessing "+fileName+" via file system: "+e.getMessage());
					}
				}
				if (is == null) continue;
				props.load(is);
				break;
			} catch (Exception e) {
				log.warn("Could not read properties from "+fileName+": "+e.getMessage());
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// NOOP
					}
				}
			}
		}
		return props;
	}
}
