package com.rubenrzprz.reshub.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ActorHeaderRequestWrapperTest {

  @Test
  void nullOverrideSuppressesOriginalHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Hotel-Id", "spoofed-value");

    ActorHeaderRequestWrapper wrapper = new ActorHeaderRequestWrapper(request);
    wrapper.putHeader("X-Hotel-Id", null);

    Assertions.assertNull(wrapper.getHeader("X-Hotel-Id"));
    Assertions.assertFalse(wrapper.getHeaders("X-Hotel-Id").hasMoreElements());
  }
}
