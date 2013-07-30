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

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Converts the specified json to a string and writes it as response.
 * @author ridcully
 */
public class JsonTarget extends Target {

	private String text;

	public JsonTarget(JSONObject json) {
		super();
		if (json != null) {
			this.text = json.toString();
		}
	}

	public JsonTarget(JSONArray json) {
		super();
		if (json != null) {
			this.text = json.toString();
		}
	}

	@Override
	public void go() throws Exception {
		Context ctx = Context.getInstance();
		ctx.getResponse().setContentType("application/json; charset=UTF-8");
		ctx.getResponse().getWriter().write(text);
	}
}
