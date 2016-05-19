package org.apache.hadoop.fs.manta;

import com.joyent.manta.client.MantaDirectoryListingIterator;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.fs.RemoteIterator;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of {@link RemoteIterator} that wraps a {@link MantaDirectoryListingIterator}
 * so that we can stream directory listing results without buffering beyond the each HTTP
 * request to Manta.
 */
public class MantaRemoteIterator implements RemoteIterator<LocatedFileStatus>,
        AutoCloseable {
    /**
     * Filter instance that determines if an entry is returned.
     */
    private final PathFilter filter;

    /**
     * Wrapped inner iterator resource stream.
     */
    private final MantaDirectoryListingIterator inner;

    /**
     * Directory root being listed.
     */
    private final Path path;

    /**
     * Reference to the invoking {@link FileSystem} instance.
     */
    private final FileSystem fs;

    /**
     * Look ahead buffer used for prefiltering entries.
     */
    private AtomicReference<Map<String, Object>> nextRef =
            new AtomicReference<>();

    /**
     * Flag indicating that we close the underlying resources when true.
     */
    private final boolean autocloseWhenFinished;

    public MantaRemoteIterator(final PathFilter filter,
                               final MantaDirectoryListingIterator inner,
                               final Path path,
                               final FileSystem fs,
                               final boolean autocloseWhenFinished) {
        this.filter = filter;
        this.inner = inner;
        this.path = path;
        this.fs = fs;
        this.autocloseWhenFinished = autocloseWhenFinished;
        this.nextRef.set(nextAcceptable());
    }

    @Override
    public void close() throws Exception {
        inner.close();
    }

    @Override
    public boolean hasNext() throws IOException {
        return nextRef.get() != null;
    }

    @Override
    public LocatedFileStatus next() throws IOException {
        if (!hasNext()) {
            String msg = String.format("No more listings in [%s]", path);
            throw new NoSuchElementException(msg);
        }

        final Map<String, Object> props = nextRef.getAndUpdate(stringObjectMap -> nextAcceptable());
        @SuppressWarnings("unchecked")
        final Path nextPath = (Path)props.get("path");
        final FileStatus status = new MantaFileStatus(props, nextPath);
        final BlockLocation[] locs;

        if (status.isFile()) {
            locs = fs.getFileBlockLocations(status, 0, status.getLen());
        } else {
            locs = null;
        }

        return new LocatedFileStatus(status, locs);
    }

    /**
     * Builds the {@link Path} of an entry based on its filename.
     *
     * @param props object properties to read filename from
     * @return path instance with the full path to the entry
     */
    private Path findPath(final Map<String, Object> props) {
        if (!props.containsKey("name")) {
            throw new IllegalArgumentException("No filename returned from results");
        }

        String entryName = props.get("name").toString();
        return new Path(this.path, entryName);
    }

    /**
     * Iterates the wrapped streaming iterator until an acceptable value is found.
     *
     * @return value matching filter or null if no values left in wrapped iterator
     */
    private Map<String, Object> nextAcceptable() {
        Map<String, Object> value;

        while (true) {
            if (!inner.hasNext()) {
                if (autocloseWhenFinished) {
                    inner.close();
                }
                return null;
            }

            value = inner.next();
            Path path = findPath(value);

            if (filter == null || filter.accept(path)) {
                /* I hate side-effects, but this is doing a nice micro-optimization
                 * so that we don't have to recreate the Path object twice. */
                value.put("path", path);
                return value;
            }
        }
    }
}
