package cn.bran.play;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.text.StrSubstitutor;

import cn.bran.japid.util.UrlMapper;

import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.exceptions.ActionNotFoundException;
import play.exceptions.NoRouteFoundException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.mvc.ActionInvoker;
import play.mvc.Router;
import play.mvc.Http.Request;
import play.mvc.Router.ActionDefinition;
import play.mvc.Router.Route;

/**
 * the logic is modeled after the the Play!'s {@code Template.ActionBridge.}
 * 
 * 
 * @author bran
 * 
 */
public class RouteAdapter implements UrlMapper {
	// String controllerName;

	/**
	 * 
	 * @param controllerName
	 *            the current controller's name. If null is specified, the
	 *            Request.current().controller will be used
	 */
	public RouteAdapter() {
		super();
		// this.controllerName = controllerName;
	}

	/**
	 * Some sample form of action: CRUD.index(), blank(), attachment(_id, _name)
	 * this one has method call inside of controller param list
	 * 
	 * @{Application.show(post.previous().id)
	 * 
	 *                                        this is a super long one: *
	 * 
	 * @{list( ).remove('page').add('search', params.search).add('order',
	 *         (_caller.order == 'DESC' ? 'ASC' : 'DESC'))} in the table.html in
	 * 
	 *         the CRUD module
	 * 
	 *         this one has property access in the param list:
	 * @{show(_caller.object.id)
	 * 
	 * @param actionString
	 *            the leasing part of the whole expression, e.g.
	 *            Application.show in @{Application.show(post.previous().id)}.
	 *            it can also bear the form of a simple function call, such as:
	 *            show
	 * @param params
	 *            the runtime value of the parameters used to call the action,
	 *            e.g. 123 from evaluating "post.previous().id"
	 */
	@Override
	public String lookup(String actionString, Object[] params) {
		return reverseWithCache(actionString, params);

	}

	@Override
	public String lookupAbs(String action, Object[] args) {
		return Request.current().getBase() + this.lookup(action, args);
	}

	@Override
	public String lookupStatic(String resource) {
		return reverseStaticLookup(resource);
		// return Router.reverseWithCheck(resource,
		// Play.getVirtualFile(resource));
	}

	@Override
	public String lookupStaticAbs(String resource) {
		return Request.current().getBase() + this.lookupStatic(resource);
	}

	/**
	 * 
	 * add cache check before passing to the heavy reverse check
	 * 
	 * @author Bing Ran<bing_ran@hotmail.com>
	 * @param action
	 * @param args
	 * @return
	 */
	public static String reverseWithCache(String actionString, Object[] params) {
		// compose the key:

		try {
			String controllerName = Request.current().controller;
			// forms: Controller.action, action, package.Controller.action
			String action = actionString;
			if (actionString.indexOf(".") > 0) {
				// fell spec with controller name
			} else {
				action = controllerName + "." + actionString;
			}
			if (action.endsWith(".call")) {
				action = action.substring(0, action.length() - 5);
			}
			try {
				// find out the param types
				String[] targetParamNames = getActionParamCache().get(action);
				if (targetParamNames == null) {
					Method actionMethod = (Method) ActionInvoker.getActionMethod(action)[1];
					Class<?> actionClass = actionMethod.getDeclaringClass();
					Integer methodHash = LocalVariablesNamesTracer.computeMethodHash(actionMethod.getParameterTypes());
					String synthFieldForMethod = "$" + actionMethod.getName() + methodHash;
					targetParamNames = (String[]) actionClass.getDeclaredField(synthFieldForMethod).get(null);
					getActionParamCache().put(action, targetParamNames);
				}
				// too many parameters versus action, possibly a developer
				// error. we must warn him.
				if (targetParamNames.length < params.length) {
					throw new NoRouteFoundException(action, null);
				}

				String k = action;
				for (String p : targetParamNames) {
					k += p;
				}

				// assemble the named parameter hash
				Map<String, Object> paramMap = new HashMap<String, Object>();
				for (int i = 0; i < params.length; i++) {
					String key = i < targetParamNames.length ? targetParamNames[i] : "";
					String value = params[i] == null ? null : params[i].toString();
					paramMap.put(key, value);
				}

				// check cache first before go the expensive result
				String urlPattern = getActionCache().get(k); // format
																		// /action/{id}/{tag}
																		// etc
				if (urlPattern != null) {
					StrSubstitutor sub = new StrSubstitutor(paramMap);
					sub.setVariablePrefix("{");
					sub.setVariableSuffix("}");
					String url = sub.replace(urlPattern);
					return url;
				} else {
					ActionDefinition actionDef = Router.reverse(action, paramMap);
					Route route = actionDef.route;
					// find is the route is cacheable. it's hard to cache path
					// with constrained parameter
					// 
					String path = route.path;
					if (!path.contains("<")) {
						// cacheable
						if (path.endsWith("?"))
							path = path.substring(0, path.length() - 1);
						getActionCache().put(k, path);
					}
					return actionDef.toString();
				}
			} catch (ActionNotFoundException e) {
				throw new NoRouteFoundException(action, null);
			}
		} catch (Exception e) {
			if (e instanceof PlayException) {
				throw (PlayException) e;
			}
			throw new UnexpectedException(e);
		}
	}

	/**
	 * @return
	 */
	private static HashMap<String, String> getActionCache() {
		HashMap<String, String> hash = actionReverseCache.get();
		if (hash == null) {
			hash = new HashMap<String, String>();
			actionReverseCache.set(hash);
		}
		return hash;
	}

	/**
	 * @return
	 */
	private static HashMap<String, String[]> getActionParamCache() {
		HashMap<String, String[]> hash = actionParamNamesCache.get();
		if (hash == null) {
			hash = new HashMap<String, String[]>();
			actionParamNamesCache.set(hash);
		}
		return hash;
	}

	/**
	 * reverse looup a static resource
	 * 
	 * provide cache
	 * 
	 * @author Bing Ran<bing_ran@hotmail.com>
	 * @param resource
	 * @return
	 */
	public static String reverseStaticLookup(String resource) {
		HashMap<String, String> hash = getStaticCache();
		String url = hash.get(resource);
		if (url == null) {
			url = Router.reverseWithCheck(resource, Play.getVirtualFile(resource));
			hash.put(resource, url);
		}
		return url;
	}

	/**
	 * @return
	 */
	private static HashMap<String, String> getStaticCache() {
		HashMap<String, String> hash = staticCache.get();
		if (hash == null) {
			hash = new HashMap<String, String>();
			staticCache.set(hash);
		}
		return hash;
	}

	// cache lookups on the current thread.
	// ideally they can be done in the router for longer persistence and be
	// synched with route table reloading.

	// store quick reverse lookup
	static ThreadLocal<HashMap<String, String>> staticCache = new ThreadLocal<HashMap<String, String>>();
	// <action & param hash, url pattern>
	static ThreadLocal<HashMap<String, String>> actionReverseCache = new ThreadLocal<HashMap<String, String>>();
	// <action, paramNames>
	static ThreadLocal<HashMap<String, String[]>> actionParamNamesCache = new ThreadLocal<HashMap<String, String[]>>();

}