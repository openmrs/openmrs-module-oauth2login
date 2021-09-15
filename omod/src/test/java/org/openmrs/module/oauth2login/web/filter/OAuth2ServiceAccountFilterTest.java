package org.openmrs.module.oauth2login.web.filter;

import static org.apache.commons.lang3.reflect.ConstructorUtils.getAccessibleConstructor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.module.oauth2login.OAuth2LoginConstants.OAUTH_PROP_BEAN_NAME;
import static org.openmrs.module.oauth2login.web.filter.OAuth2ServiceAccountFilter.HEADER_NAME_AUTH;
import static org.openmrs.module.oauth2login.web.filter.OAuth2ServiceAccountFilter.HEADER_NAME_X_JWT_ASSERT;
import static org.openmrs.module.oauth2login.web.filter.OAuth2ServiceAccountFilter.SCHEME_BEARER;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.api.context.Context;
import org.openmrs.module.oauth2login.authscheme.OAuth2TokenCredentials;
import org.openmrs.module.oauth2login.authscheme.UserInfo;
import org.openmrs.module.oauth2login.web.JwtUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import io.jsonwebtoken.Claims;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, JwtUtils.class, OAuth2ServiceAccountFilter.class })
public class OAuth2ServiceAccountFilterTest {
	
	@Mock
	private Properties mockProps;
	
	@Mock
	private HttpServletRequest mockRequest;
	
	@Mock
	private Claims mockClaims;
	
	@Mock
	private FilterChain mockFilterChain;
	
	@Mock
	private OAuth2TokenCredentials mockCredentials;
	
	@Mock
	private UserInfo mockUserInfo;
	
	@Mock
	private Logger mockLogger;
	
	private OAuth2ServiceAccountFilter filter;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		mockStatic(Context.class);
		mockStatic(JwtUtils.class);
		filter = PowerMockito.spy(new OAuth2ServiceAccountFilter());
		Whitebox.setInternalState(OAuth2ServiceAccountFilter.class, Logger.class, mockLogger);
	}
	
	@After
	public void tearDown() throws Exception {
		verify(mockFilterChain).doFilter(mockRequest, null);
	}
	
	@Test
	public void doFilter_shouldAuthenticateTheRequestWithATokenSpecifiedWithAuthHeaderAndBearerScheme() throws Exception {
		final String jwtToken = "header.payload.signature";
		when(mockRequest.getHeader(HEADER_NAME_AUTH)).thenReturn(SCHEME_BEARER + " " + jwtToken);
		when(Context.getRegisteredComponent(OAUTH_PROP_BEAN_NAME, Properties.class)).thenReturn(mockProps);
		when(JwtUtils.parseAndVerifyToken(jwtToken, mockProps)).thenReturn(mockClaims);
		final String propName = "testProperty";
		final String username = "testUsername";
		when(mockProps.getProperty(UserInfo.PROP_USERNAME)).thenReturn(propName);
		when(mockClaims.get(propName, String.class)).thenReturn(username);
		String userInfoJson = "{\"preferred_username\":\"" + username + "\"}";
		whenNew(getAccessibleConstructor(UserInfo.class, Properties.class, String.class)).withArguments(mockProps,
		    userInfoJson).thenReturn(mockUserInfo);
		whenNew(getAccessibleConstructor(OAuth2TokenCredentials.class, UserInfo.class, boolean.class)).withArguments(
		    mockUserInfo, true).thenReturn(mockCredentials);
		
		filter.doFilter(mockRequest, null, mockFilterChain);
		
		verifyStatic();
		Context.authenticate(mockCredentials);
	}
	
	@Test
	public void doFilter_shouldAuthenticateTheRequestWithATokenSpecifiedWithXJwtAssertHeader() throws Exception {
		final String jwtToken = "header.payload.signature";
		when(mockRequest.getHeader(HEADER_NAME_X_JWT_ASSERT)).thenReturn(jwtToken);
		when(Context.getRegisteredComponent(OAUTH_PROP_BEAN_NAME, Properties.class)).thenReturn(mockProps);
		when(JwtUtils.parseAndVerifyToken(jwtToken, mockProps)).thenReturn(mockClaims);
		final String propName = "testProperty";
		final String username = "testUsername";
		when(mockProps.getProperty(UserInfo.PROP_USERNAME)).thenReturn(propName);
		when(mockClaims.get(propName, String.class)).thenReturn(username);
		String userInfoJson = "{\"preferred_username\":\"" + username + "\"}";
		whenNew(getAccessibleConstructor(UserInfo.class, Properties.class, String.class)).withArguments(mockProps,
		    userInfoJson).thenReturn(mockUserInfo);
		whenNew(getAccessibleConstructor(OAuth2TokenCredentials.class, UserInfo.class, boolean.class)).withArguments(
		    mockUserInfo, true).thenReturn(mockCredentials);
		
		filter.doFilter(mockRequest, null, mockFilterChain);
		
		verifyStatic();
		Context.authenticate(mockCredentials);
	}
	
	@Test
	public void doFilter_shouldIgnoreTheRequestWithATokenThatIsNotAJwt() throws Exception {
		when(mockRequest.getHeader(HEADER_NAME_AUTH)).thenReturn(SCHEME_BEARER + " header.payload");
		when(mockLogger.isDebugEnabled()).thenReturn(true);
		
		filter.doFilter(mockRequest, null, mockFilterChain);
		
		verify(mockLogger).debug("Ignoring non JWT token");
	}
	
	@Test
	public void doFilter_shouldNotAuthenticateTheRequestWithAnInValidJwtToken() throws Exception {
		final String jwtToken = "header.payload.signature";
		when(mockRequest.getHeader(HEADER_NAME_AUTH)).thenReturn(SCHEME_BEARER + " " + jwtToken);
		when(Context.getRegisteredComponent(OAUTH_PROP_BEAN_NAME, Properties.class)).thenReturn(mockProps);
		Exception e = new Exception();
		when(JwtUtils.parseAndVerifyToken(jwtToken, mockProps)).thenThrow(e);
		
		filter.doFilter(mockRequest, null, mockFilterChain);
		
		verify(mockLogger).warn("Failed to authenticate user using oauth token", e);
	}
	
	@Test
	public void doFilter_shouldIgnoreTheRequestWithNoAuthHeader() throws Exception {
		when(mockLogger.isDebugEnabled()).thenReturn(true);
		
		filter.doFilter(mockRequest, null, mockFilterChain);
		
		verify(mockLogger).debug("No oauth token specified via supported header names");
	}
	
}
