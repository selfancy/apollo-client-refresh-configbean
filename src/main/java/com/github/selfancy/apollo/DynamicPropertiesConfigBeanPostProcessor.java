package com.github.selfancy.apollo;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationBeanFactoryMetadata;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DynamicPropertiesConfigBeanPostProcessor
 * <p>
 * Created by mike on 2020/10/27 since 1.0
 */
class DynamicPropertiesConfigBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware, InitializingBean, Ordered {

    private ConfigurableApplicationContext applicationContext;
    private ConfigurationBeanFactoryMetadata beanFactoryMetadata;
    private static final Map<String, Object> configBeanMap = new ConcurrentHashMap<>();
    static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void afterPropertiesSet() {
        this.beanFactoryMetadata = this.applicationContext.getBean(
                ConfigurationBeanFactoryMetadata.BEAN_NAME,
                ConfigurationBeanFactoryMetadata.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        final ConfigurationProperties configurationProperties = getAnnotation(bean, beanName, ConfigurationProperties.class);
        final DynamicProperties dynamicProperties = getAnnotation(bean, beanName, DynamicProperties.class);
        if (configurationProperties != null && dynamicProperties != null) {
            configBeanMap.put(configurationProperties.prefix(), bean);
        }
        return bean;
    }

    private <A extends Annotation> A getAnnotation(Object bean, String beanName, Class<A> type) {
        A annotation = this.beanFactoryMetadata.findFactoryAnnotation(beanName, type);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(bean.getClass(), type);
        }
        return annotation;
    }

    static Map<String, Object> getConfigBeanMap() {
        return configBeanMap;
    }
}
