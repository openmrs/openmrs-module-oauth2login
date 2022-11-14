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

import static org.openmrs.module.oauth2login.OAuth2LoginConstants.AUTH_SCHEME_COMPONENT;

import java.util.Collection;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Authenticated;
import org.openmrs.api.context.BasicAuthenticated;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.api.context.Credentials;
import org.openmrs.api.context.Daemon;
import org.openmrs.api.context.DaoAuthenticationScheme;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A scheme that authenticates with OpenMRS based on the 'username'.
 */
@Transactional
@Component(AUTH_SCHEME_COMPONENT)
public class OAuth2UserInfoAuthenticationScheme extends DaoAuthenticationScheme implements DaemonTokenAware {
	
	protected Log log = LogFactory.getLog(getClass());
	
	private DaemonToken daemonToken;
	
	private AuthenticationPostProcessor postProcessor;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	@Qualifier("providerService")
	ProviderService ps;
	
	public void setDaemonToken(DaemonToken daemonToken) {
		this.daemonToken = daemonToken;
	}
	
	public void setPostProcessor(AuthenticationPostProcessor postProcessor) {
		this.postProcessor = postProcessor;
	}
	
	public OAuth2UserInfoAuthenticationScheme() {
		setPostProcessor(new AuthenticationPostProcessor() {
			
			@Override
			public void process(UserInfo userInfo) {
				// no post-processing by default
			}
		});
	}
	
	@Override
	public Authenticated authenticate(Credentials credentials) throws ContextAuthenticationException {
		
		OAuth2TokenCredentials creds;
		try {
			creds = (OAuth2TokenCredentials) credentials;
		}
		catch (ClassCastException e) {
			throw new ContextAuthenticationException("The credentials provided did not match those needed for the "
			        + getClass().getSimpleName() + " authentication scheme.", e);
		}
		
		User user = getContextDAO().getUserByUsername(credentials.getClientName());
		if (!creds.isServiceAccount()) {
			if (user == null) {
				createUser(creds.getUserInfo());
			} else {
				updateUser(user, creds.getUserInfo());
			}
			
			postProcessor.process(creds.getUserInfo());
		}
		
		return new BasicAuthenticated(user, credentials.getAuthenticationScheme());
	}
	
	private void createUser(UserInfo userInfo) throws ContextAuthenticationException {
		User user = null;
		try {
			user = getContextDAO().createUser(userInfo.getOpenmrsUser("n/a"), RandomStringUtils.random(100, true, true),
			    userInfo.getRoleNames());
			if ("true".equalsIgnoreCase(userInfo.getString(UserInfo.PROP_PROVIDER, "true"))) {
				associateProviderAccountForUser(user);
			} else {
				disAssociateProviderAccountForUser(user);
			}
		}
		catch (Exception e) {
			throw new ContextAuthenticationException(e.getMessage(), e);
		}
	}
	
	private void updateUser(User user, UserInfo userInfo) {
		try {
			UpdateUserTask task = new UpdateUserTask(userService, userInfo);
			Daemon.runInDaemonThread(task, daemonToken);
			if ("true".equalsIgnoreCase(userInfo.getString(UserInfo.PROP_PROVIDER, "true"))) {
				associateProviderAccountForUser(user);
			}
		}
		catch (Exception e) {
			throw new ContextAuthenticationException(e.getMessage(), e);
		}
	}
	
	private void associateProviderAccountForUser(User user) {
		try {
			Context.addProxyPrivilege(PrivilegeConstants.GET_PROVIDERS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PERSONS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_USERS);
			Context.addProxyPrivilege(PrivilegeConstants.MANAGE_PROVIDERS);
			
			Person person = Context.getPersonService().getPerson(user.getPerson().getId());
			Collection<Provider> possibleProvider = ps.getProvidersByPerson(user.getPerson());
			if (possibleProvider.size() == 0) {
				Provider provider = new Provider();
				provider.setIdentifier(user.getSystemId());
				provider.setPerson(person);
				provider.setCreator(userService.getUserByUsername("daemon"));
				ps.saveProvider(provider);
			} else {
				for (Provider pd : possibleProvider) {
					if (pd.getRetired()) {
						ps.unretireProvider(pd);
					}
				}
			}
		}
		catch (Exception e) {
			log.error("Could not create provider account associated with user '" + user.getDisplayString(), e);
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_PROVIDERS);
			Context.removeProxyPrivilege(PrivilegeConstants.GET_PERSONS);
			Context.removeProxyPrivilege(PrivilegeConstants.GET_USERS);
			Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_PROVIDERS);
		}
	}
	
	private void disAssociateProviderAccountForUser(User user) {
		try {
			Context.addProxyPrivilege(PrivilegeConstants.GET_PROVIDERS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PERSONS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_USERS);
			Context.addProxyPrivilege(PrivilegeConstants.MANAGE_PROVIDERS);
			
			Collection<Provider> possibleProvider = ps.getProvidersByPerson(user.getPerson());
			for (Provider provider : possibleProvider) {
				ps.retireProvider(provider, "Disabling provider account by oaut2login");
			}
		}
		catch (Exception e) {
			log.error("Could not purge provider account associated with user '" + user.getDisplayString(), e);
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_PROVIDERS);
			Context.removeProxyPrivilege(PrivilegeConstants.GET_PERSONS);
			Context.removeProxyPrivilege(PrivilegeConstants.GET_USERS);
			Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_PROVIDERS);
		}
	}
}
