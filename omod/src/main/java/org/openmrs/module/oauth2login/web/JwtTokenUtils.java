package org.openmrs.module.oauth2login.web;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import sun.security.rsa.RSAPublicKeyImpl;

import java.security.InvalidKeyException;
import java.util.Base64;

/**
 * Utility class for parsing and verifying JWT tokens
 */
public class JwtTokenUtils {
	
	public static Claims parseAndVerifyToken(String jwtToken, String verificationKey) throws InvalidKeyException {
		
		return Jwts.parserBuilder().setSigningKey(RSAPublicKeyImpl.newKey(Base64.getDecoder().decode(verificationKey)))
		        .build().parseClaimsJws(jwtToken).getBody();
		
	}
	
}
