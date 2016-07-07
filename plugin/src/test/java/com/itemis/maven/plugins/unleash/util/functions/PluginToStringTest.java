package com.itemis.maven.plugins.unleash.util.functions;

import org.apache.maven.model.Plugin;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class PluginToStringTest {
  @DataProvider
  public static Object[][] plugins() {
    return new Object[][] { { createPlugin("x", "y", "2.0"), "x:y:2.0" }, { createPlugin("x", "y", null), "x:y" } };
  }

  @Test
  @UseDataProvider("plugins")
  public void testApply(Plugin p, String expected) {
    Assert.assertEquals(expected, PluginToString.INSTANCE.apply(p));
  }

  private static Plugin createPlugin(String gid, String aid, String version) {
    Plugin p = new Plugin();
    p.setGroupId(gid);
    p.setArtifactId(aid);
    p.setVersion(version);
    return p;
  }
}
