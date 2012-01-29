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
package panama.persistence;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

/**
 * A basic bean for persistence of multi-language objects
 * @author Ridcully
 */
public class PolyglotPersistentBean extends PersistentBean implements Serializable {

	String language = "--";
	Translation translations = new Translation();

	public void setLanguage(String language) {
		this.language = language;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public Translation getTranslations() {
		return translations;
	}

	public void setTranslations(Translation translations) {
		this.translations = translations;
	}

	/**
	 * Creates a copy of current object with different language.
	 * The copy is connected to the current object via the Translation object they share.
	 * 
	 * You will want to overwrite this method if you want to duplicate collections etc. 
	 * If you do so, make sure to call the method of this base class (<code>super.createTranslatedObject(locale)</code>)
	 * to ensure that the language and the translation stuff is set correctly.
	 * 
	 * Additionally this copies the values of all basic fields (Integer, Long, Float, Double, Boolean, String and Date) to the new object.
	 * If you use a SecurityManager, only public fields can be copied, otherwise all fields will be copied (even private ...)
	 * 
	 * @param locale The language to translate to. If locale.language equals the language of the current object, the object itself (<code>this</code>) is returned and no copy is created.
	 * @return a flat not persisted copy of the object, or the object itself, or <code>null</code> if locale is <code>null</code>
	 */
	public PolyglotPersistentBean translateTo(Locale locale) {
		PolyglotPersistentBean result = null;
		if (locale != null) {
			try {
				if (locale.getLanguage().equals(this.getLanguage())) {
					result = this;
				} else {
					result = (PolyglotPersistentBean)this.getClass().newInstance();
					result.setLanguage(locale.getLanguage());
					result.setTranslations(this.getTranslations());
					copyPropertiesTo(result);
				} 
			} catch (Exception e) {
				result = null;
			}
		}
		return result;
	}
	
	/**
	 * Copies values of fields of supported types from current object to target object.
	 * @param target The object values are to be copied to.
	 */
	private void copyPropertiesTo(PolyglotPersistentBean target) {
		ArrayList supportedTypes = new ArrayList(Arrays.asList(new Class[] {Integer.class, Long.class, Float.class, Double.class, Boolean.class, String.class, java.util.Date.class }));
		Class clazz = this.getClass();
		
		HashSet fields = new HashSet();
		while(clazz != null && clazz != PolyglotPersistentBean.class) {
			Field[] fs = clazz.getDeclaredFields();
			fields.addAll(Arrays.asList(fs));
			clazz = clazz.getSuperclass();
		}
		for (Iterator i = fields.iterator(); i.hasNext(); ) {
			Field f = (Field)i.next();
			Class type = f.getType();
			if (supportedTypes.contains(type)) {
				try {
					/* force access even to private and protected fields - may fail if security manager is there; than only public fields will be copied */
					f.setAccessible(true);
					Object value = f.get(this);
					f.set(target, value);
				} catch (Exception e) {
					/* something went wrong, so copy lacks this value */
				}
			}
		}
	}
}
