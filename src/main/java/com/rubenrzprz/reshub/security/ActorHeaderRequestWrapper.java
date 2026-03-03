package com.rubenrzprz.reshub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

public class ActorHeaderRequestWrapper extends HttpServletRequestWrapper {

  private final Map<String, String> overrides = new LinkedHashMap<>();

  public ActorHeaderRequestWrapper(HttpServletRequest request) {
    super(request);
  }

  public void putHeader(String name, String value) {
    if (value == null) {
      overrides.remove(name);
    } else {
      overrides.put(name, value);
    }
  }

  @Override
  public String getHeader(String name) {
    if (overrides.containsKey(name)) {
      return overrides.get(name);
    }
    return super.getHeader(name);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    if (overrides.containsKey(name)) {
      return Collections.enumeration(Collections.singletonList(overrides.get(name)));
    }
    return super.getHeaders(name);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    Vector<String> names = new Vector<>();
    Enumeration<String> base = super.getHeaderNames();
    while (base.hasMoreElements()) {
      names.add(base.nextElement());
    }
    for (String key : overrides.keySet()) {
      if (!names.contains(key)) {
        names.add(key);
      }
    }
    return names.elements();
  }
}
