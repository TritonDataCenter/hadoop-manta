package org.apache.hadoop.fs.manta;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.joyent.manta.client.MantaObject;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 *
 */
public class MantaFileStatus extends FileStatus {
    private static final Logger LOG = LoggerFactory.getLogger(MantaFileStatus.class);

    private static final long UNKNOWN_BLOCK_SIZE = 0L;
    private static final long UNKNOWN_MOD_TIME = 0L;
    private static final long UNKNOWN_LENGTH = 1L;
    private static final short UNKNOWN_REPLICATION_FACTOR = 1;
    private static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    private static final DateFormat TIMESTAMP_PARSER =
            new SimpleDateFormat(ISO_8601_FORMAT);

    public MantaFileStatus(final MantaObject mantaObject) {
        this(mantaObject, new Path(mantaObject.getPath()));
    }

    public MantaFileStatus(final MantaObject mantaObject, final Path path) {
        super(length(mantaObject), mantaObject.isDirectory(),
              replicationFactor(mantaObject), UNKNOWN_BLOCK_SIZE,
              modificationTime(mantaObject), path);
    }

    public MantaFileStatus(final Map<String, Object> props, final Path path) {
        super(length(props), isDirectory(props), replicationFactor(props),
                UNKNOWN_BLOCK_SIZE, modificationTime(props), path);
    }

    private static long length(final MantaObject mantaObject) {
        Preconditions.checkNotNull(mantaObject);

        return Objects.firstNonNull(mantaObject.getContentLength(), UNKNOWN_LENGTH);
    }

    private static long length(final Map<String, Object> props) {
        Preconditions.checkNotNull(props);

        @SuppressWarnings("unchecked")
        Number number = (Number)props.get("size");

        if (number == null) {
            return UNKNOWN_LENGTH;
        }
        return number.longValue();
    }

    private static boolean isDirectory(final Map<String, Object> props) {
        Preconditions.checkNotNull(props);

        Object type = props.get("type");

        if (type == null) {
            return false;
        }

        return type.toString().equals("directory");
    }

    private static short replicationFactor(final MantaObject mantaObject) {
        Preconditions.checkNotNull(mantaObject);

        final Integer durability = mantaObject.getHttpHeaders().getDurabilityLevel();

        if (durability == null) {
            return UNKNOWN_REPLICATION_FACTOR;
        }

        return durability.shortValue();
    }

    private static short replicationFactor(final Map<String, Object> props) {
        Preconditions.checkNotNull(props);

        @SuppressWarnings("unchecked")
        Number number = (Number)props.get("durability");

        if (number == null) {
            return UNKNOWN_REPLICATION_FACTOR;
        }
        return number.shortValue();
    }

    private static long modificationTime(final MantaObject mantaObject) {
        Preconditions.checkNotNull(mantaObject);

        if (mantaObject.getLastModifiedTime() == null) {
            return UNKNOWN_MOD_TIME;
        }

        return mantaObject.getLastModifiedTime().getTime();
    }

    private static long modificationTime(final Map<String, Object> props) {
        Preconditions.checkNotNull(props);

        if (!props.containsKey("mtime")) {
            return UNKNOWN_MOD_TIME;
        }

        final String mtime = props.get("mtime").toString();

        try {
            final Date date = TIMESTAMP_PARSER.parse(mtime);
            return date.getTime();
        } catch (ParseException e) {
            String msg = String.format("Unable to parse modification time [%s] "
                    + "with pattern [%s]", mtime, ISO_8601_FORMAT);
            LOG.warn(msg, e);
            return UNKNOWN_MOD_TIME;
        }
    }
}
