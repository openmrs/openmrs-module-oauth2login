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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.User;

import com.jayway.jsonpath.JsonPath;

public class OAuth2User {
	
	public final static String MAPPINGS_PFX = "openmrs.mapping.";
	
	public final static String PROP_USERNAME = "user.username";
	
	public final static String PROP_EMAIL = "user.email";
	
	public final static String PROP_GIVEN_NAME = "person.givenName";
	
	public final static String PROP_MIDDLE_NAME = "person.middleName";
	
	public final static String PROP_FAMILY_NAME = "person.familyName";
	
	public final static String PROP_GENDER = "person.gender";
	
	public final static String PROP_SYSTEMID = "user.systemId";
	
	public static final String PROP_ROLES = "user.roles";
	
	private String username;
	
	private String userInfoJson; // user info as obtained from the OAuth 2 provider
	
	public OAuth2User(String username, String userInfoJson) {
		this.username = username;
		this.userInfoJson = userInfoJson;
	}
	
	@Override
	public String toString() {
		return getUsername();
	}
	
	public String getUsername() {
		return username;
	}
	
	/**
	 * Converts the OAUth user info into an OpenMRS user based on the OAuth 2 properties mappings.
	 * 
	 * @param props The mappings between the user info fields and the corresponding OpenMRS
	 *            user/person properties.
	 * @return The OpenMRS {@link User}
	 */
	public User toOpenmrsUser(Properties props) {
		
		User user = new User();
		user.setUsername(getUsername());
		user.setSystemId(get(userInfoJson, MAPPINGS_PFX + PROP_SYSTEMID, props));
		user.setEmail(get(userInfoJson, MAPPINGS_PFX + PROP_EMAIL, props, null));
		
		Person person = new Person();
		person.setGender(get(userInfoJson, MAPPINGS_PFX + PROP_GENDER, props, "n/a"));
		PersonName name = new PersonName();
		name.setGivenName(get(userInfoJson, MAPPINGS_PFX + PROP_GIVEN_NAME, props));
		name.setMiddleName(get(userInfoJson, MAPPINGS_PFX + PROP_MIDDLE_NAME, props));
		name.setFamilyName(get(userInfoJson, MAPPINGS_PFX + PROP_FAMILY_NAME, props));
		
		user.setPerson(person);
		user.addName(name);
		
		return user;
	}
	
	/**
	 * Extracts values from the user info JSON based on a one to one mapping provided via a
	 * properties file.
	 * 
	 * @param userInfoJson The OAuth 2 user info response JSON.
	 * @param propertyKey The property key to look for, eg "openmrs.mapping.user.username".
	 * @param props The mapping between properties keys and their mapping.
	 * @param defaultValue The value to default to if no property could be found.
	 * @return The corresponding value from the JSON, an empty String if none is found.
	 */
	public static String get(String userInfoJson, String propertyKey, Properties props, String defaultValue) {
		String propertyValue = props.getProperty(propertyKey, null);
		String res = defaultValue;
		if (!StringUtils.isEmpty(propertyValue)) {
			res = JsonPath.read(userInfoJson, "$." + propertyValue);
		}
		return res;
	}
	
	/**
	 * Return a roles list based on the OAuth 2 properties mappings.
	 * 
	 * @param props The mappings between the user info fields and the corresponding OpenMRS
	 *            user/person properties.
	 * @return The list of roles
	 */
	public List<String> getRoles(Properties props) {
		String rolesName = get(userInfoJson, MAPPINGS_PFX + PROP_ROLES, props,
				"");



		return Stream.of(rolesName.split(","))
				.filter(elem -> StringUtils.isNoneBlank(elem))
				.map(elem -> new String(elem))
				.collect(Collectors.toList());
	}
	
	/**
	 * @see #get(String, String, Properties, String)
	 */
	public static String get(String userInfoJson, String propertyKey, Properties props) {
		return get(userInfoJson, propertyKey, props, "");
	}
	
}
