package org.apache.hadoop.fs.manta;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.config.ConfigContext;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.UUID;

public class MantaFileSystemIT {
    private String testPathPrefix;
    private MantaFileSystem fs;
    private MantaClient client;
    private ConfigContext config;

    @Before
    public void setup() throws IOException {
        this.fs = instance();
        this.client = this.fs.getMantaClient();
        this.config = this.fs.getConfig();

        testPathPrefix = String.format("%s/stor/%s/",
                config.getMantaHomeDirectory(), UUID.randomUUID());
        client.putDirectory(testPathPrefix);
    }

    @After
    public void cleanup() throws IOException {
        if (client != null) {
            client.deleteRecursive(testPathPrefix);
            client.closeQuietly();
        }
    }

    private static MantaFileSystem instance() {
        final Configuration config = new Configuration();
        URI uri = URI.create("manta:///");

        try {
            FileSystem fs = FileSystem.get(uri, config);
            @SuppressWarnings("unchecked")
            MantaFileSystem mfs = (MantaFileSystem)fs;

            return mfs;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void canListStatus() throws IOException {

        Path path = new Path("stor");
        FileStatus[] statuses = fs.listStatus(path);

        for (FileStatus status : statuses) {
            System.out.println(status);
        }
    }

    @Test
    public void canListMantaDirectory() throws IOException {
        Path path = new Path("/elijah.zupancic/stor/");
        RemoteIterator<LocatedFileStatus> itr = fs.listFiles(path, false);

        while (itr.hasNext()) {
            LocatedFileStatus next = itr.next();

            System.out.println(next);
        }
    }

    @Test
    public void canMakeDirectory() throws IOException {
        Path newDirectory = new Path("stor/newDirectory");
        fs.mkdirs(newDirectory);
    }

    @Test
    public void isFile() throws IOException {
        fs.isFile(new Path("stor/hello.txt"));
    }
}
