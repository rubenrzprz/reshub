package com.rubenrzprz.reshub.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.rubenrzprz.reshub.health.HealthController;
import com.rubenrzprz.reshub.security.JwtService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
  "spring.application.name=reshub-test"
})
class HealthApiIT {

  @Value("${spring.application.name}")
  String appName;

  @Autowired
  MockMvc mvc;

  @MockitoBean
  JwtService jwtService;

  @Test
  void health_returns_ok_with_configured_app_name() throws Exception {
    mvc.perform(get("/health"))
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.app").value(appName))
      .andExpect(jsonPath("$.status").value("ok"));
  }
}
