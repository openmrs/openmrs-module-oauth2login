/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.oauth2login.web.filter;

import org.openmrs.api.context.Context;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
		
		if (!moduleURIs.contains(path)) {
			
			// Logout (forwarding)
			// Logout (then redirect to logout url)
			//"manual-logout": should be a constant from org.openmrs.module.appui.AppUiConstants
			if (isLogoutRequest(path, httpRequest)) {
				httpResponse.sendRedirect(httpRequest.getContextPath() + "/oauth2logout");
				return;
			}
			
			// Login
			if (!Context.isAuthenticated()) {
				// non-authenticated requests are forwarded to the module login controller
				httpResponse.sendRedirect(httpRequest.getContextPath() + "/oauth2login");
				return;
			}
		}
		
		chain.doFilter(httpRequest, httpResponse);
	}
	
	private boolean isLogoutRequest(String path, HttpServletRequest httpServletRequest) {
		//"manual-logout": should be a constant from org.openmrs.module.appui.AppUiConstants
		//in OpenMRS the path is .../.../logout.action : should we use this .
		return path.equalsIgnoreCase("/logout")
		//				|| path.equalsIgnoreCase("/oauth2logout")
		        || (httpServletRequest.getSession() != null && "true".equals(httpServletRequest.getSession().getAttribute(
		            "manual-logout")));
	}
	
}
