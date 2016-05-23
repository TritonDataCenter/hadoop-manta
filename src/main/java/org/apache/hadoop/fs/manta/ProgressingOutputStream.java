package org.apache.hadoop.fs.manta;

import com.google.common.base.Preconditions;
import org.apache.hadoop.util.Progressable;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link OutputStream} wrapper that periodically makes calls to a
 * {@link Progressable} implementation to indicate that streaming is continuing.
 */
public class ProgressingOutputStream extends OutputStream {
    /**
     * Interval in which to wait before sending another progress update.
     */
    private static final long PROGRESS_INTERVAL_MILLIS = 500L;

    /**
     * Progress object.
     */
    private final Progressable progress;

    /**
     * Wrapped output stream.
     */
    private final OutputStream wrapped;

    /**
     * The last time in which the timestamp was updated.
     */
    private volatile long lastTimeMillis = Long.MIN_VALUE;

    /**
     * Create a new instance.
     *
     * @param progress progress object
     * @param wrapped wrapped output stream
     */
    public ProgressingOutputStream(final Progressable progress,
                                   final OutputStream wrapped) {
        Preconditions.checkNotNull(progress);
        this.progress = progress;
        this.wrapped = wrapped;

        // Always call progress at least once
        updateProgress();
    }

    @Override
    public void write(final int buff) throws IOException {
        updateProgress();
        wrapped.write(buff);
    }

    @Override
    public void write(final byte[] buff) throws IOException {
        updateProgress();
        wrapped.write(buff);
    }

    @Override
    public void write(final byte[] buff, final int off, final int len) throws IOException {
        updateProgress();
        wrapped.write(buff, off, len);
    }

    @Override
    public void flush() throws IOException {
        updateProgress();
        wrapped.flush();
    }

    @Override
    public void close() throws IOException {
        updateProgress();
        wrapped.close();
    }

    /**
     * Checks to see if we have exceeded our update interval and if we have,
     * then we update the progress.
     */
    protected synchronized void updateProgress() {
        final boolean update;
        final long now = System.currentTimeMillis();

        if (now <= lastTimeMillis) {
            update = false;
        } else {
            update = now - lastTimeMillis > PROGRESS_INTERVAL_MILLIS;
        }

        this.lastTimeMillis = now;

        if (update) {
            this.progress.progress();
        }
    }
}
