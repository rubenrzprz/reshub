package com.rubenrzprz.reshub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

public class ActorHeaderRequestWrapper extends HttpServletRequestWrapper {

  private final Map<String, String> overrides = new LinkedHashMap<>();
  private final Set<String> suppressed = new LinkedHashSet<>();

  public ActorHeaderRequestWrapper(HttpServletRequest request) {
    super(request);
  }

  public void putHeader(String name, String value) {
    if (value == null) {
      overrides.remove(name);
      suppressed.add(name);
    } else {
      overrides.put(name, value);
      suppressed.remove(name);
    }
  }

  @Override
  public String getHeader(String name) {
    if (overrides.containsKey(name)) {
      return overrides.get(name);
    }
    if (suppressed.contains(name)) {
      return null;
    }
    return super.getHeader(name);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    if (overrides.containsKey(name)) {
      return Collections.enumeration(Collections.singletonList(overrides.get(name)));
    }
    if (suppressed.contains(name)) {
      return Collections.emptyEnumeration();
    }
    return super.getHeaders(name);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    Vector<String> names = new Vector<>();
    Enumeration<String> base = super.getHeaderNames();
    while (base.hasMoreElements()) {
      String header = base.nextElement();
      if (!suppressed.contains(header)) {
        names.add(header);
      }
    }
    for (String key : overrides.keySet()) {
      if (!names.contains(key)) {
        names.add(key);
      }
    }
    return names.elements();
  }
}
