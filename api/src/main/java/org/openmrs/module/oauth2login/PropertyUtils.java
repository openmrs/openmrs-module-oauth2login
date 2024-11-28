/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.oauth2login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.util.OpenmrsUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertyUtils {
	
	protected static final Log LOG = LogFactory.getLog(PropertyUtils.class);
	
	private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
	
	public static Properties getProperties(Path path) throws IOException {
        Properties props = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            props.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                props.setProperty(key, resolveEnvVariables(value));
            }
        }
        return props;
    }
	
	/**
	 * Resolves environment variables in the given string.
	 * <p>
	 * This method searches for placeholders in the format ${ENV_VAR} within the input string and
	 * replaces them with the corresponding environment variable values. It first checks system
	 * properties and then environment variables for the value of each placeholder.
	 * 
	 * @param value the input string containing placeholders for environment variables
	 * @return the input string with all environment variables resolved
	 */
	public static String resolveEnvVariables(String value) {
		Matcher matcher = ENV_PATTERN.matcher(value);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			String envVar = matcher.group(1);
			String envValue = System.getProperty(envVar, System.getenv(envVar));
			matcher.appendReplacement(buffer, Matcher.quoteReplacement(envValue != null ? envValue : matcher.group(0)));
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}
	
	public static Path getOAuth2PropertiesPath() {
		Path path = Paths.get(OpenmrsUtil.getApplicationDataDirectory(), "oauth2.properties");
		if (!path.toFile().exists()) {
			LOG.error("the property file doesn't exist " + path.toAbsolutePath());
		}
		return path;
	}
	
	public static Properties getOAuth2Properties() throws IOException {
		return getProperties(getOAuth2PropertiesPath());
	}
}
