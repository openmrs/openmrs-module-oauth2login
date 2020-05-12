/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.oauth2login.web.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Authenticated;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.oauth2login.authscheme.OAuth2TokenCredentials;
import org.openmrs.module.oauth2login.authscheme.OAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class OAuth2LoginController {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	private String userInfoUri;
	
	private Properties oauth2Props;
	
	private RestOperations restTemplate; // to fetch user infos from OAuth2 provider
	
	@Autowired
	public void setRestTemplate(@Qualifier("oauth2.restTemplate") RestOperations restTemplate) {
		this.restTemplate = restTemplate;
	}
	
	@Autowired
	public void setOAuth2Properties(@Qualifier("oauth2.properties") Properties oauth2Props) {
		this.oauth2Props = oauth2Props;
	}
	
	@Autowired
	public void setUserInfoUri(@Qualifier("oauth2.userInfoUri") String userInfoUri) {
		this.userInfoUri = userInfoUri;
	}
	
	@RequestMapping(value = "/oauth2login", method = GET)
	public ModelAndView login() throws RestClientException, IOException, URISyntaxException {
		
		authenticateWithSpringSecurity();
		if (log.isInfoEnabled()) {
			log.info("try login via userInfoUri: " + userInfoUri);
		}
		String userInfoJson;
		try {
			userInfoJson = restTemplate.getForObject(new URI(userInfoUri), String.class);
		}
		catch (Exception ex) {
			//just to have the error in openMRS logs.
			log.error("can't validate oauth2 login", ex);
			throw ex;
		}
		
		String username = getUsername(userInfoJson);
		if (StringUtils.isEmpty(username)) {
			throw new ContextAuthenticationException(
			        "The user info did not point to a valid or usable username to authenticate.");
		}
		
		OAuth2User user = new OAuth2User(username, userInfoJson);
		
		Authenticated authenticated = Context.authenticate(new OAuth2TokenCredentials(user));
		log.info("The user '" + username + "' was successfully authenticated with OpenMRS with user "
		        + authenticated.getUser());
		
		return new ModelAndView("redirect:/");
	}
	
	public String getUsername(String userInfoJson) {
		return OAuth2User.get(userInfoJson, OAuth2User.MAPPINGS_PFX + OAuth2User.PROP_USERNAME, oauth2Props);
	}
	
	private void authenticateWithSpringSecurity() {
		SecurityContextHolder.getContext().setAuthentication(new Authentication() {
			
			@Override
			public String getName() {
				// TODO Check what should be appropriate here
				return "internal";
			}
			
			@Override
			public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
			}
			
			@Override
			public boolean isAuthenticated() {
				return true;
			}
			
			@Override
			public Object getPrincipal() {
				// TODO Check what should be appropriate here
				return "internal";
			}
			
			@Override
			public Object getDetails() {
				return null;
			}
			
			@Override
			public Object getCredentials() {
				return null;
			}
			
			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				return null;
			}
		});
	}
}
