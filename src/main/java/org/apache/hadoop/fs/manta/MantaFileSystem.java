package org.apache.hadoop.fs.manta;

import com.joyent.manta.config.ConfigContext;
import org.apache.hadoop.classification.InterfaceAudience;
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
@InterfaceAudience.Public
public class MantaFileSystem extends FileSystem {
    /**
     * Logger instance.
     */
    public static final Logger LOG =
            LoggerFactory.getLogger(MantaFileSystem.class);

    static final String PATH_DELIMITER = Path.SEPARATOR;
    private static final int MANTA_MAX_LISTING_LENGTH = 1000;

    private URI uri;
    private ConfigContext config;

    public MantaFileSystem(final ConfigContext config) {
        this.config = config;
    }

    /**
     * Return the protocol scheme for the FileSystem.
     *
     * @return "manta"
     */
    public String getScheme() {
        return "manta";
    }

    @Override
    public URI getUri() {
        return this.uri;
    }

    @Override
    public FSDataInputStream open(final Path path, final int i) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Opening '{}' for reading.", path);
        }
        final FileStatus fileStatus = getFileStatus(path);

        if (fileStatus.isDirectory()) {
            final String msg = String.format("Can't open %s because it is a directory", path);
            throw new FileNotFoundException(msg);
        }

        return null;

//        return new FSDataInputStream(new S3AInputStream(bucket, pathToKey(f),
//                fileStatus.getLen(), s3, statistics));
    }

    @Override
    public FSDataOutputStream create(final Path path, final FsPermission fsPermission,
                                     final boolean b, final int i, final short i1, final long l,
                                     final Progressable progressable) throws IOException {
        return null;
    }

    @Override
    public FSDataOutputStream append(final Path path, final int i,
                                     final Progressable progressable) throws IOException {
        throw new IOException("Not supported");
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
