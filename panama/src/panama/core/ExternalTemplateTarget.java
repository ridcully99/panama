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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

/**
 * Use this to render a template other than a Velocity template.
 * The dispatcher of the application container (e. g. tomcat) is used to invoke the correct class for rendering.
 * @author Robert
 */
public class ExternalTemplateTarget extends Target {

	private String template;

	public ExternalTemplateTarget(String template) {
		super();
		setTemplate(template);
	}

	public String getTemplate() {
		return template;
	}
	public void setTemplate(String template) {
		this.template = template;
	}

	public void go() throws ServletException, IOException {

		applyStatusCode();

		String templateName = getTemplate();
		Context ctx = Context.getInstance();
		// prepend '/' if not there
		if (!templateName.startsWith("/")) {
			templateName = "/"+templateName;
		}
		RequestDispatcher dispatcher = ctx.getApplicationContext().getRequestDispatcher(templateName);
		dispatcher.forward(ctx.getRequest(), ctx.getResponse());
	}
}
