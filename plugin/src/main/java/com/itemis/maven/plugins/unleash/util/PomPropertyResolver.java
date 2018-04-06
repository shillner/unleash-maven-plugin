package com.itemis.maven.plugins.unleash.util;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

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
  private List<String> profiles;
  private Settings settings;
  private Properties additionalProperties;

  /**
   * A new resolver for property references.
   *
   * @param project
   *          the project from which to resolve the properties.
   * @param profiles
   *          the profiles that shall be respected.
   */
  public PomPropertyResolver(MavenProject project, Settings settings, List<String> profiles,
      Properties additionalProperties) {
    this.project = project;
    this.profiles = profiles;
    this.settings = settings;
    this.additionalProperties = additionalProperties;
  }

  /**
   * @return all resolved properties (direct and indirect).
   */
  public Map<String, String> getProperties() {
    resolveAllProperties();
    return this.properties;
  }

  /**
   * @param key
   *          the property key.
   * @return the value of the property or {@code null} if the key is not known.
   */
  public String getProperty(String key) {
    resolveAllProperties();
    return this.properties.get(key);
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
    if (this.properties != null) {
      return;
    }
    this.properties = Maps.newHashMap();

    // 1. get the properties of all profiles from the settings.
    for (org.apache.maven.settings.Profile profile : this.settings.getProfiles()) {
      if (!this.profiles.contains(profile.getId())) {
        continue;
      }
      for (Map.Entry<Object, Object> entry : profile.getProperties().entrySet()) {
        this.properties.put((String) entry.getKey(), (String) entry.getValue());
      }
    }

    // 2. after that add the direct properties to override the ones of the parents
    for (Map.Entry<Object, Object> entry : this.project.getProperties().entrySet()) {
      this.properties.put((String) entry.getKey(), (String) entry.getValue());
    }

    // 3. get all properties of all activated profiles
    for (Profile profile : this.project.getModel().getProfiles()) {
      if (!this.profiles.contains(profile.getId())) {
        continue;
      }
      for (Map.Entry<Object, Object> entry : profile.getProperties().entrySet()) {
        this.properties.put((String) entry.getKey(), (String) entry.getValue());
      }
    }

    // 4. Adding all environment variables
    for (Map.Entry<String, String> sysEnv : System.getenv().entrySet()) {
      this.properties.put("env." + sysEnv.getKey(), sysEnv.getValue());
    }

    // 5. Adding all command line properties
    this.additionalProperties.forEach((k, v) -> this.properties.put((String) k, (String) v));

    // 6. special properties
    this.properties.put("project.version", this.project.getVersion());

    // now resolve property references within the properties
    for (String propertyKey : this.properties.keySet()) {
      this.properties.put(propertyKey, resolveProperty(propertyKey));
    }
  }

  private String resolveProperty(String key) {
    String value = this.properties.get(key);
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
