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
