package com.itemis.maven.plugins.unleash.util;

import java.util.Map;

import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

/**
 * A resolver for direct and indirect (parents) property references that respects all profiles.<br>
 * The resolve also respects the correct overriding order of the property definitions.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.4.3
 */
public class PomPropertyResolver {
  private MavenProject project;
  private Map<String, String> properties;

  /**
   * A new resolver for property references.
   * 
   * @param project
   *          the project from which to resolve the properties.
   */
  public PomPropertyResolver(MavenProject project) {
    this.project = project;
  }

  /**
   * @return all resolved properties (direct and indirect).
   */
  public Map<String, String> getProperties() {
    resolveAllProperties();
    return properties;
  }

  /**
   * @param key
   *          the property key.
   * @return the value of the property or {@code null} if the key is not known.
   */
  public String getProperty(String key) {
    resolveAllProperties();
    return properties.get(key);
  }

  /**
   * Resolves property references from the input string if there are any.
   * 
   * @param s
   *          the input string which might contain property references.
   * @return the input string with expanded property references.
   */
  public String expandPropertyReferences(String s) {
    resolveAllProperties();
    return resolveReferences(s);
  }

  private void resolveAllProperties() {
    if (properties != null) {
      return;
    }

    properties = Maps.newHashMap();
    // after that add the direct properties to override the ones of the parents
    for (Map.Entry<Object, Object> entry : project.getProperties().entrySet()) {
      properties.put((String) entry.getKey(), (String) entry.getValue());
    }
    for (Profile profile : project.getModel().getProfiles()) {
      for (Map.Entry<Object, Object> entry : profile.getProperties().entrySet()) {
        properties.put((String) entry.getKey(), (String) entry.getValue());
      }
    }
    properties.put("project.version", project.getVersion());
    // Adding all environment variables
    for (Map.Entry<String, String> sysEnv : System.getenv().entrySet()) {
      properties.put("env." + sysEnv.getKey(), sysEnv.getValue());
    }

    // now resolve property references within the properties
    for (String propertyKey : properties.keySet()) {
      properties.put(propertyKey, resolveProperty(propertyKey));
    }
  }

  private String resolveProperty(String key) {
    String value = properties.get(key);
    return resolveReferences(value);
  }

  private String resolveReferences(String s) {
    if (s == null) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    StringBuilder currentKey = null;
    boolean isRef = false;
    for (char c : s.toCharArray()) {
      switch (c) {
        case '$':
          isRef = true;
          currentKey = new StringBuilder();
          break;
        case '{':
          if (!isRef) {
            sb.append(c);
          }
          break;
        case '}':
          if (isRef) {
            sb.append(
                MoreObjects.firstNonNull(resolveProperty(currentKey.toString()), "${" + currentKey.toString() + "}"));
            currentKey = null;
            isRef = false;
          } else {
            sb.append(c);
          }
          break;
        default:
          if (isRef) {
            currentKey.append(c);
          } else {
            sb.append(c);
          }
          break;
      }
    }
    return sb.toString();
  }
}
