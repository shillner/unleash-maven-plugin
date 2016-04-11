package com.itemis.maven.plugins.unleash.scm;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Optional;

public class MavenScmUtil {
  private MavenScmUtil() {
    // no instanciation of this utility class!
  }

  public static Optional<String> calcProviderName(MavenProject project) {
    String providerName = null;

    Scm scm = project.getScm();
    if (scm != null) {
      // takes the developer conncetion first or the connection url if devConnection is empty or null
      String connection = StringUtils.trimToNull(scm.getDeveloperConnection());
      connection = connection != null ? connection : StringUtils.trimToNull(scm.getConnection());

      // scm url format description: https://maven.apache.org/scm/scm-url-format.html
      if (connection != null) {
        // cuts the substring "scm:" at the beginning
        connection = connection.substring(4);

        // as stated in the scm url format description, the provider delimiter may be a colon (:) or a pipe (|) if
        // colons are used otherwise (e.g. for windows paths)

        // svn:http://... -> svn:http://...
        // svn|http://... -> svn
        // svn:http://xyz|... -> svn:http://xyz
        int nextPipe = connection.indexOf('|');
        if (nextPipe > -1) {
          connection = connection.substring(0, nextPipe);
        }

        // svn -> svn
        // svn:http... -> svn
        int nextColon = connection.indexOf(':');
        if (nextColon > -1) {
          providerName = connection.substring(0, nextColon);
        } else {
          providerName = connection;
        }
      }
    }

    return Optional.fromNullable(providerName);
  }
}
