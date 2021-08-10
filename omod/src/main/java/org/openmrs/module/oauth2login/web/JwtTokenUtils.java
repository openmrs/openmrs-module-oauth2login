package org.openmrs.module.oauth2login.web;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

/**
 * Utility class for parsing and verifying JWT tokens
 */
public class JwtTokenUtils {
	
	public static Claims parseAndVerifyToken(String jwtToken, String key) throws Exception {
		//TODO Support other algorithms other than RSA depending on the alg value in the token header
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(key));
		PublicKey publicKey = keyFactory.generatePublic(keySpec);
		return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(jwtToken).getBody();
		
	}
	
}
