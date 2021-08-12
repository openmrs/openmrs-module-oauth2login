package org.openmrs.module.oauth2login.web;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

/**
 * Provides utility methods for parsing and verifying JWT tokens
 */
public class JwtUtils {
	
	protected static final Logger log = LoggerFactory.getLogger(JwtUtils.class);
	
	public static final String OAUTH_PROP_KEY = "publicKey";
	
	public static final String OAUTH_PROP_KEY_FILE = "publicKeyFilename";
	
	public static String publicKeyTxt = null;
	
	/**
	 * Parses and verifies a JWT token
	 * 
	 * @param jwtToken the JWT token
	 * @param oauthProps oauth2 properties instance
	 * @return Claims object
	 * @throws Exception
	 */
	public static Claims parseAndVerifyToken(String jwtToken, Properties oauthProps) throws Exception {
		if (publicKeyTxt == null) {
			publicKeyTxt = getPublicKey(oauthProps);
		}
		
		if (StringUtils.isBlank(publicKeyTxt)) {
			throw new APIException("Unable to find public key to verify JWT tokens");
		}
		
		//TODO Support other algorithms other than RSA depending on the alg value in the token header
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyTxt));
		PublicKey publicKey = keyFactory.generatePublic(keySpec);
		return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(jwtToken).getBody();
		
	}
	
	/**
	 * Looks up the public key based on the specified oauthProps properties. Lookup order is the oauth
	 * property, and then the configured file containing the key.
	 * 
	 * @param oauthProps Properties instance
	 * @return the public key
	 * @throws Exception
	 */
	public synchronized static String getPublicKey(Properties oauthProps) throws Exception {
		if (StringUtils.isNotBlank(oauthProps.getProperty(OAUTH_PROP_KEY))) {
			log.info("Using public key specified via the oauth property named: " + OAUTH_PROP_KEY);
			
			return oauthProps.getProperty(OAUTH_PROP_KEY).trim();
		}
		
		if (StringUtils.isNotBlank(oauthProps.getProperty(OAUTH_PROP_KEY_FILE))) {
			File file = Utils.getFileInAppDataDirectory(oauthProps.getProperty(OAUTH_PROP_KEY_FILE).trim());
			if (file.exists()) {
				log.info("Using public key from the file: " + file);
				
				return FileUtils.readFileToString(file, UTF_8).trim();
			}
			
			log.error("The oauth public key file doesn't exist -> " + file.getAbsolutePath());
		}
		
		//TODO Add support to look up public key from IDP
		
		return null;
	}
	
}
