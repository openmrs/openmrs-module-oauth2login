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

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Authenticated;
import org.openmrs.api.context.BasicAuthenticated;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.api.context.Credentials;
import org.openmrs.api.context.Daemon;
import org.openmrs.api.context.DaoAuthenticationScheme;
import org.openmrs.module.DaemonToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A scheme that authenticates with OpenMRS based on the 'username'.
 */
@Component("oauth2login.usernameAuthenticationScheme")
public class UsernameAuthenticationScheme extends DaoAuthenticationScheme {
	
	protected Log log = LogFactory.getLog(getClass());
	
	private DaemonToken daemonToken;
	
	@Autowired
	private UserService userService;
	
	public void setDaemonToken(DaemonToken daemonToken) {
		this.daemonToken = daemonToken;
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
		if (user == null) {
			createUser(creds.getUserInfo());
		} else {
			updateUser(user, creds.getUserInfo());
		}
		
		return new BasicAuthenticated(user, credentials.getAuthenticationScheme());
	}
	
	private void createUser(UserInfo userInfo) throws ContextAuthenticationException {
		try {
			getContextDAO().createUser(userInfo.getOpenmrsUser(), RandomStringUtils.random(100, true, true),
			    userInfo.getRoleNames());
		}
		catch (Exception e) {
			throw new ContextAuthenticationException(e.getMessage(), e);
		}
	}
	
	private void updateUser(User user, UserInfo userInfo) {
		Daemon.runInDaemonThread(new UpdateUserTask(userService, userInfo), daemonToken);
	}
}
