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
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.api.APIException;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Field;

@RunWith(MockitoJUnitRunner.class)
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
		// Use safe constructor arguments instead of null to avoid potential NPEs
		UserRedirectRequiredException ex = new UserRedirectRequiredException("dummyUri", Collections.emptyMap());
		Assert.assertNull(resolver.resolveException(mockRequest, mockResp, mockHandler, ex));
	}
	
	@Test
	public void resolveException_shouldDelegateToSuperClassForOtherExceptionsTypes() throws Exception {
		final String viewName = "testErrorView";
		APIException ex = new APIException();
		
		Set<Object> mappedHandlers = new HashSet<>();
		mappedHandlers.add(mockHandler);
		
		// Replace PowerMock's Whitebox.setInternalState with standard Java reflection
		Field field = resolver.getClass().getSuperclass().getDeclaredField("mappedHandlers");
		field.setAccessible(true);
		field.set(resolver, mappedHandlers);
		
		Properties props = new Properties();
		props.put(APIException.class.getName(), viewName);
		resolver.setExceptionMappings(props);
		
		ModelAndView mav = resolver.resolveException(mockRequest, mockResp, mockHandler, ex);
		
		Assert.assertNotNull(mav);
		Assert.assertEquals(viewName, mav.getViewName());
	}
}
