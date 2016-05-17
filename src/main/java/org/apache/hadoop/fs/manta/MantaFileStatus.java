package org.apache.hadoop.fs.manta;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.joyent.manta.client.MantaObject;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

/**
 *
 */
public class MantaFileStatus extends FileStatus {
    private static final long UNKNOWN_BLOCK_SIZE = 0L;
    private static final long UNKNOWN_MOD_TIME = 0L;

    public MantaFileStatus(final MantaObject mantaObject) {
        this(mantaObject, new Path(mantaObject.getPath()));
    }

    public MantaFileStatus(final MantaObject mantaObject, final Path path) {
        super(length(mantaObject), mantaObject.isDirectory(),
              replicationFactor(mantaObject), UNKNOWN_BLOCK_SIZE,
              modificationTime(mantaObject), path);
    }

    private static long length(final MantaObject mantaObject) {
        Preconditions.checkNotNull(mantaObject);

        return Objects.firstNonNull(mantaObject.getContentLength(), -1L);
    }

    private static short replicationFactor(final MantaObject mantaObject) {
        Preconditions.checkNotNull(mantaObject);

        final Integer durability = mantaObject.getHttpHeaders().getDurabilityLevel();

        if (durability == null) {
            return (short)1;
        }

        return durability.shortValue();
    }

    private static long modificationTime(final MantaObject mantaObject) {
        Preconditions.checkNotNull(mantaObject);

        if (mantaObject.getLastModifiedTime() == null) {
            return UNKNOWN_MOD_TIME;
        }

        return mantaObject.getLastModifiedTime().getTime();
    }
}
