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
package panama.examples.tiles;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.core.BaseController;
import panama.core.Target;

/**
 * @author ridcully
 *
 */
@Controller(alias="tilesexample", defaultAction="view")
public class TilesExampleController extends BaseController {

	@Action
	public Target view() {
		return render("main.vm");
	}

	@Action
	public Target header() {
		return render("header.vm");
	}

	@Action
	public Target hello() {
		String name = context.getParameter("name");
		if (name == null) {
			name = "World";
		}
		context.put("name", name);
		context.put("red", (int)(128+Math.random()*127d));
		context.put("green", (int)(128+Math.random()*127d));
		context.put("blue", (int)(128+Math.random()*127d));
		return render("hello.vm");
	}
}
