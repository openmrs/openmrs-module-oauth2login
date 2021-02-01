/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.oauth2login.web.controller;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Credentials;
import org.openmrs.module.oauth2login.authscheme.OAuth2TokenCredentials;
import org.openmrs.module.oauth2login.authscheme.OAuth2User;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestOperations;

public abstract class OAuth2IntegrationTest extends BaseModuleContextSensitiveTest {
	
	private final static String OPENMRS_APPLICATION_DATA_DIRECTORY = "OPENMRS_APPLICATION_DATA_DIRECTORY";
	
	/**
	 * @return The test app data dir name for the current integration test.
	 */
	protected abstract String getAppDataDirName();
	
	/**
	 * @return The user info JSON response received from the OAuth 2 resource server.
	 */
	protected abstract String getUserInfoJson();
	
	public OAuth2IntegrationTest() {
		super();
		initBaseModuleContext(getAppDataDirName());
		
	}
	
	protected static void initBaseModuleContext(String appDataDirName) {
		initPathInSystemProperties(appDataDirName);
		BaseModuleContextSensitiveTest.runtimeProperties.setProperty(
		    OpenmrsConstants.APPLICATION_DATA_DIRECTORY_RUNTIME_PROPERTY,
		    System.getProperty(OPENMRS_APPLICATION_DATA_DIRECTORY));
		Context.setRuntimeProperties(runtimeProperties);
	}
	
	public static void initPathInSystemProperties(String appDataDirName) {
		String path = normalizePath(OAuth2IntegrationTest.class.getClassLoader().getResource(appDataDirName).getPath());
		System.setProperty(OPENMRS_APPLICATION_DATA_DIRECTORY, path);
	}
	
	private static String normalizePath(String path) {
		//on windows we get /C:/.... so have to replace the first /
		return path.replaceFirst("^/(.:/)", "$1");
	}
	
	@Mock
	private RestOperations testRestTemplate;
	
	@Autowired
	protected OAuth2LoginController controller;
	
	@Autowired
	@Qualifier("oauth2.userInfoUri")
	private String userInfoUri;
	
	private Properties oauth2Props;
	
	@Override
	protected Credentials getCredentials() {
		
		try {
			oauth2Props = OAuth2BeanFactory.getProperties(Paths.get(System.getProperty(OPENMRS_APPLICATION_DATA_DIRECTORY),
			    "oauth2.properties"));
		}
		catch (IOException e) {
			e.printStackTrace();
			Assert.fail("The OAuth 2 properties could not be obtained for the authentication test: " + e.getMessage());
		}
		
		String username = OAuth2User.get(getUserInfoJson(), OAuth2User.MAPPINGS_PFX + OAuth2User.PROP_USERNAME, oauth2Props);
		return new OAuth2TokenCredentials(new OAuth2User(getUserInfoJson(), oauth2Props));
	}
	
	@Before
	public void setup() throws Exception {
		
		controller.setOAuth2Properties(oauth2Props);
		
		controller.setRestTemplate(testRestTemplate);
		when(testRestTemplate.getForObject(eq(new URI(userInfoUri)), eq(String.class))).thenReturn(getUserInfoJson());
		
		//		// Ideally we should do something like this:
		//		User u = Context.getAuthenticatedUser();
		//		Context.becomeUser("daemon");
		//		Context.getUserService().purgeUser(u);
		Context.logout();
	}
	
	protected abstract void assertAuthenticatedUser(User user);
	
	@Test
	public void assertOAuth2Authentication() throws Exception {
		// pre-verif
		Assert.assertFalse(Context.isAuthenticated());
		
		// replay
		controller.login();
		
		// verif
		User user = Context.getAuthenticatedUser();
		Assert.assertNotNull(user);
		assertAuthenticatedUser(user);
	}
}
