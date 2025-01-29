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

import static java.net.URLDecoder.decode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.oauth2login.OAuth2LoginConstants;
import org.openmrs.module.oauth2login.PropertyUtils;
import org.openmrs.module.oauth2login.web.controller.OAuth2IntegrationTest;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.UriComponents;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, PropertyUtils.class })
public class UtilsTest {
	
	private static final String BASE_URL = "http://localhost:8081/auth/realms/demo/protocol/openid-connect/logout?";
	
	private static final String PARAM_TOKEN = "id_token_hint";
	
	protected static final String PARAM_POST_LOGOUT_URL = "post_logout_redirect_uri";
	
	@BeforeClass
	public static void setUp() {
		OAuth2IntegrationTest.initPathInSystemProperties("Keycloak");
	}
	
	@Test
	public void getPostLogoutRedirectUrl_redirectToLogoutURL() throws Exception {
		final String idToken = "myToken";
		PowerMockito.mockStatic(Context.class);
		User user = new User();
		user.setUserProperty(OAuth2LoginConstants.USER_PROP_ID_TOKEN, idToken);
		Mockito.when(Context.getAuthenticatedUser()).thenReturn(user);
		MockHttpServletRequest request = new MockHttpServletRequest();
		
		String redirect = Utils.getPostLogoutRedirectUrl(request);
		
		assertEquals(redirect, BASE_URL + "id_token_hint=" + idToken);
	}
	
	@Test
	public void getPostLogoutRedirectUrl_shouldNotSetIdTokenIfUserIsNotAuthenticated() throws Exception {
		PowerMockito.mockStatic(Context.class);
		MockHttpServletRequest request = new MockHttpServletRequest();
		
		String redirect = Utils.getPostLogoutRedirectUrl(request);
		
		assertEquals(redirect, BASE_URL + "client_id=openmrs");
	}
	
	@Test
	public void getPostLogoutRedirectUrl_shouldNotSetIdTokenIfNoneIsSetForTheUser() throws Exception {
		PowerMockito.mockStatic(Context.class);
		Mockito.when(Context.getAuthenticatedUser()).thenReturn(new User());
		MockHttpServletRequest request = new MockHttpServletRequest();
		
		String redirect = Utils.getPostLogoutRedirectUrl(request);
		
		assertEquals(redirect, BASE_URL + "client_id=openmrs");
	}
	
	@Test
	public void encodeUrl_shouldEncodeThePostLogoutUrl() {
		final String baseLogoutUrl = "https://idp.com";
		final String token = "myToken";
		final String postLogoutUrl = "http://openmrs.org";
		String logoutUrl = baseLogoutUrl + "?" + PARAM_TOKEN + "=" + token + "&" + PARAM_POST_LOGOUT_URL + "="
		        + postLogoutUrl;
		
		logoutUrl = Utils.encodeUrl(logoutUrl);
		
		UriComponents logoutUrlComponents = fromUriString(logoutUrl).build();
		assertEquals("https", logoutUrlComponents.getScheme());
		assertEquals("idp.com", logoutUrlComponents.getHost());
		assertEquals(-1, logoutUrlComponents.getPort());
		assertNull(logoutUrlComponents.getPath());
		assertEquals(2, logoutUrlComponents.getQueryParams().size());
		assertEquals(token, logoutUrlComponents.getQueryParams().getFirst(PARAM_TOKEN));
		assertEquals("http%3A%2F%2Fopenmrs.org", logoutUrlComponents.getQueryParams().getFirst(PARAM_POST_LOGOUT_URL));
	}
	
	@Test
	public void encodeUrl_shouldEncodeAPostLogoutUrlWithPortAndPathAndOneParameter() throws Exception {
		final String logoutPath = "/realm";
		final String baseLogoutUrl = "https://idp.com:8080" + logoutPath;
		final String token = "myToken";
		final String postLogoutPath = "/spa";
		final String postLogoutUrl = "http://openmrs.org:8081" + postLogoutPath + "?p=my+v";
		String logoutUrl = baseLogoutUrl + "?" + PARAM_TOKEN + "=" + token + "&" + PARAM_POST_LOGOUT_URL + "="
		        + postLogoutUrl;
		
		logoutUrl = Utils.encodeUrl(logoutUrl);
		
		UriComponents logoutUrlComponents = fromUriString(logoutUrl).build();
		assertEquals("https", logoutUrlComponents.getScheme());
		assertEquals("idp.com", logoutUrlComponents.getHost());
		assertEquals(8080, logoutUrlComponents.getPort());
		assertEquals(logoutPath, logoutUrlComponents.getPath());
		assertEquals(2, logoutUrlComponents.getQueryParams().size());
		assertEquals(token, logoutUrlComponents.getQueryParams().getFirst(PARAM_TOKEN));
		final String encodedPostLogoutUrl = logoutUrlComponents.getQueryParams().getFirst(PARAM_POST_LOGOUT_URL);
		UriComponents postLogoutUrlComponents = fromUriString(decode(encodedPostLogoutUrl, "UTF-8")).build();
		assertEquals("http", postLogoutUrlComponents.getScheme());
		assertEquals("openmrs.org", postLogoutUrlComponents.getHost());
		assertEquals(8081, postLogoutUrlComponents.getPort());
		assertEquals(postLogoutPath, postLogoutUrlComponents.getPath());
		assertEquals("p=my+v", postLogoutUrlComponents.getQuery());
	}
	
