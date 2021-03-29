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
import org.springframework.boot.context.properties.ConfigurationBeanFactoryMetadata;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.bind.handler.IgnoreTopLevelConverterNotFoundBindHandler;
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
    private ConfigurationBeanFactoryMetadata beanFactoryMetadata;
    private String[] apolloNamespaces;
    private static final int ORDER = DynamicPropertiesConfigBeanPostProcessor.ORDER - 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicPropertiesChangeBinderListener.class);

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        final ConfigurableApplicationContext context = (ConfigurableApplicationContext) applicationContext;
        final ConfigurableEnvironment environment = context.getEnvironment();
        final MutablePropertySources propertySources = environment.getPropertySources();
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
        final Set<String> changedKeys = changeEvent.changedKeys();
        final Set<String> refreshedKeys = new HashSet<>();
        for (Map.Entry<String, Object> entry : getConfigBeanMap().entrySet()) {
            String propertiesPrefix = entry.getKey();
            for (String changedKey : changedKeys) {
                if (changedKey.startsWith(propertiesPrefix) && !refreshedKeys.contains(propertiesPrefix)) {
                    final Object bean = entry.getValue();
                    refreshConfigPropertiesBean(propertiesPrefix, bean);
                    LOGGER.info("Dynamic update apollo changed value successfully, refreshed bean {}.\n{}", bean.getClass().getName(),
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

    private <T> void refreshConfigPropertiesBean(String configPrefix, T bean) {
        final Bindable<T> target = Bindable.ofInstance(bean);
        BindHandler handler = new IgnoreTopLevelConverterNotFoundBindHandler();
        this.binder.bind(configPrefix, target, handler);
    }

    private Map<String, Object> getConfigBeanMap() {
        return DynamicPropertiesConfigBeanPostProcessor.getConfigBeanMap();
    }
}
