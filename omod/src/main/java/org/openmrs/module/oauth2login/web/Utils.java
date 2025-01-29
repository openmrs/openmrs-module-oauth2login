package org.openmrs.module.oauth2login.web;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.oauth2login.OAuth2LoginConstants;
import org.openmrs.module.oauth2login.PropertyUtils;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Contains general utility methods
 */
public class Utils {
	
	/**
	 * Gets a file located in the OpenMRS app data directory
	 * 
	 * @param filename the name of the file to lookup
	 * @return The file object
	 * @throws Exception
	 */
	public static File getFileInAppDataDirectory(String filename) {
		return Paths.get(OpenmrsUtil.getApplicationDataDirectory(), filename).toFile();
	}
	
	public static String getPostLogoutRedirectUrl(HttpServletRequest request) throws IOException {
		Properties properties = PropertyUtils.getProperties(PropertyUtils.getOAuth2PropertiesPath());
		String redirectPath = properties.getProperty("logoutUri");
		boolean encodeDisabled = Boolean.valueOf(properties.getProperty("logoutUri.encode.disabled"));
		//the redirect path can contain a [token] that should be replaced by the auth token
		if (StringUtils.isNoneBlank(redirectPath) && redirectPath.contains("[token]")) {
			String token = null;
			if (Context.getAuthenticatedUser() != null) {
				token = Context.getAuthenticatedUser().getUserProperty(OAuth2LoginConstants.USER_PROP_ID_TOKEN);
			}
			
			if (StringUtils.isNotBlank(token)) {
				String encoded = URLEncoder.encode(token, "UTF-8");
				redirectPath = StringUtils.replace(redirectPath, "[token]", encoded);
			} else {
				//Oauth2 specification requires the id_token_hint or client_id, fallback to client_id
				UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(redirectPath);
				MultiValueMap<String, String> params = urlBuilder.build().getQueryParams();
				if (!params.containsKey("client_id")) {
					redirectPath = StringUtils.replace(redirectPath, "id_token_hint=[token]",
					    "client_id=" + properties.getProperty("clientId"));
				}
			}
		}
		
		if (!encodeDisabled) {
			redirectPath = encodeUrl(redirectPath);
		}
		return StringUtils.defaultIfBlank(redirectPath, request.getContextPath() + "/oauth2login");
	}
	
	protected static String encodeUrl(String logoutUrl) {
		UriComponents urlComponents = UriComponentsBuilder.fromHttpUrl(logoutUrl).build();
		MultiValueMap<String, String> params = urlComponents.getQueryParams();
		UriComponentsBuilder urlBuilderEncoded = UriComponentsBuilder.newInstance();
		urlBuilderEncoded.scheme(urlComponents.getScheme());
		urlBuilderEncoded.host(urlComponents.getHost());
		urlBuilderEncoded.port(urlComponents.getPort());
		urlBuilderEncoded.path(urlComponents.getPath());
		urlBuilderEncoded.fragment(urlComponents.getFragment());
		if (params != null) {
			for (Map.Entry<String, List<String>> entry : params.entrySet()) {
				final String encodedKey = URLEncoder.encode(entry.getKey());
				entry.getValue().forEach(v -> urlBuilderEncoded.queryParam(encodedKey, URLEncoder.encode(v)));
			}
		}
		
		return urlBuilderEncoded.build().toString();
	}
}
