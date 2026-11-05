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
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Authenticated;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.api.context.Credentials;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.TestDaemonToken;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
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
	
	private OAuth2TokenCredentials credentials;
	
	@Before
	public void setup() {
		Context.setDAO(contextDAO);
		// a valid token, so that the user update can run in a daemon thread
		new TestDaemonToken().setDaemonToken(authScheme);
		credentials = mock(OAuth2TokenCredentials.class);
		when(credentials.getClientName()).thenReturn("tester");
		when(credentials.getUserInfo()).thenReturn(userInfo);
		when(credentials.getAuthenticationScheme()).thenReturn("oauth2");
	}
	
	@Test
	public void authenticate_shouldCreateNewUserWhenUserDoesNotExist() throws Exception {
		when(contextDAO.getUserByUsername("tester")).thenReturn(null);
		User newUser = new User();
		when(userInfo.getOpenmrsUser(anyString())).thenReturn(newUser);
		when(userInfo.getRoleNames()).thenReturn(Arrays.asList("Provider", "Nurse"));
		
		Authenticated result = authScheme.authenticate(credentials);
		
		verify(contextDAO).createUser(eq(newUser), anyString(), eq(Arrays.asList("Provider", "Nurse")));
		verify(userService, never()).saveUser(any(User.class));
		assertNotNull(result);
	}
	
	@Test
	public void authenticate_shouldApplyRoleChangesToTheUserOfThisSignIn() throws Exception {
		// the stored user has the role 'Nurse', the identity provider now says 'Midwife'
		Set<String> storedRoleNames = new HashSet<>(Collections.singletonList("Nurse"));
		when(userService.getUserByUsername("tester")).thenAnswer(invocation -> userWithRoles(storedRoleNames));
		when(contextDAO.getUserByUsername("tester")).thenAnswer(invocation -> userWithRoles(storedRoleNames));
		when(userService.getRole(anyString())).thenAnswer(invocation -> new Role((String) invocation.getArguments()[0]));
		when(userService.saveUser(any(User.class))).thenAnswer(invocation -> {
			User saved = (User) invocation.getArguments()[0];
			storedRoleNames.clear();
			storedRoleNames.addAll(saved.getRoles().stream().map(Role::getRole).collect(Collectors.toSet()));
			return saved;
		});
		when(userInfo.getUsername()).thenReturn("tester");
		when(userInfo.getOpenmrsUser()).thenReturn(new User());
		when(userInfo.getRoleNames()).thenReturn(Collections.singletonList("Midwife"));
		
		Authenticated result = authScheme.authenticate(credentials);
		
		Set<String> roleNames = result.getUser().getRoles().stream().map(Role::getRole).collect(Collectors.toSet());
		assertEquals(Collections.singleton("Midwife"), roleNames);
	}
	
	private static User userWithRoles(Set<String> roleNames) {
		User user = new User();
		user.setUsername("tester");
		user.setRoles(roleNames.stream().map(Role::new).collect(Collectors.toSet()));
		return user;
	}
	
	@Test(expected = ContextAuthenticationException.class)
	public void authenticate_shouldThrowExceptionForInvalidCredentials() {
		Credentials invalidCredentials = mock(Credentials.class);
		
		authScheme.authenticate(invalidCredentials);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void authenticate_shouldSkipUserCreationForServiceAccount() throws Exception {
		when(credentials.isServiceAccount()).thenReturn(true);
		when(contextDAO.getUserByUsername("tester")).thenReturn(new User());
		
		Authenticated result = authScheme.authenticate(credentials);
		
		assertNotNull(result);
		verify(userInfo, never()).getOpenmrsUser(anyString());
		verify(contextDAO, never()).createUser(any(User.class), anyString(), anyList());
	}
}
