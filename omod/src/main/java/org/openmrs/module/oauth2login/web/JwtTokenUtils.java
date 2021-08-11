package org.openmrs.module.oauth2login.web;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.openmrs.util.OpenmrsUtil.getApplicationDataDirectory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

/**
 * Utility class for parsing and verifying JWT tokens
 */
public class JwtTokenUtils {
	
	protected static final Logger log = LoggerFactory.getLogger(JwtTokenUtils.class);
	
	public static final String OAUTH_PROP_KEY = "publicKey";
	
	public static final String OAUTH_PROP_KEY_FILE = "publicKeyFilename";
	
	public static String publicKeyTxt = null;
	
	public static Claims parseAndVerifyToken(String jwtToken) throws Exception {
		if (publicKeyTxt == null) {
			publicKeyTxt = getPublicKey();
		}
		
		if (StringUtils.isNotBlank(publicKeyTxt)) {
			throw new APIException("Unable to find public key to verify JWT tokens");
		}
		
		//TODO Support other algorithms other than RSA depending on the alg value in the token header
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyTxt));
		PublicKey publicKey = keyFactory.generatePublic(keySpec);
		return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(jwtToken).getBody();
		
	}
	
	public synchronized static String getPublicKey() throws Exception {
		Properties props = Context.getRegisteredComponent("oauth2.properties", Properties.class);
		if (StringUtils.isNotBlank(props.getProperty(OAUTH_PROP_KEY))) {
			log.info("Using public key specified via the oauth property named: " + OAUTH_PROP_KEY);
			return props.getProperty(OAUTH_PROP_KEY).trim();
		}
		
		if (StringUtils.isNotBlank(props.getProperty(OAUTH_PROP_KEY_FILE))) {
			Path path = Paths.get(getApplicationDataDirectory(), props.getProperty(OAUTH_PROP_KEY_FILE).trim());
			if (path.toFile().exists()) {
				log.info("Using public key from the file: " + path.toFile());
				return FileUtils.readFileToString(path.toFile(), UTF_8).trim();
			}
			
			log.error("The oauth public key file doesn't exist -> " + path.toAbsolutePath());
		}
		
		//TODO Add support to look up public key from IDP
		
		return null;
	}
	
}
