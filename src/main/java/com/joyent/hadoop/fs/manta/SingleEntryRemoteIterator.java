package com.joyent.hadoop.fs.manta;

import org.apache.hadoop.fs.RemoteIterator;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link RemoteIterator} implementation that only returns a single static value.
 * @param <E> type of value returned from iterator
 */
public class SingleEntryRemoteIterator<E> implements RemoteIterator<E> {
    /**
     * Reference to the single value to iterate and clear.
     */
    private AtomicReference<E> singleValue = new AtomicReference<>();

    /**
     * Create a new instance populated with a single value.
     * @param singleValue single value to populate
     */
    public SingleEntryRemoteIterator(final E singleValue) {
        this.singleValue.set(singleValue);
    }

    @Override
    public boolean hasNext() throws IOException {
        return singleValue.get() != null;
    }

    @Override
    public E next() throws IOException {

        if (singleValue.get() != null) {
            return singleValue.getAndSet(null);
        }

        return null;
    }
}
