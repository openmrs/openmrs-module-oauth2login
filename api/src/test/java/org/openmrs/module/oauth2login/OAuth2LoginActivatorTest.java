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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.module.ModuleException;
import org.openmrs.module.ModuleFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Properties;

import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PropertyUtils.class, ModuleFactory.class })
public class OAuth2LoginActivatorTest {
	
	private OAuth2LoginActivator activator;
	
	@Before
	public void setup() {
		activator = new OAuth2LoginActivator();
		PowerMockito.mockStatic(PropertyUtils.class);
		PowerMockito.mockStatic(ModuleFactory.class);
	}
	
	@Test
	public void willStart_shouldStopAndUnloadModuleIfOAuth2IsDisabled() throws IOException {
		// Setup
		Properties mockProperties = mock(Properties.class);
		when(mockProperties.getProperty(OAuth2LoginConstants.OAUTH2_ENABLED_PROPERTY, "true")).thenReturn("false");
		when(PropertyUtils.getOAuth2Properties()).thenReturn(mockProperties);
		
		// Replay
		activator.willStart();
		
		// Verify
		verifyStatic(times(1));
		ModuleFactory.stopModule(ModuleFactory.getModuleById(OAuth2LoginConstants.MODULE_ARTIFACT_ID));
		
		verifyStatic(times(1));
		ModuleFactory.unloadModule(ModuleFactory.getModuleById(OAuth2LoginConstants.MODULE_ARTIFACT_ID));
	}
	
	@Test
	public void willStart_shouldStartModuleIfOAuth2IsEnabled() throws IOException {
		// Setup
		Properties mockProperties = mock(Properties.class);
		when(mockProperties.getProperty(OAuth2LoginConstants.OAUTH2_ENABLED_PROPERTY, "true")).thenReturn("true");
		when(PropertyUtils.getOAuth2Properties()).thenReturn(mockProperties);
		
		// Replay
		activator.willStart();
		
		// Verify
		verifyStatic(times(0));
		ModuleFactory.stopModule(ModuleFactory.getModuleById(OAuth2LoginConstants.MODULE_ARTIFACT_ID));
		
		verifyStatic(times(0));
		ModuleFactory.unloadModule(ModuleFactory.getModuleById(OAuth2LoginConstants.MODULE_ARTIFACT_ID));
	}
	
	@Test
	public void willStart_shouldStopAndUnloadModuleIfModuleIsAlreadyStarted() throws IOException {
		// Setup
		Properties mockProperties = mock(Properties.class);
		when(mockProperties.getProperty(OAuth2LoginConstants.OAUTH2_ENABLED_PROPERTY, "true")).thenReturn("false");
		when(PropertyUtils.getOAuth2Properties()).thenReturn(mockProperties);
		when(ModuleFactory.isModuleStarted(OAuth2LoginConstants.MODULE_ARTIFACT_ID)).thenReturn(true);
		
		// Replay
		activator.willStart();
		
		// Verify
		verifyStatic(times(1));
		ModuleFactory.isModuleStarted(OAuth2LoginConstants.MODULE_ARTIFACT_ID);
		
		verifyStatic(times(2));
		ModuleFactory.stopModule(ModuleFactory.getModuleById(OAuth2LoginConstants.MODULE_ARTIFACT_ID));
		
		verifyStatic(times(2));
		ModuleFactory.unloadModule(ModuleFactory.getModuleById(OAuth2LoginConstants.MODULE_ARTIFACT_ID));
	}
	
	@Test(expected = ModuleException.class)
	public void willStart_shouldThrowModuleExceptionIfPropertiesFileCannotBeLoaded() throws IOException {
		OAuth2LoginActivator activator = new OAuth2LoginActivator();
		PropertyUtils propertyUtils = mock(PropertyUtils.class);
		when(PropertyUtils.getOAuth2Properties()).thenThrow(new IOException());
		
		activator.willStart();
	}
}
