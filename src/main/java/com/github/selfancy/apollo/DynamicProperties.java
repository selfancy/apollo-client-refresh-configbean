package com.github.selfancy.apollo;

import java.lang.annotation.*;

/**
 * Dynamic properties.
 * Both required annotation `@DynamicProperties` and `@ConfigurationProperties`.
 * <br>
 * Sample:
 * <pre>
 * <br> @Data
 * <br> @DynamicProperties
 * <br> @ConfigurationProperties("custom")
 * <br> public class CustomProperties {
 * <br>     private String value;
 * <br> }
 * </pre>
 * Created by mike on 2020/8/13 since 1.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DynamicProperties {

    /**
     * Enabled property to enable dynamic refresh
     * <br>
     * Such as @ConfigurationProperties("sample") bean.
     * <br>
     * enable property: sample._dynamic_enabled=true  (default is true, empty property also true)
     * <br>
     * disable property: sample._dynamic_enabled=false
     *
     * @return String
     * @since 1.0.1
     */
    String enableProperty() default "_dynamic_enabled";
}
