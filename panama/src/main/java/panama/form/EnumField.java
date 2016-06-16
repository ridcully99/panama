/**
 *
 */
package panama.form;

import java.text.ParseException;

import panama.form.Field;

/**
 * @author robert.brandner
 *
 */
public class EnumField extends Field {

	private Class<? extends Enum<?>> enumClass;

	public EnumField(String name, Class<? extends Enum<?>> enumClass) {
		this(name, enumClass, false);
	}

	public EnumField(String fieldName, Class<? extends Enum<?>> enumClass, boolean notEmpty) {
		super(fieldName, enumClass, notEmpty);
		this.enumClass = enumClass;
	}

	/**
	 * This converts the given string into the value itself
	 * @see Field#stringToValue(String)
	 * @param valueString
	 * @return An Enum value
	 * @throws ParseException
	 */
	protected Object stringToValue(String valueString) throws ParseException {
		for (Enum<?> e : enumClass.getEnumConstants()) {
			if (e.toString().equals(valueString)) return e;
		}
		return null;
	}

	/**
	 * This converts the given value into a string representation
	 * @see Field#valueToString(Object)
	 * @param value
	 * @return A String
	 * @throws ParseException
	 */
	public String valueToString(Object value) throws ParseException {
		return ((Enum<?>)value).toString();
	}
}
