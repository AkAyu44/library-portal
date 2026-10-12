package kz.iitu.library;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** Lab 3: application-dev.yml overrides application.yml; the dev banner bean is the only one. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevProfileTests {

    @Autowired MockMvc mvc;

    @Test
    void devProfileOverridesSharedValues() throws Exception {
        mvc.perform(get("/api/config"))
                .andExpect(jsonPath("$.serverPort").value("8081"))
                .andExpect(jsonPath("$.mailRetryCount").value(1))
                .andExpect(jsonPath("$.mailEnabled").value(false))
                .andExpect(jsonPath("$.cacheTtl").value("PT30S"))
                .andExpect(jsonPath("$.cacheMaxEntries").value(50))
                .andExpect(jsonPath("$.activeProfiles[0]").value("dev"))
                .andExpect(jsonPath("$.banner").value("DEVELOPMENT: the data is test data"));
    }
}
