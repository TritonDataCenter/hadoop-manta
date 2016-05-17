package org.apache.hadoop.fs.manta;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

public class MantaFileSystemIT {
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
        try (MantaFileSystem fs = instance()) {
//            fs.setWorkingDirectory(fs.getHomeDirectory());
            Path path = new Path("stor");
            FileStatus[] statuses = fs.listStatus(path);

            for (FileStatus status : statuses) {
                System.out.println(status);
            }
        }
    }

    @Test
    public void canListMantaDirectory() throws IOException {
        try (MantaFileSystem fs = instance()) {
            Path path = new Path("/elijah.zupancic/stor/");
            RemoteIterator<LocatedFileStatus> itr = fs.listFiles(path, false);

            while (itr.hasNext()) {
                LocatedFileStatus next = itr.next();

                System.out.println(next);
            }
        }
    }
}
