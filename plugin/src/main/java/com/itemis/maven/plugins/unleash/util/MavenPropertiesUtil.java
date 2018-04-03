package com.itemis.maven.plugins.unleash.util;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mathieu Debove <mad@teecu.be>
 */
public class MavenPropertiesUtil {

    private final MavenProject project;
    private final MavenSession session;
    private final Settings settings;
    private static final Pattern mavenPropertyPattern = Pattern.compile("\\$\\{([^}]*)\\}"); // ${prop} regex pattern

    public MavenPropertiesUtil(MavenProject project, MavenSession session, Settings settings) {
        this.project = project;
        this.session = session;
        this.settings = settings;
    }

    @SuppressWarnings("unchecked") // because of Maven poor typing
    public String getPropertyValueInSettings(String propertyName, Settings settings) {
        if (settings == null) {
            return null;
        }

        List<String> activeProfiles = settings.getActiveProfiles();

        for (Object _profileWithId : settings.getProfilesAsMap().entrySet()) {
            Map.Entry<String, Profile> profileWithId = (Map.Entry<String, Profile>) _profileWithId;
            if (activeProfiles.contains(profileWithId.getKey())) {
                Profile profile = profileWithId.getValue();

                String value = profile.getProperties().getProperty(propertyName);
                if (value != null) {
                    return value;
                }
            }
        }

        return null;
    }

