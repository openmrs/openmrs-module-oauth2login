/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.oauth2login.authscheme;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * This is an object representation of the OAuth2 user info response with convenience methods to
 * extract useful information from it.
 */
public class UserInfo {
	
	private final static Logger log = LoggerFactory.getLogger(UserInfo.class);
	
	public final static String MAPPINGS_PFX = "openmrs.mapping.";
	
	public final static String PROP_USERNAME = MAPPINGS_PFX + "user.username";
	
	public final static String PROP_EMAIL = MAPPINGS_PFX + "user.email";
	
	public final static String PROP_GIVEN_NAME = MAPPINGS_PFX + "person.givenName";
	
	public final static String PROP_MIDDLE_NAME = MAPPINGS_PFX + "person.middleName";
	
	public final static String PROP_FAMILY_NAME = MAPPINGS_PFX + "person.familyName";
	
	public final static String PROP_GENDER = MAPPINGS_PFX + "person.gender";
	
	public final static String PROP_SYSTEMID = MAPPINGS_PFX + "user.systemId";
	
	public static final String PROP_ROLES = MAPPINGS_PFX + "user.roles";
	
	private String json; // the user info json
	
	private Properties props;
	
	/**
	 * The user info object representation is built from the user info JSON and the OAuth2 mapping
	 * properties file. The OAuth2 properties file defines the mappings between OpenMRS meaningful
	 * values (such as 'username', 'user/person first name', 'user email', 'user/person gender' ...
	 * etc, and the root level keys where they are found in the user info JSON.
	 * 
	 * @param oauth2Props A mapping file that
	 * @param userInfoJson The user info JSON.
	 */
	public UserInfo(Properties oauth2Props, String userInfoJson) {
		this.props = oauth2Props;
		this.json = userInfoJson;
	}
	
	@Override
	public String toString() {
		return getUsername();
	}
	
	/**
	 * Fetches the value in the user info JSON based on the property key as defined in the OAuth2
	 * properties file. If the key is not defined in the OAuth2 properties file or if the value for
	 * that key cannot be parsed from the user info JSON file, then the fallback default value is
	 * returned.
	 * 
	 * @param propertyKey The OAuth2 mapping file property key, eg. "openmrs.mapping.user.username".
	 * @param defaultValue The default value.
	 * @return
	 */
	public String get(String propertyKey, String defaultValue) {
		String res = defaultValue;
		
		if (!props.containsKey(propertyKey)) {
			log.warn("There was an attempt to read the value of " + propertyKey
			        + " out of the user info JSON, but the JSON property could not be found.");
			return res;
		}
		
		String jsonKey = props.getProperty(propertyKey);
		try {
			res = JsonPath.read(json, "$." + jsonKey);
		}
		catch (PathNotFoundException e) {
			log.error("There was an error when reading the the JSON path $." + jsonKey + " in the user info JSON.", e);
		}
		
		return res;
	}
	
	/**
	 * @see #get(String, String)
	 */
	public String get(String propertyKey) {
		return get(propertyKey, null);
	}
	
	/**
	 * Convenience method that retrieves the OpenMRS user name from the OAuth2 user info JSON based
	 * on the mappings defined in the OAuth2 properties file.
	 * 
	 * @return The OpenMRS username.
	 */
	public String getUsername() {
		return get(PROP_USERNAME);
	}
	
	/**
	 * Convenience method to get a minimum viable OpenMRS {@link User} from the OAuth2 user info
	 * JSON based on the mappings defined in the OAuth2 properties file.
	 * 
	 * @return A minimum viable OpenMRS {@link User}.
	 */
	public User getOpenmrsUser() {
		
		User user = new User();
		user.setUsername(getUsername());
		user.setSystemId(get(PROP_SYSTEMID));
		user.setEmail(get(PROP_EMAIL, ""));
		
		Person person = new Person();
		person.setGender(get(PROP_GENDER, "n/a"));
		PersonName name = new PersonName();
		name.setGivenName(get(PROP_GIVEN_NAME));
		name.setMiddleName(get(PROP_MIDDLE_NAME, ""));
		name.setFamilyName(get(PROP_FAMILY_NAME));
		
		user.setPerson(person);
		user.addName(name);
		
		return user;
	}
	
	/**
	 * Convenience method that retrieves the list of role names from the OAuth2 user info JSON based
	 * on the mappings defined in the OAuth2 properties file.
	 * 
	 * @return The list of role names, eg. ["Nurse", "Anaesthesia Assistant"].
	 */
	public List<String> getRoleNames() {
		String roleNames = get(PROP_ROLES, "");
		return Stream.of(roleNames.split(","))
				.filter(n -> StringUtils.isNoneBlank(n))
				.map(n -> StringUtils.trim(n))
				.collect(Collectors.toList());
	}
}
