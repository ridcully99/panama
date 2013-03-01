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
package panama.tests;

import panama.core.BaseController;
import panama.core.TemplateTarget;

public class BaseControllerTest extends ContextTestCase {

	public void testRenderPathCreation() {
		BaseController ctrl = new BaseController();
		String abs = "/panama/core/template.vm";
		
		assertEquals(abs, ((TemplateTarget)ctrl.render(abs)).getTemplate());
		assertEquals(abs, ((TemplateTarget)ctrl.render("template.vm")).getTemplate());
		assertEquals(abs, ((TemplateTarget)ctrl.render("./template.vm")).getTemplate());
		assertEquals(abs, ((TemplateTarget)ctrl.render("template.vm")).getTemplate());
		assertEquals("/panama/template.vm", ((TemplateTarget)ctrl.render("../template.vm")).getTemplate());
		assertEquals("/template.vm", ((TemplateTarget)ctrl.render("../../template.vm")).getTemplate());
		assertEquals("/panama/core/sub/template.vm", ((TemplateTarget)ctrl.render("sub/template.vm")).getTemplate());
		assertEquals("/panama/core/sub/template.vm", ((TemplateTarget)ctrl.render("./sub/template.vm")).getTemplate());
		assertEquals("/panama/core/sub/sub/template.vm", ((TemplateTarget)ctrl.render("./sub/sub/template.vm")).getTemplate());
		assertEquals("/panama/parallel/template.vm", ((TemplateTarget)ctrl.render("../parallel/template.vm")).getTemplate());
		assertEquals("/panama/parallel/sub/template.vm", ((TemplateTarget)ctrl.render("../parallel/sub/template.vm")).getTemplate());
	}
}
