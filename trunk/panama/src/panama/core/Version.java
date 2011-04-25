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
package panama.core;

/**
 * This class contains the current version of Panama.
 * @author Ridcully
 *
 */
public class Version {

	// Version-String: <a>.<b>.<c>
	// a: Version
	// b: Revision (increases when cool new features are added)
	// c: Minor	(increases when updates or fixes are made - even numbers mark stable releases)
	
	public final static String VERSION = "3.3.2";
	
	public final static String LOGO_ASCIIART = 
	" ______ _______ _______ _______ _______ _______ \n"+
	"|   __ \\   _   |    |  |   _   |   |   |   _   |\n"+
	"|    __/       |       |       |       |       |\n"+
	"|___|  |___|___|__|____|___|___|__|_|__|___|___|\n"+
	"\n"+
	"Lightweight Web-Application Framework "+VERSION+"\n";
		

	/* 	History
	 * ---------------------------------------------------------------------------------
	 * 3.3.2    2011-04-25 Read parameter map from request lazily (See Issue #10) 
	 * 3.3.1    2011-04-13 Fixed a problem when tomcat restores tables in session scope after restart and query is gone.
	 * 3.3.0    2011-04-01 Reduced allowed URLs to controller[/action], checking for illegal aliases; update to Ebean 7.2.3 and adaptions to Filters to work with it.
	 * 3.2.6	2011-03-23 Fixed massive bug with ThreadLocal context (Gott sei Dank gefunden!)
	 * 3.2.4    2011-03-21 support for primitives in Form system
	 * 3.2.3	2011-03-10 little fixes to allow derived versions of dispatcher.
	 *          2010-12-31 switched to other name
	 * 3.2.2  - 2010-12-06 Fix for JsonTarget - send correct content type (application/json) and do utf-8 encoding
	 * 3.2.1  - 2010-11-15 Allow dots in action names
	 * 3.2.0  - 2010-11-14 Ebean update to 2.7.1 and added json-simple.jar and a JsonTarget for easier use with JSON/AJAX
	 * 3.1.0  - 2010-11-02 Ebean ORM, Removed authorization as well, config via Filter Init-Parameters, no more pandora.xml
	 * 3.1.0 alpha - 2010-10-24   Removed quartz and hibernate -> switch to Ebean (and perhaps a flexible plugin System)
	 * 3.0.0  - 2010-10-22 Finally!
	 * -""-   - 2010-10-07 Filter statt Servlet --> auch pretty urls möglich
	 *                     RequestEncodeFilter in Dispatcher integriert
	 * 3.0.0.alpha - 2010-09-.. XSS entfernt, Controller/Action Konzept vereinfacht mithilfe von Annotations
	 * 2.3.0  - 2010-08-03 Included optional XSS (cross side scripting) protection for all FormFields (stripping all html from the input). Uses jericho htmlparser (http://jericho.htmlparser.net) introducing a new dependency (jericho-html-3.1.jar)
	 * 2.2.0  - 2010-04-27 Included misc. updates, applied Apache License and published on code.google.com/p/pandora-web-framework!
	 * 2.0.4  - 2010-03-23 Added FileItem Field with necessary updates to FormData and Context -- see Mediathek#save() for usage.
	 * 2.0.3  - 2010-01-22 Added Getters for Value-Arrays to FormData (e.g. getStrings(), getLongs())
	 * 2.0.2  - 2010-01-12 Form, FormData und IntelliBean erweitert um auch Array-Properties zu unterstützen (siehe FormDataTest)
	 * 2.0.1  - 2009-12-28 QuartzSupport integriert
	 * 2.0.0  - 2009-12-08 Filter erweitert (all, any) und mit Java 5 Features aufgewertet --> Versionssprung auf 2.0.0!
	 * 1.9.10 - 2009-12-06 FormData presets input for BooleanFields with FALSE (for easier handling of checkboxes)
	 * 1.9.9  - 2009-11-28 IntelliBean now supports subProperties also in getPropertyNames and getPropertyValueClass
	 * 1.9.8  - 2009-11-05 Changed UUID creation in PersistentBean so that there are no colons (:) in it
	 * 1.9.7  - 2009-10-29 Added FormData#getInput() method that always returns an array.
	 * 1.9.6  - 2008-06-03 pandora.showerrors Config-Parameter (allows to not show exceptions in production environment)
	 * 1.9.5  - 2008-05-21 Changed signature of ValidatorException constructor to use a list for additional parameters
	 * 1.9.4  - 2008-05-15 Fixed MessageTool flaw
	 * 1.9.3  - 2008-04-?? Create context in synchronized way and also the Hibernate session if required 
	 * 1.9.1  - 2006-12-10 Switched common Exceptions (ForceTarget...) to RuntimeExceptions for cleaner source-code, switched to Java 5
	 * 1.7.11 - 2006-11-09 Additional addAction methods and a GenericPandoraAction class
	 * 1.7.10 - 2006-10-17 Multi-Language support via PolyglotFilter 
	 * 1.7.02 - 2006-10-06 New href() method in PandorasBox Tool - for encodeURL() in templates
	 * 1.7.01 - 2006-08-31 EasyCache added (wrapper for WhirlyCache)
	 * 1.6.23 - 2006-08-22 renamed FastDBTable to FastCriteriaTable and fixed behavior that getRows returned only rows of current page.
	 * 1.6.22 - 2006-08-19 DefaultTable sorting of String properties is now case-insensitive
	 */
	
}

	