package org.openmrs.module.oauth2login.authscheme;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
public class OAuth2UserTest {
	
	private OAuth2User oAuth2User;
	
	Properties props;
	
	@Before
	public void setUp() throws Exception {
		
	}
	
	@Test
	public void shouldGetRoles() {
		
		// Mock properties
		props = new Properties();
		props.setProperty(OAuth2User.MAPPINGS_PFX + OAuth2User.PROP_ROLES, "roles");
		
		// Initialize oAuth2User
		oAuth2User = new OAuth2User(mockUserInfoJsonWithRoles(), props);
		
		// Test
		List<String> roles = oAuth2User.getRoles();
		
		// Verify
		Assert.assertEquals(2, roles.size());
		Assert.assertTrue(roles.contains("nurse"));
		Assert.assertTrue(roles.contains("doctor"));
	}
	
	@Test
	public void shouldNotGetRoles() {
		
		// Mock properties
		props = new Properties();
		
		// Initialize oAuth2User
		oAuth2User = new OAuth2User(mockUserInfoJsonWithoutRoles(), props);
		
		// Test
		List<String> roles = oAuth2User.getRoles();
		
		// Verify
		Assert.assertEquals(0, roles.size());
	}
	
	private String mockUserInfoJsonWithRoles() {
		return "{\n" + "  \"sub\": \"31a709c3-67f4-4b01-b76c-b39e650c0a41\",\n" + "  \"name\": \"John Doe\",\n"
		        + "  \"given_name\": \"John\",\n" + "  \"family_name\": \"Doe\",\n"
		        + "  \"profile\": \"http://example.com/profile\",\n" + "  \"picture\": \"http://example.com/picture\",\n"
		        + "  \"email\": \"jdoe@example.com\",\n" + "  \"email_verified\": true,\n" + "  \"locale\": \"en\",\n"
		        + "  \"roles\": \"nurse,doctor\"\n" + "}";
	}
	
	private String mockUserInfoJsonWithoutRoles() {
		return "{\n" + "  \"sub\": \"31a709c3-67f4-4b01-b76c-b39e650c0a41\",\n" + "  \"name\": \"John Doe\",\n"
		        + "  \"given_name\": \"John\",\n" + "  \"family_name\": \"Doe\",\n"
		        + "  \"profile\": \"http://example.com/profile\",\n" + "  \"picture\": \"http://example.com/picture\",\n"
		        + "  \"email\": \"jdoe@example.com\",\n" + "  \"email_verified\": true,\n" + "  \"locale\": \"en\"\n" + "}";
	}
}
