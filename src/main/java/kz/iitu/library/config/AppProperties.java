package kz.iitu.library.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Lab 3: typed, validated configuration (prefix "app").
 * An invalid value (e.g. app.mail.retry-count=99) stops the application at startup.
 */
@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(
        @NotBlank String owner,
        @NotBlank String group,
        String datePattern,
        @Valid Mail mail,
        @Valid Cache cache
) {

    public record Mail(
            @NotBlank @Email String from,
            @Min(1) @Max(10) int retryCount,
            @DefaultValue("5s") Duration timeout,
            @DefaultValue("true") boolean enabled
    ) {}

    /** Lab 3, variant 7: app.cache — now really used by CitationCache. */
    public record Cache(
            @DefaultValue("1m") Duration ttl,
            @Min(10) @Max(10000) int maxEntries
    ) {}
}
