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
package panama.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import panama.exceptions.PropertyNotFoundException;
import panama.exceptions.WrongValueTypeException;



/**
 * A collection of static methods for dynamic bean access.
 *
 * @author Ridcully
 */
public class DynaBeanUtils {

	/**
	 * Gets value of property of this object (using reflection)
	 *
	 * If propertyName contains '.' get property for first part of name and call getProperty of this property with rest of propertyName
	 *
	 * @param propertyName (simple or linked by dots (eg. user.name))
	 * @return the value of the named property
	 * @throws PropertyNotFoundException If no getter for propertyName is found
	 */
	public static Object getProperty(Object bean, String propertyName) throws PropertyNotFoundException {

		Class<?> c = bean.getClass();
		Object value = null;

		String subProperty = null;

		if (propertyName.indexOf(".") != -1) {
			subProperty = propertyName.substring(propertyName.indexOf(".")+1);
			propertyName = propertyName.substring(0, propertyName.indexOf("."));
		}
		String getterName = getterName(propertyName);
		try {
			Method getterMethod = c.getMethod(getterName);
			value = getterMethod.invoke(bean);
			if (subProperty != null && value != null) {
				value = getProperty(value, subProperty);
			}
		}
		catch (Exception e) {
			// try with isName if we have a primitive boolean
			if (getPropertyClass(bean, propertyName) == Boolean.TYPE) {
				getterName = isName(propertyName);
				try {
					Method getterMethod = c.getMethod(getterName);
					value = getterMethod.invoke(bean);
					if (subProperty != null && value != null) {
						value = getProperty(value, subProperty);
					}
				} catch (Exception e2) {
					throw new PropertyNotFoundException(bean.getClass(), propertyName);
				}
			} else {
				throw new PropertyNotFoundException(bean.getClass(), propertyName);
			}
		}
		return value;
	}

	/**
	 * Sets a property of this object to a given value (using reflection)
	 *
	 * If propertyName contains '.' get property for first part of name and call setProperty of this property with rest of propertyName
	 *
	 * @param propertyName (simple or linked by dots (eg. user.name))
	 * @param value The value (may also be an Array)
	 * @throws PropertyNotFoundException if no getter or setter for propertyName is found
	 * @throws WrongValueTypeException if class of value does not match class of property
	 */
	public static void setProperty(Object bean, String propertyName, Object value) throws PropertyNotFoundException, WrongValueTypeException {

		Class<?> c = bean.getClass();

		String subProperty = null;

		if (propertyName.indexOf(".") != -1) {
			subProperty = propertyName.substring(propertyName.indexOf(".")+1);
			propertyName = propertyName.substring(0, propertyName.indexOf("."));
		}

		if (subProperty != null) {
			// get property-value and call it's setProperty for subProperty
			Object o = getProperty(bean, propertyName);
			setProperty(o, subProperty, value);
		}
		else {
			if (isVersionField(bean.getClass(), propertyName)) {
				return;
			}
			Class<?> valueClass = getPropertyClass(bean.getClass(), propertyName);

			if (valueClass.isPrimitive() && value == null) {
				value = getNullValueForPrimitive(valueClass);
			}

			String setterName = setterName(propertyName);
			String getterName = getterName(propertyName);
			try {
				Method setterMethod = c.getMethod(setterName, new Class[]{valueClass});
				Method getterMethod = null;
				try {
					getterMethod = c.getMethod(getterName);
				} catch (Exception e) {
					// if not there, it cannot be annotated, that's okay.
				}
				if (isVersionMethod(setterMethod) || isVersionMethod(getterMethod)) {
					return;
				}
				try {
					setterMethod.invoke(bean, value);
				} catch (Exception e) {
					throw new WrongValueTypeException(bean.getClass(), propertyName, valueClass);
				}
			} catch (Exception e) {
				throw new PropertyNotFoundException(bean.getClass(), propertyName);
			}
		}
	}

	public static Class<?> getPropertyClass(Object bean, String propertyName) throws PropertyNotFoundException {
		return getPropertyClass(bean.getClass(), propertyName);
	}

	public static Class<?> getPropertyClass(Class<?> clazz, String propertyName) throws PropertyNotFoundException {

		String originalPropertyName = propertyName;
		String subProperty = null;

		if (propertyName.indexOf(".") != -1) {
			subProperty = propertyName.substring(propertyName.indexOf(".")+1);
			propertyName = propertyName.substring(0, propertyName.indexOf("."));
		}
		String getterName = getterName(propertyName);
		try {
			Method getterMethod = null;
			try {
				getterMethod = clazz.getMethod(getterName);
			} catch (NoSuchMethodException e) {
				getterMethod = clazz.getMethod(isName(propertyName));	// also might throw exception but this is caught from the outer catch
				// if not a boolean, isXY is not a valid 'getter'
				if (getterMethod.getReturnType() != Boolean.TYPE) throw new PropertyNotFoundException(clazz.getClass(), originalPropertyName);
			}
			Class<?> type = getterMethod.getReturnType();
			if (subProperty != null) {
				type = getPropertyClass(type, subProperty);
			}
			return type;
		}
		catch (Exception e) {
			throw new PropertyNotFoundException(clazz.getClass(), propertyName);
		}
	}

	/**
	 * Returns the names of all properties of this class.
	 * Properties are recognized by their getters and setters.
	 * @param bean the object to work with
	 * @return an array with the names of all properties of this class
	 */
	public static String[] getPropertyNames(Object bean) {
		return getPropertyNames(bean, false);
	}

	/**
	 * Returns the names of all properties of this class.
	 * Properties are recognized by their getters and setters.
	 * @param bean the object to work with
	 * @param includeSubProperties wether to add subproperties too (as foo.bar)
	 * @return an array with the names of all properties of this class
	 */
	public static String[] getPropertyNames(Object bean, boolean includeSubProperties) {
		return getPropertyNames(bean.getClass(), includeSubProperties);
	}

