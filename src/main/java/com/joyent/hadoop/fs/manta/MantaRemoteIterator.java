package com.joyent.hadoop.fs.manta;

import com.joyent.manta.client.MantaDirectoryListingIterator;
import com.joyent.manta.client.MantaObject;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.fs.RemoteIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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
    private final Iterator<MantaObject> inner;

    /**
     * Stream instance with close() method.
     */
    private final Stream<MantaObject> closeableStream;

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
    private AtomicReference<MantaObject> nextRef = new AtomicReference<>();

    /**
     * Flag indicating that we close the underlying resources when true.
     */
    private final boolean autocloseWhenFinished;

    /**
     * Creates a new instance wrapping a {@link MantaDirectoryListingIterator}.
     *
     * @param filter filter object that will filter out results
     * @param stream backing stream
     * @param path base path that is being iterated
     * @param fs reference to the underlying filesystem
     * @param autocloseWhenFinished flag indicate whether or not to close all
     *                              resources when we have finished iterating
     */
    public MantaRemoteIterator(final PathFilter filter,
                               final Stream<MantaObject> stream,
                               final Path path,
                               final FileSystem fs,
                               final boolean autocloseWhenFinished) {
        this.filter = filter;

        if (filter == null) {
            this.inner = stream.iterator();
        } else {
            this.inner = stream.filter(obj -> filter.accept(new Path(obj.getPath()))).iterator();
        }

        this.closeableStream = stream;
        this.path = path;
        this.fs = fs;
        this.autocloseWhenFinished = autocloseWhenFinished;
        this.nextRef.set(nextAcceptable());
    }

    @Override
    public void close() {
        closeableStream.close();
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

        final MantaObject object = nextRef.getAndUpdate(stringObjectMap -> nextAcceptable());
        @SuppressWarnings("unchecked")
        final Path nextPath = new Path(object.getPath());
        final FileStatus status = new MantaFileStatus(object, nextPath);
        final BlockLocation[] locs;

        if (status.isFile()) {
            locs = fs.getFileBlockLocations(status, 0, status.getLen());
        } else {
            locs = null;
        }

        return new LocatedFileStatus(status, locs);
    }

    /**
     * Iterates the wrapped streaming iterator until an acceptable value is found.
     *
     * @return value matching filter or null if no values left in wrapped iterator
     */
    private MantaObject nextAcceptable() {
        while (true) {
            if (!inner.hasNext()) {
                if (autocloseWhenFinished) {
                    closeableStream.close();
                }

                return null;
            }

            return inner.next();
        }
    }
}
