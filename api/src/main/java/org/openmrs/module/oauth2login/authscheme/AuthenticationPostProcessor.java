package org.openmrs.module.oauth2login.authscheme;

import org.openmrs.api.context.ContextAuthenticationException;

/**
 * Implementations of this interface can be set to UsernameAuthenticationScheme as a way to hook a
 * OAuth2 user info post processing routine.
 * 
 * @see UsernameAuthenticationScheme#setPostProcessor
 */
public interface AuthenticationPostProcessor {
	
	void process(UserInfo userInfo) throws ContextAuthenticationException;
}
