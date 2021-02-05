package org.openmrs.module.oauth2login.web.controller;

import org.openmrs.module.oauth2login.authscheme.UserInfo;

public interface AuthenticationPostProcessor {
	
	void process(UserInfo userInfo);
}
