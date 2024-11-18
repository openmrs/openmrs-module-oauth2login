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

import static org.openmrs.module.oauth2login.OAuth2LoginConstants.USER_PROP_ID_TOKEN;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

public class CustomLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler implements LogoutSuccessHandler {
	
	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
	        throws IOException, ServletException {
		Properties properties = OAuth2BeanFactory.getProperties(OAuth2BeanFactory.getOAuth2PropertiesPath());
		String redirectPath = properties.getProperty("logoutUri");
		//the redirect path can contain a [token] that should be replaced by the aut token
		if (StringUtils.isNoneBlank(redirectPath) && redirectPath.contains("[token]")) {
			String token = Context.getAuthenticatedUser().getUserProperty(USER_PROP_ID_TOKEN);
			String encoded = URLEncoder.encode(token, "UTF-8");
			redirectPath = StringUtils.replace(redirectPath, "[token]", encoded);
		}
		Context.logout();
		redirectPath = StringUtils.defaultIfBlank(redirectPath, request.getContextPath() + "/oauth2login");
		super.getRedirectStrategy().sendRedirect(request, response, redirectPath);
	}
	
}
