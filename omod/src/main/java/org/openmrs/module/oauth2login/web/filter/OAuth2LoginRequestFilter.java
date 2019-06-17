/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.oauth2login.web.filter;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openmrs.api.context.Context;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * This servlet filter ensures that the only way to authenticate is through the appropriate URI
 * "/oauth2login". It also disables the possibility to logout from OpenMRS since the logout process
 * is delegated to the OAuth 2 authentication provider.
 */
public class OAuth2LoginRequestFilter implements Filter {
	
	/**
	 * The list of URIs that should not be filtered because they are actually served by this module.
	 * In other words there are controllers within this module that implement their behaviour, and
	 * we need to let those controllers run, authenticated or not.
	 */
	private List<String> moduleURIs;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		String param = filterConfig.getInitParameter("moduleURIs");
		moduleURIs = Arrays.asList(param.split(","));
	}
	
	@Override
	public void destroy() {
	}
	
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
	        throws IOException, ServletException {
		
		HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
		HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
		
		String path = httpRequest.getServletPath();
		path = (path == null) ? "" : path;
		
		// Logout (forwarding)
		if (path.equalsIgnoreCase("/logout")) {
			httpResponse.sendRedirect(Paths.get(httpRequest.getContextPath(), "/oauth2logout").toString());
			return;
		}
		
		// Logout (then re-authentication)
		if (path.equalsIgnoreCase("/oauth2logout")) {
			
			Context.logout();
			
			logoutFromSpringSecurity(httpRequest, httpResponse);
			
			httpResponse.sendRedirect(Paths.get(httpRequest.getContextPath(), "/oauth2login").toString());
			return;
		}
		
		// Login
		if (!Context.isAuthenticated() && !moduleURIs.contains(path)) {
			// non-authenticated requests are forwarded to the module login controller
			httpResponse.sendRedirect(Paths.get(httpRequest.getContextPath(), "/oauth2login").toString());
			return;
		}
		
		chain.doFilter(httpRequest, httpResponse);
	}
	
	protected void logoutFromSpringSecurity(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
	        throws ServletException {
		HttpSession httpSession = httpRequest.getSession();
		if (httpSession != null) {
			SecurityContext securityContext = (SecurityContextImpl) httpSession.getAttribute("SPRING_SECURITY_CONTEXT");
			if (securityContext != null) {
				Authentication auth = securityContext.getAuthentication();
				
				if (auth.isAuthenticated()) {
					auth.setAuthenticated(false);
					new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, auth);
					SecurityContextHolder.clearContext();
					httpRequest.logout();
					httpRequest.getSession().invalidate();
				}
			}
		}
	}
}
