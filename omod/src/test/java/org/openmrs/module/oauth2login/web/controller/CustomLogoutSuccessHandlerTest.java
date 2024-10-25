package org.openmrs.module.oauth2login.web.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.oauth2login.OAuth2LoginConstants;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.RedirectStrategy;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class CustomLogoutSuccessHandlerTest {
	
	public CustomLogoutSuccessHandlerTest() {
		OAuth2IntegrationTest.initPathInSystemProperties("Keycloak");
	}
	
	@Test
	public void onLogoutSuccess_redirectToLogoutURL() throws IOException, ServletException {
		final String idToken = "myToken";
		//setup
		PowerMockito.mockStatic(Context.class);
		User user = new User();
		user.setUserProperty(OAuth2LoginConstants.USER_PROP_ID_TOKEN, idToken);
		Mockito.when(Context.getAuthenticatedUser()).thenReturn(user);
		
		CustomLogoutSuccessHandler customLogoutSuccessHandler = new CustomLogoutSuccessHandler();
		RedirectStrategy redirectStrategy = mock(RedirectStrategy.class);
		customLogoutSuccessHandler.setRedirectStrategy(redirectStrategy);
		MockHttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = mock(HttpServletResponse.class);
		//replay
		customLogoutSuccessHandler.onLogoutSuccess(request, response, null);
		
		//verify
		PowerMockito.verifyStatic(times(1));
		Context.logout();
		
		verify(redirectStrategy, times(1)).sendRedirect(request, response,
		    "http://localhost:8081/auth/realms/demo/protocol/openid-connect/logout?id_token_hint=" + idToken);
		
	}
}
