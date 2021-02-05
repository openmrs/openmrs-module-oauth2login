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
import org.openmrs.api.context.Authenticated;
import org.openmrs.api.context.BasicAuthenticated;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.api.context.Credentials;
import org.openmrs.api.context.DaoAuthenticationScheme;
import org.springframework.stereotype.Component;

/**
 * A scheme that authenticates with OpenMRS based on the 'username'.
 */
@Component
public class UsernameAuthenticationScheme extends DaoAuthenticationScheme {
	
	protected Log log = LogFactory.getLog(getClass());
	
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
		
		if (user == null) {
			
			try {
				user = getContextDAO().createUser(creds.getUserInfo().getOpenmrsUser(),
				    RandomStringUtils.random(100, true, true), creds.getUserInfo().getRoleNames());
			}
			catch (Exception e) {
				throw new ContextAuthenticationException(
				        "The credentials provided pointed to a user that did not exist yet in OpenMRS: '"
				                + credentials.getClientName() + "'. The user creation was attempted but has failed. ", e);
			}
		}
		
		return new BasicAuthenticated(user, credentials.getAuthenticationScheme());
	}
}
