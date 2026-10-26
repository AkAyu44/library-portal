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
// Lab 8: prod validates the schema (ddl-auto: validate); the test database is recreated by every
// context, so this test, which never touches the books, does not ask Hibernate about the schema
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
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
