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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OAuth2BeanFactoryTest {
	
	@Test
	public void resolveEnvVariables_shouldReturnResolvedValue() {
		String value = "Database URL: ${DB_URL}";
		System.setProperty("DB_URL", "jdbc:mysql://localhost:3306/openmrs");
		
		String resolvedValue = OAuth2BeanFactory.resolveEnvVariables(value);
		
		assertNotNull(resolvedValue);
		assertEquals("Database URL: jdbc:mysql://localhost:3306/openmrs", resolvedValue);
	}
	
	@Test
	public void resolveEnvVariables_shouldReturnOriginalValueIfNoEnvVariable() {
		String value = "Database URL: ${DB_URL}";
		
		String resolvedValue = OAuth2BeanFactory.resolveEnvVariables(value);
		
		assertNotNull(resolvedValue);
		assertEquals("Database URL: ${DB_URL}", resolvedValue);
	}
}