    private List<String> getActiveProfiles(Settings settings) {
        if (settings == null) return null;

        List<String> result = settings.getActiveProfiles();
        if (result == null) {
            result = new ArrayList<String>();
        }

        if (settings.getProfiles() != null) {
            for (Profile profile : settings.getProfiles()) {
                if (!result.contains(profile.getId())) {
                    if (profile.getActivation() != null && profile.getActivation().isActiveByDefault()) {
                        result.add(profile.getId());
                    }
                }
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked") // because of Maven poor typing
    public boolean propertyExistsInSettings(String propertyName, Settings settings) {
        if (settings == null) {
            return false;
        }

        List<String> activeProfiles = getActiveProfiles(settings);

        for (Object _profileWithId : settings.getProfilesAsMap().entrySet()) {
            Map.Entry<String, Profile> profileWithId = (Map.Entry<String, Profile>) _profileWithId;
            if (activeProfiles.contains(profileWithId.getKey())) {
                Profile profile = profileWithId.getValue();

                boolean result = profile.getProperties().containsKey(propertyName);
                if (result) {
                    return result;
                }
            }
        }

        return false;
    }

    private String getPropertyValueInCommandLine(String propertyName, MavenSession session) {
        if (session == null) {
            return null;
        }

        return session.getRequest().getUserProperties().getProperty(propertyName);
    }

    public boolean propertyExistsInSettings(String propertyName) {
        return propertyExistsInSettings(propertyName, session.getSettings());
    }

    public boolean propertyExists(String propertyName) {
        return propertyExists(project, propertyName);
    }

    public boolean propertyExists(MavenProject mavenProject, String propertyName) {
        return mavenProject.getOriginalModel().getProperties().containsKey(propertyName) ||
                mavenProject.getModel().getProperties().containsKey(propertyName) ||
                session.getRequest().getUserProperties().containsKey(propertyName) ||
                propertyExistsInSettings(propertyName, session.getSettings());
    }

    private String getPropertyValueInOriginalModel(Model originalModel, String propertyName, List<org.apache.maven.model.Profile> activeProfiles) {
        if (originalModel == null || propertyName == null) return null;

        String result = originalModel.getProperties().getProperty(propertyName);

        if (result == null && activeProfiles != null) {
            for (org.apache.maven.model.Profile profile : originalModel.getProfiles()) {
                if (activeProfiles.contains(profile)) {
                    result = profile.getProperties().getProperty(propertyName);
                }
            }
        }

        return result;
    }
    public String getPropertyValue(MavenProject mavenProject, String propertyName, boolean lookInSettingsProperties, boolean lookInCommandLine, boolean onlyInOriginalModel) {
        if (mavenProject == null) return null;
        String result = null;

        if (onlyInOriginalModel) {
//			result = mavenProject.getOriginalModel().getProperties().getProperty(propertyName);
            result = getPropertyValueInOriginalModel(mavenProject.getOriginalModel(), propertyName, mavenProject.getActiveProfiles());
        } else {
            result = mavenProject.getModel().getProperties().getProperty(propertyName);
        }
        if (lookInCommandLine && (result == null || result.isEmpty())) {
            boolean wasEmpty = result != null && result.isEmpty();
            result = getPropertyValueInCommandLine(propertyName, session);
            if (result == null && wasEmpty) {
                result = "";
            }
        }
        if (lookInSettingsProperties && (result == null || result.isEmpty())) {
            boolean wasEmpty = result != null && result.isEmpty();
            result = getPropertyValueInSettings(propertyName, settings);
            if (result == null && wasEmpty) {
                result = "";
            }
        }

        if (result == null && ("basedir".equals(propertyName) || "project.basedir".equals(propertyName))) {
            if (mavenProject.getFile() != null && mavenProject.getFile().getParentFile() != null && mavenProject.getFile().getParentFile().isDirectory()) {
                result = mavenProject.getFile().getParentFile().getAbsolutePath();
            }
        } else if (result == null && ("project.groupId".equals(propertyName))) {
            result = mavenProject.getGroupId();
        } else if (result == null && ("project.artifactId".equals(propertyName))) {
            result = mavenProject.getArtifactId();
        } else if (result == null && ("project.version".equals(propertyName))) {
            result = mavenProject.getVersion();
        } else if (result == null && ("user.home".equals(propertyName))) {
            result = System.getProperty("user.home");
        }

        return result;
    }

    public String getPropertyValue(String propertyName, boolean onlyInOriginalModel) {
        return getPropertyValue(project, propertyName, true, true, onlyInOriginalModel);
    }

    public String getPropertyValue(String propertyName) {
        return getPropertyValue(propertyName, false);
    }

    public String getRootProjectProperty(MavenProject mavenProject, String propertyName) {
        return mavenProject == null ? "" : (mavenProject.getParent() == null ? getPropertyValue(mavenProject, propertyName, false, false, false) : getRootProjectProperty(mavenProject.getParent(), propertyName));
    }

    public String getRootProjectProperty(MavenProject mavenProject, String propertyName, boolean onlyInOriginalModel) {
        return mavenProject == null ? "" : (mavenProject.getParent() == null ? getPropertyValue(mavenProject, propertyName, false, false, onlyInOriginalModel) : getRootProjectProperty(mavenProject.getParent(), propertyName, onlyInOriginalModel));
    }

    public String getPropertyValue(String modelPropertyName, boolean propertyInRootProject, boolean onlyInOriginalModel, boolean lookInSettings) {
        String value = null;
        if (lookInSettings) {
            value = getPropertyValueInSettings(modelPropertyName, settings);
        }
        if (value == null) {
            if (propertyInRootProject) {
                value = getRootProjectProperty(project, modelPropertyName, onlyInOriginalModel);
            } else {
                value = getPropertyValue(modelPropertyName, onlyInOriginalModel);
            }
        }
        return value;
    }

    public String replaceProperties(String string) {
        if (string == null) return null;

        Matcher m = mavenPropertyPattern.matcher(string);

        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String propertyKey = m.group(1);
            String propertyValue = getPropertyValue(propertyKey);
            if (propertyValue != null) {
                m.appendReplacement(sb, Matcher.quoteReplacement(propertyValue));
            }
        }
        m.appendTail(sb);
        string = sb.toString();

        return string;
    }

    public String replaceProperty(String string, String propertyKey, String propertyValue) {
        if (string == null || propertyKey == null || propertyValue == null) return null;

        Matcher m = Pattern.compile("\\$\\{" + propertyKey + "\\}").matcher(string);

        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(propertyValue));
        }
        m.appendTail(sb);
        string = sb.toString();

        return string;
    }

}
