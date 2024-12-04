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

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.APIException;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.web.servlet.ModelAndView;

@RunWith(PowerMockRunner.class)
public class Oauth2ExceptionResolverTest {
	
	@Mock
	private HttpServletRequest mockRequest;
	
	@Mock
	private HttpServletResponse mockResp;
	
	@Mock
	private Object mockHandler;
	
	private Oauth2ExceptionResolver resolver = new Oauth2ExceptionResolver();
	
	@Test
	public void resolveException_shouldReturnNullForUserRedirectRequiredException() {
		UserRedirectRequiredException ex = new UserRedirectRequiredException(null, null);
		Assert.assertNull(resolver.resolveException(mockRequest, mockResp, mockHandler, ex));
	}
	
	@Test
	public void resolveException_shouldDelegateToSuperClassForOtherExceptionsTypes() {
		final String viewName = "testErrorView";
		APIException ex = new APIException();
		Set<Object> mappedHandlers = new HashSet<>();
		mappedHandlers.add(mockHandler);
		Whitebox.setInternalState(resolver, "mappedHandlers", mappedHandlers);
		Properties props = new Properties();
		props.put(APIException.class.getName(), viewName);
		resolver.setExceptionMappings(props);
		
		ModelAndView mav = resolver.resolveException(mockRequest, mockResp, mockHandler, ex);
		
		Assert.assertEquals(viewName, mav.getViewName());
	}
}
