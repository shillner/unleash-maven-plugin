package com.itemis.maven.plugins.unleash.util.functions;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

@RunWith(DataProviderRunner.class)
public class FileToRelativePathTest {
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  @DataProvider({ "test", "1/2/3/xyz.txt" })
  public void testApply(String path) throws IOException {
    File base = this.tempFolder.newFolder();
    Assert.assertEquals(path, new FileToRelativePath(base).apply(new File(base, path)));
  }
}
