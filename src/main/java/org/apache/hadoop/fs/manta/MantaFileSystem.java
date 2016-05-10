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
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileSystem extends FileSystem {
    public static final Logger LOG = LoggerFactory.getLogger(MantaFileSystem.class);

    @Override
    public URI getUri() {
        return null;
    }

    @Override
    public FSDataInputStream open(Path path, int i) throws IOException {
        return null;
    }

    @Override
    public FSDataOutputStream create(Path path, FsPermission fsPermission,
                                     boolean b, int i, short i1, long l,
                                     Progressable progressable) throws IOException {
        return null;
    }

    @Override
    public FSDataOutputStream append(Path path, int i, Progressable progressable) throws IOException {
        return null;
    }

    @Override
    public boolean rename(Path path, Path path1) throws IOException {
        return false;
    }

    @Override
    public boolean delete(Path path, boolean b) throws IOException {
        return false;
    }

    @Override
    public FileStatus[] listStatus(Path path) throws FileNotFoundException, IOException {
        return new FileStatus[0];
    }

    @Override
    public void setWorkingDirectory(Path path) {

    }

    @Override
    public Path getWorkingDirectory() {
        return null;
    }

    @Override
    public boolean mkdirs(Path path, FsPermission fsPermission) throws IOException {
        return false;
    }

    @Override
    public FileStatus getFileStatus(Path path) throws IOException {
        return null;
    }
}