	@Test
	public void encodeUrl_shouldEncodeAPostLogoutUrlThatIsARelativePath() {
		final String baseLogoutUrl = "https://idp.com";
		final String token = "myToken";
		final String postLogoutUrl = "/spa";
		String logoutUrl = baseLogoutUrl + "?" + PARAM_TOKEN + "=" + token + "&" + PARAM_POST_LOGOUT_URL + "="
		        + postLogoutUrl;
		
		logoutUrl = Utils.encodeUrl(logoutUrl);
		
		UriComponents logoutUrlComponents = fromUriString(logoutUrl).build();
		assertEquals("https", logoutUrlComponents.getScheme());
		assertEquals("idp.com", logoutUrlComponents.getHost());
		assertEquals(2, logoutUrlComponents.getQueryParams().size());
		assertEquals(token, logoutUrlComponents.getQueryParams().getFirst(PARAM_TOKEN));
		assertEquals("%2Fspa", logoutUrlComponents.getQueryParams().getFirst(PARAM_POST_LOGOUT_URL));
	}
	
	@Test
	public void encodeUrl_shouldEncodeAPostLogoutUrlThatIsARelativePathWithOneParameter() {
		final String baseLogoutUrl = "https://idp.com";
		final String token = "myToken";
		final String postLogoutUrl = "/spa?p1=my+v";
		String logoutUrl = baseLogoutUrl + "?" + PARAM_TOKEN + "=" + token + "&" + PARAM_POST_LOGOUT_URL + "="
		        + postLogoutUrl;
		
		logoutUrl = Utils.encodeUrl(logoutUrl);
		
		UriComponents logoutUrlComponents = fromUriString(logoutUrl).build();
		assertEquals("https", logoutUrlComponents.getScheme());
		assertEquals("idp.com", logoutUrlComponents.getHost());
		assertEquals(2, logoutUrlComponents.getQueryParams().size());
		assertEquals(token, logoutUrlComponents.getQueryParams().getFirst(PARAM_TOKEN));
		assertEquals("%2Fspa%3Fp1%3Dmy%2Bv", logoutUrlComponents.getQueryParams().getFirst(PARAM_POST_LOGOUT_URL));
	}
	
	@Test
	public void getPostLogoutRedirectUrl_shouldEncodeTheUrl() throws Exception {
		final String postLogoutUrl = "http://openmrs.org";
		final String logoutUrl = "https://idp.com?" + PARAM_POST_LOGOUT_URL + "=" + postLogoutUrl;
		Properties props = new Properties();
		props.setProperty("logoutUri", logoutUrl);
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(PropertyUtils.class);
		Path path = Paths.get("/test");
		PowerMockito.when(PropertyUtils.getOAuth2PropertiesPath()).thenReturn(path);
		PowerMockito.when(PropertyUtils.getProperties(path)).thenReturn(props);
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		
		String redirect = Utils.getPostLogoutRedirectUrl(request);
		
		assertEquals("https://idp.com?" + PARAM_POST_LOGOUT_URL + "=http%3A%2F%2Fopenmrs.org", redirect);
	}
	
	@Test
	public void getPostLogoutRedirectUrl_shouldNotEncodeTheUrlIfDisabled() throws Exception {
		final String postLogoutUrl = "http://openmrs.org";
		final String logoutUrl = "https://idp.com?" + PARAM_POST_LOGOUT_URL + "=" + postLogoutUrl;
		Properties props = new Properties();
		props.setProperty("logoutUri", logoutUrl);
		props.setProperty("logoutUri.encode.disabled", "true");
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(PropertyUtils.class);
		Path path = Paths.get("/test");
		PowerMockito.when(PropertyUtils.getOAuth2PropertiesPath()).thenReturn(path);
		PowerMockito.when(PropertyUtils.getProperties(path)).thenReturn(props);
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		
		String redirect = Utils.getPostLogoutRedirectUrl(request);
		
		assertEquals(logoutUrl, redirect);
	}
	
}
