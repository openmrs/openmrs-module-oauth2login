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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
public class OAuth2LoginActivator extends BaseModuleActivator implements DaemonTokenAware {
	
	private final Log log = LogFactory.getLog(getClass());
	
	private DaemonToken daemonToken;
	
	/**
	 * @see #started()
	 */
	public void started() {
		log.info("Started " + OAuth2LoginConstants.MODULE_NAME);
		
		Context.getRegisteredComponent("oauth2login.usernameAuthenticationScheme", DaemonTokenAware.class).setDaemonToken(
		    daemonToken);
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
