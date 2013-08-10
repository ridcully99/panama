package org.slf4j.impl;

import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

import panama.log.DebugLevel;
import panama.log.SimpleLogger;

/**
 * A wrapper over {@link SimpleLogger} in conforming to the {@link Logger}
 * interface.
 * 
 * <p>
 * Note that the logging levels mentioned in this class refer to those defined
 * in the @{link {@link DebugLevel} class.
 * 
 * @author ridcully
 */
public final class SimpleLoggerAdapter extends MarkerIgnoringBase {

	transient final SimpleLogger logger;

	SimpleLoggerAdapter(SimpleLogger logger) {
		this.logger = logger;
		this.name = logger.getConfigName();
	}

	// actual logging methods for all levels ---------------------------------------------------

	public void log(DebugLevel level, String format, Object arg) {
		if (logger.wouldLog(level)) {
			FormattingTuple ft = MessageFormatter.format(format, arg);
			logger.db(level, ft.getMessage());
		}
	}

	public void log(DebugLevel level, String format, Object arg1, Object arg2) {
		if (logger.wouldLog(level)) {
			FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
			logger.db(level, ft.getMessage());
		}
	}
	
	public void log(DebugLevel level, String format, Object... args) {
		if (logger.wouldLog(level)) {
			FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
			logger.db(level, ft.getMessage());
		}
	}

	public void log(DebugLevel level, String msg, Throwable t) {
		if (logger.wouldLog(level)) {
			logger.db(level, msg);
			logger.dbe(level, t);
		}
	}

	// interface implementations --------------------------------------------------------------
	
	/* (non-Javadoc)
	 * @see org.slf4j.Logger#isTraceEnabled()
	 */
	@Override
	public boolean isTraceEnabled() {
		// SL4J treats TRACE like a Log-Level
		return logger.wouldLog(DebugLevel.L6_VERBOSE);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#trace(java.lang.String)
	 */
	@Override
	public void trace(String msg) {
		log(DebugLevel.L6_VERBOSE, msg);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object)
	 */
	@Override
	public void trace(String format, Object arg) {
		log(DebugLevel.L6_VERBOSE, format, arg);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(String format, Object arg1, Object arg2) {
		log(DebugLevel.L6_VERBOSE, format, arg1, arg2);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void trace(String format, Object... arguments) {
		log(DebugLevel.L6_VERBOSE, format, arguments);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void trace(String msg, Throwable t) {
		log(DebugLevel.L6_VERBOSE, msg, t);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#isDebugEnabled()
	 */
	@Override
	public boolean isDebugEnabled() {
		return logger.wouldLog(DebugLevel.L5_DEBUG);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#debug(java.lang.String)
	 */
	@Override
	public void debug(String msg) {
		logger.debug(msg);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object)
	 */
	@Override
	public void debug(String format, Object arg) {
		log(DebugLevel.L5_DEBUG, format, arg);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(String format, Object arg1, Object arg2) {
		log(DebugLevel.L5_DEBUG, format, arg1, arg2);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void debug(String format, Object... arguments) {
		log(DebugLevel.L5_DEBUG, format, arguments);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#debug(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void debug(String msg, Throwable t) {
		log(DebugLevel.L5_DEBUG, msg, t);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#isInfoEnabled()
	 */
	@Override
	public boolean isInfoEnabled() {
		return logger.wouldLog(DebugLevel.L4_INFO);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#info(java.lang.String)
	 */
	@Override
	public void info(String msg) {
		logger.info(msg);
		
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#info(java.lang.String, java.lang.Object)
	 */
	@Override
	public void info(String format, Object arg) {
		log(DebugLevel.L4_INFO, format, arg);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#info(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(String format, Object arg1, Object arg2) {
		log(DebugLevel.L4_INFO, format, arg1, arg2);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#info(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void info(String format, Object... arguments) {
		log(DebugLevel.L4_INFO, format, arguments);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#info(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void info(String msg, Throwable t) {
		log(DebugLevel.L4_INFO, msg, t);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#isWarnEnabled()
	 */
	@Override
	public boolean isWarnEnabled() {
		return logger.wouldLog(DebugLevel.L3_WARN);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#warn(java.lang.String)
	 */
	@Override
	public void warn(String msg) {
		logger.warn(msg);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object)
	 */
	@Override
	public void warn(String format, Object arg) {
		log(DebugLevel.L3_WARN, format, arg);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(String format, Object arg1, Object arg2) {
		log(DebugLevel.L3_WARN, format, arg1, arg2);
	}
	
	/* (non-Javadoc)
	 * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void warn(String format, Object... arguments) {
		log(DebugLevel.L3_WARN, format, arguments);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void warn(String msg, Throwable t) {
		log(DebugLevel.L3_WARN, msg, t);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#isErrorEnabled()
	 */
	@Override
	public boolean isErrorEnabled() {
		return logger.wouldLog(DebugLevel.L2_ERROR);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#error(java.lang.String)
	 */
	@Override
	public void error(String msg) {
		logger.error(msg);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#error(java.lang.String, java.lang.Object)
	 */
	@Override
	public void error(String format, Object arg) {
		log(DebugLevel.L2_ERROR, format, arg);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#error(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(String format, Object arg1, Object arg2) {
		log(DebugLevel.L2_ERROR, format, arg1, arg2);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#error(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void error(String format, Object... arguments) {
		log(DebugLevel.L2_ERROR, format, arguments);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.Logger#error(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void error(String msg, Throwable t) {
		log(DebugLevel.L2_ERROR, msg, t);
	}

}