	private static String[] getPropertyNames(Class<?> clazz, boolean includeSubProperties) {
		Method[] methods = clazz.getMethods();
		Set<String> properties = new HashSet<String>();
		for (int i=0; i<methods.length; i++) {
			Method m = methods[i];
			if (m.getName().startsWith("set") && m.getName().length()>3 && m.getParameterTypes().length == 1) {	// setter?
				String propertyName = propertyName(m.getName());
				String getterName = getterName(propertyName);
				try {	// check if there is a getter-method.
					Method getterMethod = null;
					try {
						getterMethod = clazz.getMethod(getterName);
					} catch (NoSuchMethodException e) {
						getterMethod = clazz.getMethod(isName(propertyName));	// also might throw exception but this is caught from the outer catch
						// if not a boolean, isXY is not a valid 'getter'
						if (getterMethod.getReturnType() != Boolean.TYPE) throw new PropertyNotFoundException(clazz.getClass(), propertyName);
					}
					Class<?> type = getterMethod.getReturnType();
					if (type == m.getParameterTypes()[0]) {
						properties.add(propertyName);
						if (includeSubProperties) {
							for (String subProperty : getPropertyNames(type)) {
								properties.add(propertyName+"."+subProperty);
							}
						}
					}
				} catch (Exception e) { /* NOP */ }
			}
		}
		String[] result = new String[properties.size()];
		properties.toArray(result);
		return result;
	}

	/**
	 * Creates the getter name for a property.
	 * @param propertyName
	 * @return name of getter method.
	 */
	private static String getterName(String propertyName) {
		String firstLetter = propertyName.substring(0, 1);
		return "get" + firstLetter.toUpperCase() + propertyName.substring(1);
	}

	/**
	 * Creates the 'is' name for a boolean property.
	 * @param propertyName
	 * @return name of is method.
	 */
	private static String isName(String propertyName) {
		String firstLetter = propertyName.substring(0, 1);
		return "is" + firstLetter.toUpperCase() + propertyName.substring(1);
	}

	/**
	 * Creates the setter name for a property
	 * @param propertyName
	 * @return name of setter method.
	 */
	private static String setterName(String propertyName) {
		String firstLetter = propertyName.substring(0, 1);
		return "set" + firstLetter.toUpperCase() + propertyName.substring(1);
	}

	/**
	 * Extracts the property name from a getter/setter method name.
	 * @param method
	 * @return name of property
	 */
	private static String propertyName(String method) {
		String name = method.substring(3,4).toLowerCase();
		if (method.length() > 4) {
			name += method.substring(4);
		}
		return name;
	}

	/**
	 * Checks whether the property is annotated as javax.persistence.Version
	 * @param beanType
	 * @param property
	 * @return true if annotated as Version, false otherwise
	 */
	private static boolean isVersionField(Class<?> beanType, String property) {
		try {
			Field field = beanType.getDeclaredField(property);
			return field.getAnnotation(javax.persistence.Version.class) != null;
		} catch (Exception e) {
			Class<?> superClass = beanType.getSuperclass();
			if (superClass != null) {
				return isVersionField(superClass, property);
			} else {
				return false;
			}
		}
	}

	/**
	 * checks whether the method is annotated as javax.persistence.Version
	 * @param method
	 * @return true if annotated as Version, false otherwise
	 */
	private static boolean isVersionMethod(Method method) {
		return method != null && method.getAnnotation(javax.persistence.Version.class) != null;
	}

	/**
	 * Gets a non null default value for primitives.
	 * @param primitiveClass
	 * @return a non-null object
	 */
	public static Object getNullValueForPrimitive(Class<?> primitiveClass) {
		if (primitiveClass == Long.TYPE) {
			return new Long(0);
		} else if (primitiveClass == Integer.TYPE) {
			return new Integer(0);
		} else if (primitiveClass == Float.TYPE) {
			return new Float(0);
		} else if (primitiveClass == Double.TYPE) {
			return new Double(0);
		} else if (primitiveClass == Boolean.TYPE) {
			return new Boolean(false);
		} else if (primitiveClass == Byte.TYPE) {
			return new Byte((byte)0);
		} else if (primitiveClass == Character.TYPE) {
			return new Character('\0');
		} else if (primitiveClass == Short.TYPE) {
			return new Short((short)0);
		} else {
			return null;	// not a primitive class
		}
	}

	/**
	 * Tries to convert string to specified primitiveCass.
	 * @param primitiveClass
	 * @param valueString
	 * @return native representation of valueString in the specified primitiveClaass. If the class ist not primitive, null is returned!
	 * @throws NumberFormatException
	 */
	public static Object parsePrimitive(Class<?> primitiveClass, String valueString) throws NumberFormatException {
		if (primitiveClass == Long.TYPE) {
			return Long.parseLong(valueString);
		} else if (primitiveClass == Integer.TYPE) {
			return Integer.parseInt(valueString);
		} else if (primitiveClass == Float.TYPE) {
			return Float.parseFloat(valueString);
		} else if (primitiveClass == Double.TYPE) {
			return Double.parseDouble(valueString);
		} else if (primitiveClass == Boolean.TYPE) {
			return Boolean.parseBoolean(valueString);
		} else if (primitiveClass == Byte.TYPE) {
			return Byte.parseByte(valueString);
		} else if (primitiveClass == Character.TYPE) {
			return valueString.charAt(0);
		} else if (primitiveClass == Short.TYPE) {
			return Short.parseShort(valueString);
		} else {
			return null;	// not a primitive class
		}
	}
}
