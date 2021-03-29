## Introduce
> Support for apollo-client to dynamic auto refresh `@ConfigurationProperties` bean.
> 
> See about: [Apollo](https://github.com/ctripcorp/apollo)

## QuickStart

#### 1. Import dependency
Maven:
```xml
<dependency>
    <groupId>com.github.selfancy</groupId>
    <artifactId>apollo-client-refresh-configbean</artifactId>
    <version>1.0.0-RELEASE</version>
</dependency>
```
Gradle:
```groovy
implementation 'com.github.selfancy:apollo-client-refresh-configbean:1.0.0-RELEASE'
```
#### 2. Annotated at SpringBoot ConfigurationProperties Bean

Both required annotation `@DynamicProperties` and `@ConfigurationProperties`.

Sample:
```java
@DynamicProperties
@ConfigurationProperties("custom")
public class CustomProperties {
    
    private String value;
}
```