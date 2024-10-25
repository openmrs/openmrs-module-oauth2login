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
package org.openmrs.module.oauth2login.web.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.oauth2login.OAuth2LoginConstants;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class })
public class OAuth2LoginControllerTest {
	
	@Mock
	private OAuth2RestOperations mockTemplate;
	
	@Mock
	private OAuth2AccessToken mockAccessToken;
	
	@Mock
	private ProviderService mockProviderService;
	
	@Mock
	private PersonService mockPersonService;
	
	@Mock
	private UserService mockUserService;
	
	@Mock
	private AdministrationService mockAdminService;
	
	private OAuth2LoginController controller;
	
	@Test
	public void login_shouldStoreTheIdTokenAsAUserProperty() {
		final String idToken = "myToken";
		final String userInfUri = "http://test/userinfo";
		PowerMockito.mockStatic(Context.class);
		Mockito.when(Context.isAuthenticated()).thenReturn(true);
		Map<String, Object> additionalInfo = new HashMap();
		additionalInfo.put("id_token", idToken);
		Mockito.when(mockAccessToken.getAdditionalInformation()).thenReturn(additionalInfo);
		Mockito.when(mockTemplate.getAccessToken()).thenReturn(mockAccessToken);
		User user = new User();
		user.setPerson(new Person());
		Mockito.when(Context.getAuthenticatedUser()).thenReturn(user);
		controller = new OAuth2LoginController();
		Properties oauth2Props = new Properties();
		Whitebox.setInternalState(controller, "oauth2Props", oauth2Props);
		Whitebox.setInternalState(controller, "userInfoUri", userInfUri);
		Whitebox.setInternalState(controller, "restTemplate", mockTemplate);
		Whitebox.setInternalState(controller, "ps", mockProviderService);
		Whitebox.setInternalState(controller, "personService", mockPersonService);
		Whitebox.setInternalState(controller, "userService", mockUserService);
		Mockito.when(Context.getAdministrationService()).thenReturn(mockAdminService);
		
		controller.login();
		
		Assert.assertEquals(idToken, user.getUserProperty(OAuth2LoginConstants.USER_PROP_ID_TOKEN));
	}
	
}
