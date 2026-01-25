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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmrs.api.APIException;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Oauth2ExceptionResolverTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private Object mockHandler;

    @InjectMocks
    private Oauth2ExceptionResolver resolver;

    @BeforeEach
    void setup() {
        resolver = new Oauth2ExceptionResolver();
    }

    @Test
    void shouldReturnNull_WhenUserRedirectRequiredException() {
        UserRedirectRequiredException ex = new UserRedirectRequiredException("dummyUri", Collections.emptyMap());
        assertNull(resolver.resolveException(mockRequest, mockResponse, mockHandler, ex));
    }

    @Test
    void shouldReturnMappedView_WhenOtherExceptionOccurs() throws Exception {
        final String viewName = "testErrorView";
        APIException ex = new APIException();

        // Simulate a handler being mapped
        Set<Object> mappedHandlers = new HashSet<>();
        mappedHandlers.add(mockHandler);

        // Use reflection instead of PowerMock
        var mappedHandlersField = resolver.getClass().getSuperclass().getDeclaredField("mappedHandlers");
        mappedHandlersField.setAccessible(true);
        mappedHandlersField.set(resolver, mappedHandlers);

        Properties props = new Properties();
        props.put(APIException.class.getName(), viewName);
        resolver.setExceptionMappings(props);

        ModelAndView mav = resolver.resolveException(mockRequest, mockResponse, mockHandler, ex);

        assertNotNull(mav);
        assertEquals(viewName, mav.getViewName());
    }
}
