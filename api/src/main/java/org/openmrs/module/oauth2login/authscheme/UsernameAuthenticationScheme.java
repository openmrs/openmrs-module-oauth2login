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

import java.util.Properties;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * A scheme that authenticates with OpenMRS based on the 'username'.
 */
@Component("usernameAuthenticationScheme")
public class UsernameAuthenticationScheme extends DaoAuthenticationScheme {
	
	protected Log log = LogFactory.getLog(getClass());
	
	private DaemonToken daemonToken;
	
	private Properties oauth2Props;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	public void setOAuth2Properties(@Qualifier("oauth2.properties") Properties oauth2Props) {
		this.oauth2Props = oauth2Props;
	}
	
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
			throw new ContextAuthenticationException(
			        "The credentials provided did not match those needed for the authentication scheme.", e);
		}
		
		User user = getContextDAO().getUserByUsername(credentials.getClientName());
		
		if (user != null) {
			updateUser(user);
		} else {
			createUser(creds.getOAuth2User(), credentials.getClientName());
		}
		return new BasicAuthenticated(user, credentials.getAuthenticationScheme());
	}
	
	private void updateUser(User user) {
		
		user.getPerson().getPersonName().setGivenName("Test");
		
		UpdateUserTask task = new UpdateUserTask(userService, user);
		Daemon.runInDaemonThreadAndWait(task, daemonToken);
	}
	
	private void createUser(OAuth2User oAuth2User, String clientName) {
		User user = oAuth2User.toOpenmrsUser(oauth2Props);
		
		try {
			user = getContextDAO().createUser(user, RandomStringUtils.random(100, true, true),
			    oAuth2User.getRoleNames(oauth2Props));
		}
		catch (Exception e) {
			throw new ContextAuthenticationException(
			        "The credentials provided pointed to a user that did not exist yet in OpenMRS: '" + clientName
			                + "'. The user creation was attempted but has failed. ", e);
		}
	}
	
}
