package panama.tests;

import panama.core.BaseController;
import panama.core.TemplateTarget;
import junit.framework.TestCase;

public class BaseControllerTest extends TestCase {

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
