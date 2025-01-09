/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.oauth2login.authscheme;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Authenticated;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.api.context.Credentials;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.DaemonToken;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OAuth2UserInfoAuthenticationSchemeTest {
	
	@InjectMocks
	private OAuth2UserInfoAuthenticationScheme authScheme;
	
	@Mock
	private ContextDAO contextDAO;
	
	@Mock
	private UserService userService;
	
	@Mock
	private UserInfo userInfo;
	
	@Mock
	private DaemonToken daemonToken;
	
	private OAuth2TokenCredentials credentials;
	
	@Before
	public void setup() {
		Context.setDAO(contextDAO);
		authScheme.setDaemonToken(daemonToken);
		credentials = mock(OAuth2TokenCredentials.class);
		when(credentials.getClientName()).thenReturn("tester");
		when(credentials.getUserInfo()).thenReturn(userInfo);
		when(credentials.getAuthenticationScheme()).thenReturn("oauth2");
		when(daemonToken.getId()).thenReturn("token");
	}
	
	@Test
	public void authenticate_shouldCreateNewUserWhenUserDoesNotExist() throws Exception {
		// Given
		when(contextDAO.getUserByUsername("tester")).thenReturn(null);
		User newUser = new User();
		when(userInfo.getOpenmrsUser(anyString())).thenReturn(newUser);
		when(userInfo.getRoleNames()).thenReturn(Arrays.asList("Provider", "Nurse"));
		
		// When
		Authenticated result = authScheme.authenticate(credentials);
		
		// Then
		verify(contextDAO).createUser(eq(newUser), anyString(), eq(Arrays.asList("Provider", "Nurse")));
		assertNotNull(result);
	}
	
	@Test(expected = ContextAuthenticationException.class)
	public void authenticate_shouldThrowExceptionForInvalidCredentials() {
		Credentials invalidCredentials = mock(Credentials.class);
		
		authScheme.authenticate(invalidCredentials);
	}
	
	@Test
	public void authenticate_shouldSkipUserCreationForServiceAccount() throws Exception {
		when(credentials.isServiceAccount()).thenReturn(true);
		User existingUser = new User();
		when(contextDAO.getUserByUsername("tester")).thenReturn(existingUser);
		
		Authenticated result = authScheme.authenticate(credentials);
		
		assertNotNull(result);
		verify(userInfo, never()).getOpenmrsUser(anyString());
		verify(contextDAO, never()).createUser(any(User.class), anyString(), anyList());
	}
}
