package com.github.selfancy.apollo;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DynamicPropertiesConfigBeanPostProcessor
 * <p>
 * Created by mike on 2020/10/27 since 1.0
 */
class DynamicPropertiesConfigBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware, Ordered {

    private ConfigurableApplicationContext applicationContext;
    private static final Map<String, DynamicPropertiesBeanBinder> configBeanBinderMap = new ConcurrentHashMap<>();
    static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        DynamicPropertiesBeanBinder beanBinder = new DynamicPropertiesBeanBinder(applicationContext, bean, beanName);
        if (beanBinder.isDynamicPropertiesBean()) {
            configBeanBinderMap.put(beanBinder.getPrefix(), beanBinder);
        }
        return bean;
    }

    static Map<String, DynamicPropertiesBeanBinder> getConfigBeanBinderMap() {
        return configBeanBinderMap;
    }
}
