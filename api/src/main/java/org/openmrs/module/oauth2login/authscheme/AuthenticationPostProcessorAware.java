package org.openmrs.module.oauth2login.authscheme;

public interface AuthenticationPostProcessorAware {
	
	void setPostProcessor(AuthenticationPostProcessor postProcessor);
}
