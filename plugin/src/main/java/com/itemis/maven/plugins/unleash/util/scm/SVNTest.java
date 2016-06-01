package com.itemis.maven.plugins.unleash.util.scm;

import java.io.File;
import java.util.logging.Logger;

import com.google.common.base.Optional;
import com.itemis.maven.plugins.unleash.scm.ScmProvider;
import com.itemis.maven.plugins.unleash.scm.providers.ScmProviderSVN;

public class SVNTest {
  public static void main(String[] args) {
    ScmProvider p = new ScmProviderSVN();
    p.initialize(new File("C:/Users/Stanley/itemis/Projekte/VOEB/unleash-maven-plugin/ws/release-test1"),
        Optional.<Logger> absent(), Optional.<String> absent(), Optional.<String> absent());

    // RevertCommitsRequest r = RevertCommitsRequest.builder().fromRevision("82").toRevision("80").merge()
    // .mergeClient(new ScmPomVersionsMergeClient()).build();
    // String newRevision = p.revertCommits(r);
    System.out.println(p.getLatestRemoteRevision());
  }
}
