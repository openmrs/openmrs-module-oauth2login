/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.oauth2login.web.controller;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.util.PrivilegeConstants;

public class AnonymousAuthenticationWithoutProviderAccountCreatedTest extends OAuth2IntegrationTest {
	
	@Override
	protected String getAppDataDirName() {
		return "anonymous";
	}
	
	@Override
	protected String getUserInfoJson() {
		return "{\n" + "  \"sub\": \"4e3074d6-5e9f-4707-84f1-ccb2aa2ab3bc\",\n" + "  \"email_verified\": true,\n"
		        + "  \"name\": \"Tommy Atkins\",\n" + "  \"preferred_username\": \"tatkins\",\n"
		        + "  \"given_name\": \"Tommy\",\n" + "  \"family_name\": \"Atkins\",\n"
		        + "  \"email\": \"tatkins@example.com\",\n" + "  \"roles\": [\"Provider\", \"B0gùs Rol3 N@m3\"]\n" + ", \"provider\": \"false\"}";
	}
	
	@Override
	protected void assertAuthenticatedUser(User user) {
		Assert.assertEquals("tatkins", user.getUsername());
		Assert.assertEquals("4e3074d6-5e9f-4707-84f1-ccb2aa2ab3bc", user.getSystemId());
		Assert.assertEquals("Tommy", user.getGivenName());
		Assert.assertEquals("Atkins", user.getFamilyName());
		Assert.assertEquals("tatkins@example.com", user.getEmail());
	}
	
	@Test
	@Override
	public void assertOAuth2Authentication() throws Exception {
		// pre-verif
		Assert.assertFalse(Context.isAuthenticated());
		
		// replay
		controller.login();
		
		// verif
		User user = Context.getAuthenticatedUser();
		Assert.assertNotNull(user);
		
		Context.addProxyPrivilege(PrivilegeConstants.GET_PROVIDERS);
		Collection<Provider> possibleProvider = Context.getProviderService().getProvidersByPerson(user.getPerson());
		Assert.assertThat(possibleProvider, hasSize(0));
		Context.removeProxyPrivilege(PrivilegeConstants.GET_PROVIDERS);
		
		assertAuthenticatedUser(user);
		Set<String> roleNames = user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet());

		Assert.assertThat(roleNames, hasSize(CollectionUtils.size(roleNamesToAssert())));
        Assert.assertThat(roleNames, containsInAnyOrder(roleNamesToAssert()));
	}
	
	@Test
	public void assertOAuth2AuthenticationAndDeactivateProviderGivenProviderSetToFalseAccount() throws Exception {
		Context.addProxyPrivilege(PrivilegeConstants.GET_USERS);
		Context.addProxyPrivilege(PrivilegeConstants.MANAGE_PROVIDERS);
		
		User usr = Context.getUserService().getUserByUsername("tatkins");
		
		Provider provider = new Provider();
		provider.setPerson(usr.getPerson());
		provider.setIdentifier(usr.getSystemId());
		provider.setCreator(Context.getUserService().getUserByUsername("daemon"));
		Context.getProviderService().saveProvider(provider);
		
		Context.removeProxyPrivilege(PrivilegeConstants.GET_USERS);
		Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_PROVIDERS);
		
		// pre-verif
		Assert.assertFalse(Context.isAuthenticated());
		Context.addProxyPrivilege(PrivilegeConstants.GET_PROVIDERS);
		Collection<Provider> existingProviderAccounts = Context.getProviderService().getProvidersByPerson(usr.getPerson(), false);
		Assert.assertThat(existingProviderAccounts, hasSize(1));
		Context.removeProxyPrivilege(PrivilegeConstants.GET_PROVIDERS);
		
		// replay
		controller.login();
		
		// verif
		User user = Context.getAuthenticatedUser();
		Assert.assertNotNull(user);
		
		Context.addProxyPrivilege(PrivilegeConstants.GET_PROVIDERS);
		Collection<Provider> possibleProvider = Context.getProviderService().getProvidersByPerson(user.getPerson(), false);
		Assert.assertThat(possibleProvider, hasSize(0));
		Context.removeProxyPrivilege(PrivilegeConstants.GET_PROVIDERS);
		
		assertAuthenticatedUser(user);
		Set<String> roleNames = user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet());

		Assert.assertThat(roleNames, hasSize(CollectionUtils.size(roleNamesToAssert())));
        Assert.assertThat(roleNames, containsInAnyOrder(roleNamesToAssert()));
	}
	
	@Override
	protected String[] roleNamesToAssert() {
		return new String[] { "Provider" };
	}
	
}
