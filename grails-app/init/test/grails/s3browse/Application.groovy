package test.grails.s3browse

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource

@Slf4j
class Application extends GrailsAutoConfiguration implements EnvironmentAware {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Override
    void setEnvironment(Environment environment) {
        String configFile = System.getenv('CONFIG_FILE') ?: System.getProperty('configFile')

        log.info "Use config file: $configFile"

        File cfg = new File(configFile)
        if (cfg.exists()) {
            log.info "Loading configuration file: ${configFile}"
            Resource cfgResource = new FileSystemResource(cfg)
            YamlPropertiesFactoryBean ypfb = new YamlPropertiesFactoryBean()
            ypfb.setResources(cfgResource)
            ypfb.afterPropertiesSet()
            Properties properties = ypfb.getObject()
            ((ConfigurableEnvironment)environment).propertySources.addFirst(new PropertiesPropertySource("config.path", properties))
        } else {
            log.info "Config file not found"
        }

    }

}