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

import static org.openmrs.module.oauth2login.OAuth2LoginConstants.AUTH_SCHEME_COMPONENT;
import static org.openmrs.module.oauth2login.OAuth2LoginConstants.MODULE_ARTIFACT_ID;
import static org.openmrs.module.oauth2login.OAuth2LoginConstants.OAUTH2_ENABLED_PROPERTY;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.openmrs.module.ModuleException;
import org.openmrs.module.ModuleFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
public class OAuth2LoginActivator extends BaseModuleActivator implements DaemonTokenAware {
	
	private final Log log = LogFactory.getLog(getClass());
	
	private DaemonToken daemonToken;
	
	/**
	 * @see #willStart()
	 */
	@Override
	public void willStart() {
		// Checks if OAuth2 is enabled, if not, stops the module and unloads it
		try {
			Properties oauth2Props = PropertyUtils.getOAuth2Properties();
			boolean moduleEnabled = Boolean.parseBoolean(oauth2Props.getProperty(OAUTH2_ENABLED_PROPERTY, "true"));
			if (!moduleEnabled) {
				log.info("OAuth2 is disabled. Skipping module start.");
				// Stop the module if it is already started
				if (ModuleFactory.isModuleStarted(MODULE_ARTIFACT_ID)) {
					ModuleFactory.stopModule(ModuleFactory.getModuleById(MODULE_ARTIFACT_ID));
				}
				ModuleFactory.unloadModule(ModuleFactory.getModuleById(MODULE_ARTIFACT_ID));
			} else {
				log.info("OAuth2 is enabled. " + OAuth2LoginConstants.MODULE_NAME + " module will start.");
			}
		}
		catch (IOException e) {
			throw new ModuleException("Failed to load OAuth2 properties file", e);
		}
	}
	
	/**
	 * @see #started()
	 */
	public void started() {
		log.info("Started " + OAuth2LoginConstants.MODULE_NAME);
		
		Context.getRegisteredComponent(AUTH_SCHEME_COMPONENT, DaemonTokenAware.class).setDaemonToken(daemonToken);
	}
	
	/**
	 * @see #shutdown()
	 */
	public void shutdown() {
		log.info("Shut down " + OAuth2LoginConstants.MODULE_NAME);
	}
	
	@Override
	public void setDaemonToken(DaemonToken daemonToken) {
		this.daemonToken = daemonToken;
	}
}
