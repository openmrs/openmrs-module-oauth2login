/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.oauth2login;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PropertyUtilsTest {
	
	@Test
	public void resolveEnvVariables_shouldReturnResolvedValue() {
		String value = "Authorization URL: ${OAUTH_URL}";
		System.setProperty("OAUTH_URL", "https://localhost:8080/auth");
		
		String resolvedValue = PropertyUtils.resolveEnvVariables(value);
		
		assertNotNull(resolvedValue);
		assertEquals("Authorization URL: https://localhost:8080/auth", resolvedValue);
	}
	
	@Test
	public void resolveEnvVariables_shouldReturnResolvedValueWithMultipleEnvVariables() {
		String value = "Authorization URL: ${OAUTH_URL}, Client Secret: ${CLIENT_SECRET}";
		System.setProperty("OAUTH_URL", "https://localhost:8080/auth");
		System.setProperty("CLIENT_SECRET", "secret");
		
		String resolvedValue = PropertyUtils.resolveEnvVariables(value);
		
		assertNotNull(resolvedValue);
		assertEquals("Authorization URL: https://localhost:8080/auth, Client Secret: secret", resolvedValue);
	}
	
	@Test
	public void resolveEnvVariables_shouldReturnResolvedValueWithMultipleOccurrencesOfEnvVariable() {
		String value = "Authorization URL: ${OAUTH_URL}, Authorization URL: ${OAUTH_URL}";
		System.setProperty("OAUTH_URL", "https://localhost:8080/auth");
		
		String resolvedValue = PropertyUtils.resolveEnvVariables(value);
		
		assertNotNull(resolvedValue);
		assertEquals("Authorization URL: https://localhost:8080/auth, Authorization URL: https://localhost:8080/auth",
		    resolvedValue);
	}
	
	@Test
	public void resolveEnvVariables_shouldReturnOriginalValueIfNoEnvVariable() {
		String value = "Authorization URL: ${OAUTH_URL}";
		
		String resolvedValue = PropertyUtils.resolveEnvVariables(value);
		
		assertNotNull(resolvedValue);
		assertEquals("Authorization URL: ${OAUTH_URL}", resolvedValue);
	}
}
