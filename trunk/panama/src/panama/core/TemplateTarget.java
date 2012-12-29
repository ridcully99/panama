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

import java.util.Enumeration;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.view.ViewToolContext;

/**
 * Use this to render a Velocity template.
 * To render JSP or other, @see {@link ExternalTemplateTarget}
 * @author Robert
 */
public class TemplateTarget extends Target {

	private String template;
	
	public TemplateTarget(String template) {
		super();
		setTemplate(template);
	}

	public String getTemplate() {
		return template;
	}
	public void setTemplate(String template) {
		this.template = template;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void go() throws Exception {
		
		Context ctx = Context.getInstance();
		VelocityEngine engine = ctx.getCore().getVelocityEngine();
		ViewToolContext velocityContext = ctx.getCore().getVelocityToolManager().createContext(ctx.getRequest(), ctx.getResponse());
		ctx.getResponse().setContentType("text/html;charset=UTF-8");
		
		// put all from request scope into context (this also contains the reference to the context itself by Dispatcher.CONTEXT_KEY)
		String key = null;
		for (Enumeration<String> e = ctx.getRequest().getAttributeNames(); e.hasMoreElements();) {
			key = e.nextElement();
			velocityContext.put(key, ctx.getRequest().getAttribute(key));
		}
		// TODO ? put all from application- and session- scope into context too. Rather not.
		Template template = null;
		template = engine.getTemplate(this.template, "UTF-8");
		template.merge(velocityContext, ctx.getResponse().getWriter());
	}
}
