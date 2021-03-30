package com.github.selfancy.apollo;

import org.springframework.context.annotation.Import;

/**
 * DynamicPropertiesBeanAutoConfiguration
 * <p>
 * Created by mike on 2020/10/27 since 1.0
 */
@Import({DynamicPropertiesConfigBeanPostProcessor.class, DynamicPropertiesChangeBinderListener.class})
class DynamicPropertiesBeanAutoConfiguration {
}
