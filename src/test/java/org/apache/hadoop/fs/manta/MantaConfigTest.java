package org.apache.hadoop.fs.manta;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertTrue;

public class MantaConfigTest {
    @Test
    public void hadoopCanLoadFilesystemFromServiceLoader() throws IOException {
        final Configuration config = new Configuration();

        config.set("manta.user", "testuser");
        config.set("manta.key_id", "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00");

        URI uri = URI.create("manta:///");
        FileSystem fs = FileSystem.get(uri, config);

        assertTrue(String.format("FileSystem is not an instant of %s. Actually: %s",
                MantaFileSystem.class, fs.getClass()), fs instanceof MantaFileSystem);
    }
}
