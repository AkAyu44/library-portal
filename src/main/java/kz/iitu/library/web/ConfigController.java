package kz.iitu.library.web;

import kz.iitu.library.config.AppProperties;
import kz.iitu.library.config.EnvironmentBanner;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lab 3: shows the resolved configuration, the active profile and the profile-scoped banner. */
@RestController
@RequestMapping("/api")
public class ConfigController {

    private final AppProperties props;
    private final EnvironmentBanner banner;
    private final Environment environment;

    public ConfigController(AppProperties props, EnvironmentBanner banner, Environment environment) {
        this.props = props;
        this.banner = banner;
        this.environment = environment;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("owner", props.owner());
        result.put("group", props.group());
        result.put("mailFrom", props.mail().from());
        result.put("mailRetryCount", props.mail().retryCount());
        result.put("mailTimeout", props.mail().timeout().toString());
        result.put("mailEnabled", props.mail().enabled());
        result.put("cacheTtl", props.cache().ttl().toString());
        result.put("cacheMaxEntries", props.cache().maxEntries());
        result.put("serverPort", environment.getProperty("server.port"));
        result.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        result.put("banner", banner.describe());
        return result;
    }
}
