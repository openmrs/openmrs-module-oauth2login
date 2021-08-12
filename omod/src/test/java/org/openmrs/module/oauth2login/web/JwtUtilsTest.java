package org.openmrs.module.oauth2login.web;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.openmrs.module.oauth2login.web.JwtUtils.OAUTH_PROP_KEY;
import static org.openmrs.module.oauth2login.web.JwtUtils.OAUTH_PROP_KEY_FILE;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.api.APIException;
import org.openmrs.util.OpenmrsUtil;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.jsonwebtoken.ExpiredJwtException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OpenmrsUtil.class, FileUtils.class, Utils.class })
public class JwtUtilsTest {
	
	@Mock
	private Properties mockProps;
	
	@Mock
	private File mockKeyFile;
	
	@Rule
	public ExpectedException ee = ExpectedException.none();
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		mockStatic(OpenmrsUtil.class);
		mockStatic(Utils.class);
		mockStatic(FileUtils.class);
	}
	
	@Test
	public void getPublicKey_shouldLookUpTheKeyFromAnOauthProperty() throws Exception {
		final String testKey = "test_key";
		when(mockProps.getProperty(OAUTH_PROP_KEY)).thenReturn(testKey);
		assertEquals(testKey, JwtUtils.getPublicKey(mockProps));
	}
	
	@Test
	public void getPublicKey_shouldLookUpTheKeyFromAFile() throws Exception {
		final String testKeyFromFile = "test_key_from_file";
		final String testFile = "test.txt";
		when(mockProps.getProperty(OAUTH_PROP_KEY_FILE)).thenReturn(testFile);
		when(Utils.getFileInAppDataDirectory(testFile)).thenReturn(mockKeyFile);
		when(mockKeyFile.exists()).thenReturn(true);
		when(FileUtils.readFileToString(mockKeyFile, StandardCharsets.UTF_8)).thenReturn(testKeyFromFile);
		assertEquals(testKeyFromFile, JwtUtils.getPublicKey(mockProps));
	}
	
	@Test
	public void parseAndVerifyToken_shouldFailIfNoPublicKeyIsFound() throws Exception {
		ee.expect(APIException.class);
		ee.expectMessage(Matchers.equalTo("Unable to find public key to verify JWT tokens"));
		JwtUtils.parseAndVerifyToken("someToken", mockProps);
	}
	
	@Test
	public void parseAndVerifyToken_shouldParseAndVerifyTheTokenWithTheConfiguredKey() throws Exception {
		ee.expect(ExpiredJwtException.class);
		ee.expectMessage("JWT expired at 2021-08-06");
		final String publicKey = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("publicKey.txt"), "UTF-8");
		when(mockProps.getProperty(OAUTH_PROP_KEY)).thenReturn(publicKey);
		final String token = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("jwtToken.txt"), "UTF-8");
		
		JwtUtils.parseAndVerifyToken(token, mockProps);
	}
	
}
