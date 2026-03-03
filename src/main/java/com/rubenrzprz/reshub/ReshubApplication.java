package com.rubenrzprz.reshub;

import com.rubenrzprz.reshub.security.JwtProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(JwtProperties.class)
@OpenAPIDefinition(
  info = @Info(
    title = "ResHub API",
    version = "0.1.0",
    description = "Multi-tenant hotel reservation API with JWT/RBAC, search & exports."
  )
)
@SpringBootApplication
public class ReshubApplication {
  public static void main(String[] args) {
    SpringApplication.run(ReshubApplication.class, args);
  }
}
