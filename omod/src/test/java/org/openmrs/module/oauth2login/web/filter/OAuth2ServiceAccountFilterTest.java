package org.openmrs.module.oauth2login.web.filter;

import static org.apache.commons.lang3.reflect.ConstructorUtils.getAccessibleConstructor;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.openmrs.module.oauth2login.OAuth2LoginConstants.OAUTH_PROP_BEAN_NAME;
import static org.openmrs.module.oauth2login.web.filter.OAuth2ServiceAccountFilter.HEADER_NAME_AUTH;
import static org.openmrs.module.oauth2login.web.filter.OAuth2ServiceAccountFilter.HEADER_NAME_X_JWT_ASSERT;
import static org.openmrs.module.oauth2login.web.filter.OAuth2ServiceAccountFilter.SCHEME_BEARER;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.Collections;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;

import org.jose4j.json.JsonUtil;
import org.junit.After;
import org.junit.Assert;
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
import io.jsonwebtoken.impl.DefaultClaims;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, JwtUtils.class, OAuth2ServiceAccountFilter.class })
public class OAuth2ServiceAccountFilterTest {
	
	@Mock
	private Properties mockProps;
	
	@Mock
	private HttpServletRequest mockRequest;
	
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
		final String propName = "testProperty";
		final String username = "testUsername";
		Claims testClaims = new DefaultClaims(Collections.singletonMap(propName, username));
		when(JwtUtils.parseAndVerifyToken(jwtToken, mockProps)).thenReturn(testClaims);
		when(mockProps.getProperty(UserInfo.PROP_USERNAME)).thenReturn(propName);
		whenNew(getAccessibleConstructor(UserInfo.class, Properties.class, String.class)).withArguments(mockProps,
		    JsonUtil.toJson(testClaims)).thenReturn(mockUserInfo);
		whenNew(getAccessibleConstructor(OAuth2TokenCredentials.class, UserInfo.class, boolean.class)).withArguments(
		    mockUserInfo, true).thenReturn(mockCredentials);
		
		filter.doFilter(mockRequest, null, mockFilterChain);
		//one call is made to check if the property PROP_USERNAME_SERVICE_ACCOUNT is defined.
		verify(mockProps, times(1)).getProperty(UserInfo.PROP_USERNAME_SERVICE_ACCOUNT, null);
		//no clone as no PROP_USERNAME_SERVICE_ACCOUNT property is defined
		verify(mockProps, never()).clone();
		verifyStatic();
		Context.authenticate(mockCredentials);
	}
	
	@Test
	public void doFilter_shouldAuthenticateTheRequestWithATokenSpecifiedWithAuthHeaderAndBearerSchemeAndDedicatedServiceAccount()
	        throws Exception {
		final String jwtToken = "header.payload.signature";
		when(mockRequest.getHeader(HEADER_NAME_AUTH)).thenReturn(SCHEME_BEARER + " " + jwtToken);
		Properties clonedProperties = new Properties();
		Properties props = new Properties() {
			
			@Override
			public synchronized Object clone() {
				return clonedProperties;
			}
		};
		final String propNameThatWontBeUsed = "testPropertyNotUsed";
		final String propNameThatWillBeUsed = "testPropertyUsed";
		final String username = "testUsernameServiceAccount";
		props.setProperty(UserInfo.PROP_USERNAME, propNameThatWontBeUsed);
		props.setProperty(UserInfo.PROP_USERNAME_SERVICE_ACCOUNT, propNameThatWillBeUsed);
		clonedProperties.setProperty(UserInfo.PROP_USERNAME, "NeverUsedProperty");
		clonedProperties.setProperty(UserInfo.PROP_USERNAME_SERVICE_ACCOUNT, propNameThatWillBeUsed);
		
		when(Context.getRegisteredComponent(OAUTH_PROP_BEAN_NAME, Properties.class)).thenReturn(props);
		Claims testClaims = new DefaultClaims(Collections.singletonMap(propNameThatWillBeUsed, username));
		when(JwtUtils.parseAndVerifyToken(jwtToken, clonedProperties)).thenReturn(testClaims);
		whenNew(getAccessibleConstructor(UserInfo.class, Properties.class, String.class)).withArguments(clonedProperties,
		    JsonUtil.toJson(testClaims)).thenReturn(mockUserInfo);
		whenNew(getAccessibleConstructor(OAuth2TokenCredentials.class, UserInfo.class, boolean.class)).withArguments(
		    mockUserInfo, true).thenReturn(mockCredentials);
		
		filter.doFilter(mockRequest, null, mockFilterChain);
		
		verifyStatic();
		Context.authenticate(mockCredentials);
		Assert.assertEquals("The initial properties object should not be changed for username", propNameThatWontBeUsed,
		    props.get(UserInfo.PROP_USERNAME));
		Assert.assertEquals("The cloned properties will be changed for username property",
		    propNameThatWillBeUsed, clonedProperties.get(UserInfo.PROP_USERNAME));
	}
	
	@Test
	public void doFilter_shouldAuthenticateTheRequestWithATokenSpecifiedWithXJwtAssertHeader() throws Exception {
		final String jwtToken = "header.payload.signature";
		when(mockRequest.getHeader(HEADER_NAME_X_JWT_ASSERT)).thenReturn(jwtToken);
		when(Context.getRegisteredComponent(OAUTH_PROP_BEAN_NAME, Properties.class)).thenReturn(mockProps);
		final String propName = "testProperty";
		final String username = "testUsername";
		Claims testClaims = new DefaultClaims(Collections.singletonMap(propName, username));
		when(JwtUtils.parseAndVerifyToken(jwtToken, mockProps)).thenReturn(testClaims);
		when(mockProps.getProperty(UserInfo.PROP_USERNAME)).thenReturn(propName);
		whenNew(getAccessibleConstructor(UserInfo.class, Properties.class, String.class)).withArguments(mockProps,
		    JsonUtil.toJson(testClaims)).thenReturn(mockUserInfo);
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
