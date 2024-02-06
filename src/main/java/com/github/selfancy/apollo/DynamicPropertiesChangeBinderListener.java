package com.github.selfancy.apollo;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DynamicPropertiesChangeBinderListener
 * <p>
 * {@link ConfigurationProperties}
 * {@link DynamicProperties}
 * <p>
 * Created by mike on 2020/8/13 since 1.0
 */
@SuppressWarnings("all")
@ConditionalOnClass(Config.class)
@ConditionalOnProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED)
class DynamicPropertiesChangeBinderListener implements ApplicationContextAware,
        ApplicationListener<ApplicationStartedEvent>, ConfigChangeListener, Ordered {

    private Binder binder;
    private String[] apolloNamespaces;
    private static final int ORDER = DynamicPropertiesConfigBeanPostProcessor.ORDER - 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicPropertiesChangeBinderListener.class);

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ConfigurableApplicationContext context = (ConfigurableApplicationContext) applicationContext;
        ConfigurableEnvironment environment = context.getEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        this.binder = new Binder(ConfigurationPropertySources.from(propertySources),
                new PropertySourcesPlaceholdersResolver(propertySources),
                context.getBeanFactory().getConversionService(),
                context.getBeanFactory()::copyRegisteredEditorsTo);
        this.apolloNamespaces = environment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES, String[].class,
                new String[]{ConfigConsts.NAMESPACE_APPLICATION});
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        String[] namespaces = this.apolloNamespaces;
        Set<String> interestedKeyPrefixes = getConfigBeanMap().keySet();
        if (!interestedKeyPrefixes.isEmpty()) {
            for (String namespace : namespaces) {
                Config config = ConfigService.getConfig(namespace);
                config.addChangeListener(this, null, interestedKeyPrefixes);
            }
        }
    }

    @Override
    public synchronized void onChange(ConfigChangeEvent changeEvent) {
        Set<String> changedKeys = changeEvent.changedKeys();
        Set<String> refreshedKeys = new HashSet<>();
        for (Map.Entry<String, DynamicPropertiesBeanBinder> entry : getConfigBeanMap().entrySet()) {
            String propertiesPrefix = entry.getKey();
            for (String changedKey : changedKeys) {
                DynamicPropertiesBeanBinder beanBinder = entry.getValue();
                if (changedKey.startsWith(propertiesPrefix) && !refreshedKeys.contains(propertiesPrefix)
                        && beanBinder.isEnabledDynamicChange()) {
                    refreshConfigPropertiesBean(propertiesPrefix, beanBinder);
                    LOGGER.info("Dynamic update apollo changed value successfully, refreshed bean {}.\n{}",
                            beanBinder.getBean().getClass().getName(),
                            changedKeys.stream()
                                    .filter(key -> key.startsWith(propertiesPrefix))
                                    .map(key -> changeEvent.getChange(key))
                                    .map(String::valueOf)
                                    .collect(Collectors.joining("\n")));
                    refreshedKeys.add(propertiesPrefix);
                }
            }
        }
    }

    private <T> void refreshConfigPropertiesBean(String configPrefix, DynamicPropertiesBeanBinder beanBinder) {
        beanBinder.bind(binder, configPrefix);
    }

    private Map<String, DynamicPropertiesBeanBinder> getConfigBeanMap() {
        return DynamicPropertiesConfigBeanPostProcessor.getConfigBeanBinderMap();
    }
}
