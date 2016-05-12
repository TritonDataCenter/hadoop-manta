package org.apache.hadoop.fs.manta;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

/**
 *
 */
public class MantaFileSystem extends FileSystem {
    /**
     * Logger instance.
     */
    public static final Logger LOG = LoggerFactory.getLogger(MantaFileSystem.class);

    @Override
    public URI getUri() {
        return null;
    }

    @Override
    public FSDataInputStream open(final Path path, final int i) throws IOException {
        return null;
    }

    @Override
    public FSDataOutputStream create(final Path path, final FsPermission fsPermission,
                                     final boolean b, final int i, final short i1, final long l,
                                     final Progressable progressable) throws IOException {
        return null;
    }

    @Override
    public FSDataOutputStream append(final Path path, final int i, final Progressable progressable) throws IOException {
        return null;
    }

    @Override
    public boolean rename(final Path path, final Path path1) throws IOException {
        return false;
    }

    @Override
    public boolean delete(final Path path, final boolean b) throws IOException {
        return false;
    }

    @Override
    public FileStatus[] listStatus(final Path path) throws FileNotFoundException, IOException {
        return new FileStatus[0];
    }

    @Override
    public void setWorkingDirectory(final Path path) {

    }

    @Override
    public Path getWorkingDirectory() {
        return null;
    }

    @Override
    public boolean mkdirs(final Path path, final FsPermission fsPermission) throws IOException {
        return false;
    }

    @Override
    public FileStatus getFileStatus(final Path path) throws IOException {
        return null;
    }
}
