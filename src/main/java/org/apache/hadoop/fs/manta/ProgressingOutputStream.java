package org.apache.hadoop.fs.manta;

import org.apache.hadoop.util.Progressable;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 */
public class ProgressingOutputStream extends OutputStream {
    private final Progressable progress;
    private final OutputStream wrapped;

    private volatile long operationCount = Long.MIN_VALUE;
    private final long pingOnNoOfOps = 100L;

    public ProgressingOutputStream(final Progressable progress, final OutputStream wrapped) {
        this.progress = progress;
        this.wrapped = wrapped;
    }

    @Override
    public void write(final int b) throws IOException {
        if (operationCount == 0L || ++operationCount % pingOnNoOfOps == 0L) {
            this.progress.progress();
        }

        wrapped.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        wrapped.write(b);
    }

    @Override
    public void write(byte[] b, final int off, final int len) throws IOException {
        wrapped.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        wrapped.flush();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }
}
