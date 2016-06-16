/**
 *
 */
package panama.form;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import panama.form.Field;

/**
 * More flexible and localizable by allowing to pass DateFormat object to use.
 * @author Robert
 *
 */
public class DateField extends Field {

	private DateFormat dateFormat;

	public DateField(String name) {
		this(name, false);
	}

	/**
	 * @param name
	 * @param notEmpty
	 */
	public DateField(String name, boolean notEmpty) {
		this(name, notEmpty, DateFormat.getDateInstance());
	}

	/**
	 * For backward compatibility. If you have a format that depends on a Locale, better use
	 * the constructor with the DateFormat parameter.
	 * @param name
	 * @param notEmpty
	 * @param simplePattern for a {@link SimpleDateFormat}
	 */
	public DateField(String name, boolean notEmpty, String simplePattern) {
		super(name, Date.class, notEmpty);
		setDateFormat(new SimpleDateFormat(simplePattern));
	}

	/**
	 * @param name
	 * @param dateFormat
	 */
	public DateField(String name, DateFormat dateFormat) {
		super(name, Date.class, false);
		setDateFormat(dateFormat);
	}

	/**
	 * @param name
	 * @param notEmpty
	 * @param dateFormat
	 */
	public DateField(String name, boolean notEmpty, DateFormat dateFormat) {
		super(name, Date.class, notEmpty);
		setDateFormat(dateFormat);
	}

	/**
	 * @param name
	 * @param dateFormat
	 * @param notEmpty
	 */
	public DateField(String name, DateFormat dateFormat, boolean notEmpty) {
		super(name, Date.class, notEmpty);
		setDateFormat(dateFormat);
	}

	/**
	 * This converts the given string into the value itself
	 * @see Field#stringToValue(String)
	 * @param valueString
	 * @return A Date object
	 * @throws ParseException
	 */
	protected Object stringToValue(String valueString) throws ParseException {
		return dateFormat.parse(valueString);
	}

	/**
	 * This converts the given value into a string representation
	 * @see Field#valueToString(Object)
	 * @param value
	 * @return A String
	 * @throws ParseException
	 */
	public String valueToString(Object value) throws ParseException {
		return dateFormat.format(value);
	}

	public DateFormat getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}
}
