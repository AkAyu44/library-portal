package kz.iitu.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Lab 8, task 4: a Page is written as a stable JSON shape —
 * {"content": [...], "page": {"size", "number", "totalElements", "totalPages"}} —
 * instead of the internal PageImpl, whose fields Spring Data does not promise to keep.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class PagingConfig {
}
