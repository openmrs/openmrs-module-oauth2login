/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.oauth2login.web.filter;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.oauth2login.authscheme.OAuth2TokenCredentials;
import org.openmrs.module.oauth2login.authscheme.UserInfo;
import org.openmrs.module.oauth2login.web.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;

/**
 * Filter for authenticating oauth2 service accounts
 */
public class OAuth2ServiceAccountFilter implements Filter {
	
	protected final Logger log = LoggerFactory.getLogger(OAuth2ServiceAccountFilter.class);
	
	public static final String GP_KEY = "oauth2login.jws.verification.key";
	
	public static final String HEADER_BEARER = "Bearer";
	
	public static final String HEADER_X_JWT_ASSERT = "X-JWT-Assertion";
	
	/**
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		log.debug("Initializing oauth2 service account login filter");
	}
	
	/**
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy() {
		log.debug("Destroying oauth2 service account login filter");
	}
	
	/**
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
	 *      javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
	    throws IOException, ServletException {
		
		if (request instanceof HttpServletRequest) {
			//TODO should we limit this authentication mechanism to webservice calls only?
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			String headerValue = httpRequest.getHeader("Authorization");
			if (StringUtils.isNotBlank(headerValue)) {
				if (headerValue.startsWith(HEADER_BEARER) || headerValue.startsWith(HEADER_X_JWT_ASSERT)) {
					String schema = headerValue.startsWith(HEADER_BEARER) ? HEADER_BEARER : HEADER_X_JWT_ASSERT;
					String token = headerValue.substring(schema.length() + 1);
					String[] parts = token.split("\\.");
					//Ignore if this is not a JWT token
					if (parts.length == 3) {
						String publicKey = Context.getAdministrationService().getGlobalProperty(GP_KEY);
						try {
							Claims claims = JwtTokenUtils.parseAndVerifyToken(token, publicKey.trim());
							Properties props = Context.getRegisteredComponent("oauth2.properties", Properties.class);
							String username = claims.get(props.getProperty(UserInfo.PROP_USERNAME), String.class);
							String userInfoJson = "{\"preferred_username\":\"" + username + "\"}";
							Context.authenticate(new OAuth2TokenCredentials(new UserInfo(props, userInfoJson), true));
						}
						catch (Throwable e) {
							//Ignore and let the API take care of authentication issues
							log.warn("Failed to authenticate user using oauth token", e);
						}
					}
				}
			}
		}
		
		chain.doFilter(request, response);
	}
}
