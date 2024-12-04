/*
 * Copyright (C) Amiyul LLC - All Rights Reserved
 *
 * This source code is protected under international copyright law. All rights
 * reserved and protected by the copyright holder.
 *
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holder. If you encounter this file and do not have
 * permission, please contact the copyright holder and delete this file.
 */
package org.openmrs.module.oauth2login.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

/**
 * This {@link org.springframework.web.servlet.HandlerExceptionResolver} implementation addresses an
 * issue where the user gets too many redirect errors in the browser.
 */
public class Oauth2ExceptionResolver extends SimpleMappingExceptionResolver {
	
	/**
	 * Ignore all instances of {@link UserRedirectRequiredException} which ensures that these
	 * exceptions propagate back to spring security's
	 * {@link org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter} which is
	 * responsible for redirecting unauthenticated users to the authentication provider's login
	 * endpoint.
	 * 
	 * @see SimpleMappingExceptionResolver#resolveException(HttpServletRequest, HttpServletResponse,
	 *      Object, Exception)
	 */
	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
	        Exception ex) {
		if (ex instanceof UserRedirectRequiredException) {
			//Tells spring framework to not handle the exception
			return null;
		}
		
		return super.resolveException(request, response, handler, ex);
	}
	
}
