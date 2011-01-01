package panama.tests;

import panama.core.BaseController;
import panama.core.TemplateTarget;
import junit.framework.TestCase;

public class BaseControllerTest extends TestCase {

	public void testRenderPathCreation() {
		BaseController ctrl = new BaseController();
		String abs = "/org/ridcully/pandora/template.vm";
		
		assertEquals(abs, ((TemplateTarget)ctrl.render(abs)).getTemplate());
		assertEquals(abs, ((TemplateTarget)ctrl.render("template.vm")).getTemplate());
		assertEquals(abs, ((TemplateTarget)ctrl.render("./template.vm")).getTemplate());
		assertEquals(abs, ((TemplateTarget)ctrl.render("template.vm")).getTemplate());
		assertEquals("/org/ridcully/template.vm", ((TemplateTarget)ctrl.render("../template.vm")).getTemplate());
		assertEquals("/org/template.vm", ((TemplateTarget)ctrl.render("../../template.vm")).getTemplate());
		assertEquals("/org/ridcully/pandora/sub/template.vm", ((TemplateTarget)ctrl.render("sub/template.vm")).getTemplate());
		assertEquals("/org/ridcully/pandora/sub/template.vm", ((TemplateTarget)ctrl.render("./sub/template.vm")).getTemplate());
		assertEquals("/org/ridcully/pandora/sub/sub/template.vm", ((TemplateTarget)ctrl.render("./sub/sub/template.vm")).getTemplate());
		assertEquals("/org/ridcully/parallel/template.vm", ((TemplateTarget)ctrl.render("../parallel/template.vm")).getTemplate());
		assertEquals("/org/ridcully/parallel/sub/template.vm", ((TemplateTarget)ctrl.render("../parallel/sub/template.vm")).getTemplate());
	}
}
