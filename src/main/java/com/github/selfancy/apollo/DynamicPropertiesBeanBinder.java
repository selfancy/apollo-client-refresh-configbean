package com.github.selfancy.apollo;

import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.IgnoreTopLevelConverterNotFoundBindHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;

/**
 * DynamicPropertiesBeanBinder
 * <p>
 * Created by mike on 2024/02/06
 */
@SuppressWarnings("all")
final class DynamicPropertiesBeanBinder {

    private static final ClassLoader defaultClassLoader = ClassUtils.getDefaultClassLoader();
    private static final String configurationBeanFactoryMetadataClassName = "org.springframework.boot.context.properties.ConfigurationBeanFactoryMetadata";
    private static final String configurationPropertiesBeanClassName = "org.springframework.boot.context.properties.ConfigurationPropertiesBean";
    private static final boolean configurationPropertiesBeanIsPresent = ClassUtils.isPresent(configurationPropertiesBeanClassName, defaultClassLoader);
    private final ApplicationContext applicationContext;
    private final Object bean;
    private final String beanName;
    private final ConfigurationProperties configurationProperties;
    private final DynamicProperties dynamicProperties;
    private final InnerBinder innerBinder;

    DynamicPropertiesBeanBinder(ApplicationContext applicationContext, Object bean, String beanName) {
        this.applicationContext = applicationContext;
        this.bean = bean;
        this.beanName = beanName;
        InnerBinder binder;
        if (configurationPropertiesBeanIsPresent) {
            binder = new ConfigurationPropertiesBeanBinder(); // support springboot 2.4.0+
        } else {
            binder = new ConfigurationBeanFactoryMetadataBinder(); // older support
        }
        this.innerBinder = binder;
        this.configurationProperties = AnnotationUtils.findAnnotation(bean.getClass(), ConfigurationProperties.class);
        this.dynamicProperties = AnnotationUtils.findAnnotation(bean.getClass(), DynamicProperties.class);
    }

    public String getPrefix() {
        return configurationProperties != null ? configurationProperties.prefix() : null;
    }

    public Bindable<?> getBindTarget() {
        return innerBinder.getBindTarget();
    }

    public boolean isEnabledDynamicChange() {
        String prefix = getPrefix();
        if (prefix != null && dynamicProperties != null) {
            Boolean enableDynamic = applicationContext.getEnvironment().getProperty(
                    prefix + "." + dynamicProperties.enableProperty(), Boolean.class, Boolean.TRUE);
            return enableDynamic == Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public BindResult<?> bind(Binder binder, String configPrefix) {
        if (innerBinder instanceof ConfigurationPropertiesBeanBinder) {
            return ((ConfigurationPropertiesBeanBinder) innerBinder).bind();
        } else {
            BindHandler handler = new IgnoreTopLevelConverterNotFoundBindHandler();
            return binder.bind(configPrefix, getBindTarget(), handler);
        }
    }

    public boolean isDynamicPropertiesBean() {
        return configurationProperties != null && innerBinder.isDynamicPropertiesBean();
    }

    public Object getBean() {
        return bean;
    }

    private abstract class InnerBinder {

        abstract Bindable<?> getBindTarget();

        boolean isDynamicPropertiesBean() {
            return dynamicProperties != null;
        }
    }

    /**
     * @see org.springframework.boot.context.properties.ConfigurationBeanFactoryMetadata
     */
    private class ConfigurationBeanFactoryMetadataBinder extends InnerBinder {

        @Override
        public Bindable<?> getBindTarget() {
            return Bindable.ofInstance(bean);
        }

        @Override
        @SneakyThrows
        public boolean isDynamicPropertiesBean() {
            Class<?> configurationBeanFactoryMetadataClass = ClassUtils.forName(configurationBeanFactoryMetadataClassName, defaultClassLoader);
            Object configurationBeanFactoryMetadata = applicationContext.getBean(configurationBeanFactoryMetadataClassName, configurationBeanFactoryMetadataClass);
            ConfigurationProperties configurationProperties = (ConfigurationProperties) invokeMethod(configurationBeanFactoryMetadata,
                    "findFactoryAnnotation", beanName, ConfigurationProperties.class);
            if (configurationProperties == null) {
                configurationProperties = DynamicPropertiesBeanBinder.this.configurationProperties;
            }
            return configurationProperties != null && super.isDynamicPropertiesBean();
        }
    }

    /**
     * @see org.springframework.boot.context.properties.ConfigurationPropertiesBean
     */
    private class ConfigurationPropertiesBeanBinder extends InnerBinder {

        private final Class<?> configurationPropertiesBeanClass;
        private final Function<Object, BindResult<?>> bindFunction;

        @SneakyThrows
        public ConfigurationPropertiesBeanBinder() {
            this.configurationPropertiesBeanClass = ClassUtils.forName(configurationPropertiesBeanClassName, defaultClassLoader);
            Class<?> binderClass = ClassUtils.forName("org.springframework.boot.context.properties.ConfigurationPropertiesBinder", defaultClassLoader);
            this.bindFunction = obj -> {
                Constructor<?> constructor;
                try {
                    constructor = ReflectionUtils.accessibleConstructor(binderClass, ApplicationContext.class);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                Object configurationPropertiesBinder = BeanUtils.instantiateClass(constructor, applicationContext);
                return (BindResult<?>) invokeMethod(configurationPropertiesBinder, "bind", obj);
            };
        }

        @Override
        public Bindable<?> getBindTarget() {
            Object configurationPropertiesBean = getConfigurationPropertiesBean(applicationContext, bean, beanName);
            return (Bindable<?>) invokeMethod(configurationPropertiesBean, "asBindTarget");
        }

        @Override
        @SneakyThrows
        public boolean isDynamicPropertiesBean() {
            Object configurationPropertiesBean = getConfigurationPropertiesBean(applicationContext, bean, beanName);
            return configurationPropertiesBean != null && super.isDynamicPropertiesBean();
        }

        @SneakyThrows
        public Object getConfigurationPropertiesBean(ApplicationContext applicationContext, Object bean, String beanName) {
            Method method = ReflectionUtils.findMethod(configurationPropertiesBeanClass, "get",
                    ApplicationContext.class, Object.class, String.class);
            return ReflectionUtils.invokeMethod(Objects.requireNonNull(method), null, applicationContext, bean, beanName);
        }

        public BindResult<?> bind() {
            Object configurationPropertiesBean = getConfigurationPropertiesBean(applicationContext, bean, beanName);
            return bindFunction.apply(configurationPropertiesBean);
        }
    }

    private static Object invokeMethod(Object obj, String methodName, Object... args) {
        Class<?>[] argsTypes;
        if (args != null && args.length > 0) {
            argsTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argsTypes[i] = args[i].getClass();
            }
        } else {
            argsTypes = new Class<?>[0];
        }
        Method method = ReflectionUtils.findMethod(obj.getClass(), methodName, argsTypes);
        if (method == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(method);
        return ReflectionUtils.invokeMethod(method, obj, args);
    }
}
