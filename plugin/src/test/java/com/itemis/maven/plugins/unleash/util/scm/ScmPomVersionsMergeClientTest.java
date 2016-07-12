package com.itemis.maven.plugins.unleash.util.scm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.itemis.maven.plugins.unleash.scm.merge.MergeClient;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ScmPomVersionsMergeClientTest {
  @DataProvider
  public static Object[][] merge() {
    int numTestResources = 5;
    Object[][] data = new Object[numTestResources][4];
    for (int i = 1; i <= numTestResources; i++) {
      data[i - 1] = new Object[] { getTestResource(i, TestResource.LOCAL), getTestResource(i, TestResource.REMOTE),
          getTestResource(i, TestResource.BASE), getTestResource(i, TestResource.EXPECTED) };
    }
    return data;
  }

  @Test
  @UseDataProvider("merge")
  public void testMerge(InputStream local, InputStream remote, InputStream base, InputStream expected)
      throws IOException {
    MergeClient mergeClient = new ScmPomVersionsMergeClient();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      mergeClient.merge(local, remote, base, os);
      String result = os.toString();
      Assert.assertEquals(new String(ByteStreams.toByteArray(expected)), result);
    } finally {
      Closeables.closeQuietly(local);
      Closeables.closeQuietly(remote);
      Closeables.closeQuietly(base);
      Closeables.close(os, true);
    }
  }

  private static InputStream getTestResource(int testNumber, TestResource resource) {
    return ScmPomVersionsMergeClientTest.class.getResourceAsStream(
        ScmPomVersionsMergeClientTest.class.getSimpleName() + "/" + testNumber + "/" + resource.getName());
  }

  private enum TestResource {
    BASE("pom_base.xml"), EXPECTED("pom_expected.xml"), LOCAL("pom_local.xml"), REMOTE("pom_remote.xml");

    private String resourceName;

    private TestResource(String resourceName) {
      this.resourceName = resourceName;
    }

    public String getName() {
      return this.resourceName;
    }
  }
}
