package kz.iitu.library;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lab 3 + Lab 7: under the prod profile the API stays, but its description and Swagger UI are closed. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProdProfileTests {

    @Autowired MockMvc mvc;

    @Test
    void swaggerIsClosedInProduction() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
        mvc.perform(get("/api/config"))
                .andExpect(jsonPath("$.banner").value("PRODUCTION: handle with care"))
                .andExpect(jsonPath("$.mailRetryCount").value(5));
    }
}
