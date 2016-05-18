package org.apache.hadoop.fs.manta;

import com.google.common.annotations.VisibleForTesting;
import com.joyent.manta.client.*;
import com.joyent.manta.config.ChainedConfigContext;
import com.joyent.manta.config.ConfigContext;
import com.joyent.manta.config.SystemSettingsConfigContext;
import com.joyent.manta.exception.MantaClientHttpResponseException;
import com.joyent.manta.exception.MantaErrorCode;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.function.Function;

/**
 *
 */
@InterfaceAudience.Public
public class MantaFileSystem extends FileSystem implements AutoCloseable {
    /**
     * Logger instance.
     */
    public static final Logger LOG =
            LoggerFactory.getLogger(MantaFileSystem.class);

    public static final Path HOME_ALIAS_PATH = new Path("~~");

    private Path workingDir;
    private URI uri;
    private ConfigContext config;
    private MantaClient client;

    static {
        LOG.debug("Manta filesystem class loaded");
    }

    public MantaFileSystem() {
        super();
    }

    @VisibleForTesting
    void initialize(final URI name, final ConfigContext config) throws IOException {
        this.config = config;
        this.client = new MantaClient(this.config);
    }

    @Override
    public void initialize(final URI name, final Configuration conf) throws IOException {
        super.initialize(name, conf);

        ChainedConfigContext chained = new ChainedConfigContext(
                new SystemSettingsConfigContext(),
                new HadoopConfigurationContext(conf)
        );

        this.config = chained;
        this.client = new MantaClient(this.config);

        this.uri = URI.create(String.format("%s%s",
                name.getScheme(), name.getAuthority()));

        this.workingDir = new Path(config.getMantaHomeDirectory());
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
        LOG.debug("Opening '{}' for reading.", path);

        final FileStatus fileStatus = getFileStatus(path);

        if (fileStatus.isDirectory()) {
            final String msg = String.format("Can't open %s because it is a directory", path);
            throw new FileNotFoundException(msg);
        }

        String mantaPath = mantaPath(path);

        MantaSeekableByteChannel channel = client.getSeekableByteChannel(mantaPath);
        FSInputStream fsInput = new MantaSeekableInputStream(channel);

        return new FSDataInputStream(fsInput);
    }

    @Override
    public FSDataOutputStream create(final Path path, final FsPermission fsPermission,
                                     final boolean overwrite,
                                     final int bufferSize,
                                     final short replication,
                                     final long blockSize,
                                     final Progressable progressable) throws IOException {
        String mantaPath = mantaPath(path);

        if (!overwrite && client.existsAndIsAccessible(mantaPath)) {
            String msg = String.format("File already exists at path: %s", path);
            throw new FileAlreadyExistsException(msg);
        }

        try (PipedOutputStream out = new PipedOutputStream();
             PipedInputStream in = new PipedInputStream(out);
             ProgressingOutputStream pout = new ProgressingOutputStream(progressable, out)) {

            MantaHttpHeaders headers = new MantaHttpHeaders();

            if (replication > 0) {
                headers.setDurabilityLevel(replication);
            }

            LOG.debug("Creating new file with {} replicas at path: {}", replication, path);

            client.put(mantaPath, in, headers);
            return new FSDataOutputStream(pout, statistics);
        }
    }

    @Override
    public FSDataOutputStream append(final Path path, final int i,
                                     final Progressable progressable) throws IOException {
        throw new IOException("Not supported");
    }

    @Override
    public boolean rename(final Path original, final Path newName) throws IOException {
        String mantaOriginal = mantaPath(original);
        String mantaNewName = mantaPath(newName);

        client.head(mantaOriginal);

        LOG.debug("Renaming [{}] to [{}]", original, newName);

        client.move(mantaOriginal, mantaNewName);

        return true;
    }

    @Override
    public boolean delete(final Path path, final boolean recursive) throws IOException {
        String mantaPath = mantaPath(path);

        if (recursive) {
            LOG.debug("Recursively deleting path: {}");
            client.delete(mantaPath);
        } else {
            LOG.debug("Deleting path: {}");
            client.deleteRecursive(mantaPath);
        }

        return true;
    }

    @Override
    public FileStatus[] listStatus(final Path path) throws FileNotFoundException, IOException {
        LOG.debug("List status for path: {}", path);
        String mantaPath = mantaPath(path);

        if (!client.existsAndIsAccessible(mantaPath)) {
            throw new FileNotFoundException(mantaPath);
        }

        return client.listObjects(mantaPath)
                .map((Function<MantaObject, FileStatus>) MantaFileStatus::new)
                .toArray(FileStatus[]::new);
    }

    @Override
    protected RemoteIterator<LocatedFileStatus> listLocatedStatus(
            final Path path, final PathFilter filter) throws FileNotFoundException, IOException {
        LOG.debug("List located status for path: {}", path);

        String mantaPath = mantaPath(path);

        if (!client.existsAndIsAccessible(mantaPath)) {
            throw new FileNotFoundException(mantaPath);
        }

        MantaDirectoryListingIterator itr = client.streamingIterator(mantaPath);
        return new MantaRemoteIterator(filter, itr, path, this, true);
    }

    @Override
    public void setWorkingDirectory(final Path path) {
        this.workingDir = path;
    }

    @Override
    public Path getWorkingDirectory() {
        return this.workingDir;
    }

    @Override
    public Path getHomeDirectory() {
        return new Path(config.getMantaHomeDirectory());
    }

    @Override
    public boolean mkdirs(final Path path, final FsPermission fsPermission) throws IOException {
        String mantaPath = mantaPath(path);
        return client.putDirectory(mantaPath);
    }

    @Override
    public FileStatus getFileStatus(final Path path) throws IOException {
        String mantaPath = mantaPath(path);
        LOG.debug("Getting path status for: {}", mantaPath);

        MantaObjectResponse response = client.head(mantaPath);
        return new MantaFileStatus(response, path);
    }

    @Override
    public boolean exists(final Path path) throws IOException {
        return client.existsAndIsAccessible(mantaPath(path));
    }

    @Override
    public boolean isDirectory(final Path path) throws IOException {
        try {
            return client.head(mantaPath(path)).isDirectory();
        } catch (MantaClientHttpResponseException e) {
            /* We imitate the behavior of FileSystem.isDirectory, by changing a
             * FileNotFoundException into a false return value. */
            if (e.getServerCode().equals(MantaErrorCode.RESOURCE_NOT_FOUND_ERROR)) {
                return false;
            }

            throw e;
        }
    }

    @Override
    public boolean isFile(final Path path) throws IOException {
        return !isDirectory(path);
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            client.closeQuietly();
        }
    }

    private String mantaPath(final Path path) {
        if (path.getParent() == null) {
            return path.toString();
        }

        if (path.getParent().equals(HOME_ALIAS_PATH)) {
            String base = StringUtils.removeStart(path.toString().substring(2), "/");

            return String.format("%s/%s", config.getMantaHomeDirectory(), base);
        }

        final Path working = getWorkingDirectory();

        if (working != null) {
            return new Path(working, path).toString();
        }

        throw new IllegalArgumentException(String.format("Invalid path: %s", path));
    }
}
