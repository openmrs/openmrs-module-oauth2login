/*
 * Copyright (C) Amiyul LLC - All Rights Reserved
 *
 * This source code is protected under international copyright law. All rights
 * reserved and protected by the copyright holder.
 *
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holder. If you encounter this file and do not have
 * permission, please contact the copyright holder and delete this file.
 */
package org.openmrs.module.oauth2login.web;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.oauth2login.OAuth2LoginConstants;
import org.openmrs.module.oauth2login.web.controller.OAuth2IntegrationTest;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class UtilsTest {
	
	private static final String BASE_URL = "http://localhost:8081/auth/realms/demo/protocol/openid-connect/logout?";
	
	@BeforeClass
	public static void setUp() {
		OAuth2IntegrationTest.initPathInSystemProperties("Keycloak");
	}
	
	@Test
	public void onLogoutSuccess_redirectToLogoutURL() throws Exception {
		final String idToken = "myToken";
		PowerMockito.mockStatic(Context.class);
		User user = new User();
		user.setUserProperty(OAuth2LoginConstants.USER_PROP_ID_TOKEN, idToken);
		Mockito.when(Context.getAuthenticatedUser()).thenReturn(user);
		MockHttpServletRequest request = new MockHttpServletRequest();
		
		String redirect = Utils.getPostLogoutRedirectUrl(request);
		
		assertEquals(redirect, BASE_URL + "id_token_hint=" + idToken);
	}
	
	@Test
	public void onLogoutSuccess_shouldNotSetIdTokenIfUserIsNotAuthenticated() throws Exception {
		PowerMockito.mockStatic(Context.class);
		MockHttpServletRequest request = new MockHttpServletRequest();
		
		String redirect = Utils.getPostLogoutRedirectUrl(request);
		
		assertEquals(redirect, BASE_URL + "client_id=openmrs");
	}
	
	@Test
	public void onLogoutSuccess_shouldNotSetIdTokenIfNoneIsSetForTheUser() throws Exception {
		PowerMockito.mockStatic(Context.class);
		Mockito.when(Context.getAuthenticatedUser()).thenReturn(new User());
		MockHttpServletRequest request = new MockHttpServletRequest();
		
		String redirect = Utils.getPostLogoutRedirectUrl(request);
		
		assertEquals(redirect, BASE_URL + "client_id=openmrs");
	}
	
}
