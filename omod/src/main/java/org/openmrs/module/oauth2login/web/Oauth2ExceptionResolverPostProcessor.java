/*
 * Copyright (C) Amiyul LLC - All Rights Reserved
 *
 * This source code is protected under international copyright law. All rights
 * reserved and protected by the copyright holder.
 *
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holder. If you encounter this file and do not have
 * permission, please contact the copyright holder and delete this file.
 */
package org.openmrs.module.oauth2login.web;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

/**
 * Replaces the {@link org.springframework.web.servlet.HandlerExceptionResolver} bean defined in
 * core with our subclass {@link Oauth2ExceptionResolver}
 * 
 * @see Oauth2ExceptionResolver
 */
@Component
public class Oauth2ExceptionResolverPostProcessor implements BeanFactoryPostProcessor {
	
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String[] names = beanFactory.getBeanNamesForType(SimpleMappingExceptionResolver.class, true, false);
		if (names.length > 0) {
			if (names.length > 1) {
				throw new RuntimeException("Expected exactly one bean of type SimpleMappingExceptionResolver");
			}
			
			final BeanDefinition beanDefinition = beanFactory.getBeanDefinition(names[0]);
			beanDefinition.setBeanClassName(Oauth2ExceptionResolver.class.getName());
		}
	}
	
}
