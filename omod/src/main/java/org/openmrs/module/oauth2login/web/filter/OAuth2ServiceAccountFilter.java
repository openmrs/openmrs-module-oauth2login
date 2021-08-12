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

import static org.openmrs.module.oauth2login.OAuth2LoginConstants.OAUTH_PROP_BEAN_NAME;

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
import org.openmrs.module.oauth2login.web.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;

/**
 * Filter for authenticating oauth2 service accounts
 */
public class OAuth2ServiceAccountFilter implements Filter {
	
	private static final Logger log = LoggerFactory.getLogger(OAuth2ServiceAccountFilter.class);
	
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
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
	        ServletException {
		
		if (request instanceof HttpServletRequest) {
			//TODO should we limit this authentication mechanism to webservice calls only?
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			String headerValue = httpRequest.getHeader("Authorization");
			if (StringUtils.isNotBlank(headerValue)) {
				if (log.isDebugEnabled()) {
					log.debug("Found Authorization header on request");
				}
				
				if (headerValue.startsWith(HEADER_BEARER) || headerValue.startsWith(HEADER_X_JWT_ASSERT)) {
					String scheme = headerValue.startsWith(HEADER_BEARER) ? HEADER_BEARER : HEADER_X_JWT_ASSERT;
					String token = headerValue.substring(scheme.length() + 1);
					String[] parts = token.split("\\.");
					//Ignore if this is not a JWT token
					if (parts.length == 3) {
						try {
							Properties props = Context.getRegisteredComponent(OAUTH_PROP_BEAN_NAME, Properties.class);
							Claims claims = JwtUtils.parseAndVerifyToken(token, props);
							String username = claims.get(props.getProperty(UserInfo.PROP_USERNAME), String.class);
							String userInfoJson = "{\"preferred_username\":\"" + username + "\"}";
							Context.authenticate(new OAuth2TokenCredentials(new UserInfo(props, userInfoJson), true));
						}
						catch (Throwable e) {
							//Ignore and let the API take care of authentication issues
							log.warn("Failed to authenticate user using oauth token", e);
						}
					} else {
						if (log.isDebugEnabled()) {
							log.debug("Ignoring non JWT token");
						}
					}
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Found unsupported authentication scheme in authorization header value");
					}
				}
			}
		}
		
		chain.doFilter(request, response);
	}
}
