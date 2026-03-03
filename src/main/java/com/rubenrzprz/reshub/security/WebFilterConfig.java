package com.rubenrzprz.reshub.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebFilterConfig {

  @Bean
  public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterFilterRegistration(JwtAuthFilter filter) {
    FilterRegistrationBean<JwtAuthFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(filter);
    bean.addUrlPatterns("/reservations/*");
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
  }
}
