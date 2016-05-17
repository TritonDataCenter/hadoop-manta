package org.apache.hadoop.fs.manta;

import com.joyent.manta.client.MantaSeekableByteChannel;
import org.apache.hadoop.fs.FSInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>{@link FSInputStream} implementation that wraps a {@link MantaSeekableByteChannel}
 * object. This class opportunistically makes use of HTTP Range requests against
 * the Manta API in order to provide random access.</p>
 *
 * <p>This class should be thread-safe. We block all operations resetting the wrapped
 * {@link MantaSeekableByteChannel} object.</p>
 */
public class MantaSeekableInputStream extends FSInputStream {
    /**
     * Constant for when we reposition vs just seek ahead.
     */
    private static long REPOSITION_TOLERENCE = 1_048_576L;

    /**
     * Logger instance.
     */
    public static final Logger LOG =
            LoggerFactory.getLogger(MantaSeekableByteChannel.class);

    private volatile MantaSeekableByteChannel seekableByteChannel;

    public MantaSeekableInputStream(final MantaSeekableByteChannel seekableByteChannel) {
        this.seekableByteChannel = seekableByteChannel;
    }

    @Override
    public synchronized void seek(final long newPos) throws IOException {
        final long currentPos = seekableByteChannel.position();

        // Do nothing if we are at the same position
        if (currentPos == newPos) {
            return;
        }

    /* If we are currently at position zero (we haven't read any data),
     * then it is more efficient to reposition than to skip on the backing
     * InputStream. Or, if the new position is behind the current position,
     * then we reposition because we can't skip backwards.
     */
        if (currentPos < 1 || currentPos > newPos) {
            reposition(newPos);
            return;
        }

        final long skipAheadBytes = newPos - currentPos;

    /* If the new position is sufficiently far away from the current position
     * we reposition rather than skip forwards.
     */
        if (skipAheadBytes >= REPOSITION_TOLERENCE) {
            reposition(newPos);
            return;
        }

    /* If we are within all of the thresholds, we just skip through the
     * backing InputStream. */
        skip(skipAheadBytes);
    }

    /**
     * Reposition uses a HTTP Range request to get an entirely new backing
     * InputStream.
     *
     * @param position position of the underlying stream to fast-forward to
     * @throws IOException thrown when we can't execute the request over the wire
     */
    private void reposition(final long position) throws IOException {
        try {
            seekableByteChannel.close();
        } catch (IOException e){
            LOG.warn("Error closing MantaSeekableByteChannel", e);
        }

        this.seekableByteChannel = (MantaSeekableByteChannel)seekableByteChannel.position(position);
    }

    @Override
    public synchronized long getPos() throws IOException {
        return this.seekableByteChannel.position();
    }

    @Override
    public synchronized boolean seekToNewSource(final long targetPos) throws IOException {
        return false;
    }

    @Override
    public int read() throws IOException {
        return this.seekableByteChannel.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return this.seekableByteChannel.read(b);
    }

    @Override
    public int read(final byte[] b, int off, int len) throws IOException {
        return this.seekableByteChannel.read(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        return this.seekableByteChannel.skip(n);
    }

    @Override
    public int available() throws IOException {
        return this.seekableByteChannel.available();
    }


    @Override
    public void mark(final int readlimit) {
        this.seekableByteChannel.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        this.seekableByteChannel.reset();
    }

    @Override
    public boolean markSupported() {
        return this.seekableByteChannel.markSupported();
    }

    @Override
    public synchronized int read(long position, byte[] buffer, int offset, int length) throws IOException {
        long oldPos = getPos();
        int nread = -1;
        try {
            seek(position);
            nread = read(buffer, offset, length);
        } finally {
            seek(oldPos);
        }

        return nread;
    }

    @Override
    public synchronized void close() throws IOException {
        this.seekableByteChannel.close();
    }
}
