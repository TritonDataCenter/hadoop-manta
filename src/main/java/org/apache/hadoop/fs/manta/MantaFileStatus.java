package org.apache.hadoop.fs.manta;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.joyent.manta.client.MantaObject;
import com.joyent.manta.client.MantaObjectResponse;
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
 * Class that provides Manta SDK aware constructors for creating a
 * {@link FileStatus} instance. These constructors handle the logic
 * of transforming the Manta response objects into a {@link FileStatus}
 * instance.
 */
public class MantaFileStatus extends FileStatus {
    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MantaFileStatus.class);

    /**
     * Default value for when the block size is unknown.
     */
    private static final long UNKNOWN_BLOCK_SIZE = 0L;

    /**
     * Default value for when the modification time is unknown.
     */
    private static final long UNKNOWN_MOD_TIME = 0L;

    /**
     * Default value for unknown file size.
     */
    private static final long UNKNOWN_LENGTH = 1L;

    /**
     * Default value for unknown replication factor.
     */
    private static final short UNKNOWN_REPLICATION_FACTOR = 1;

    /**
     * File timestamp parser instance.
     */
    private static final DateFormat TIMESTAMP_PARSER =
            new SimpleDateFormat(MantaObjectResponse.PATTERN_ISO_8601);

    /**
     * Creates a new instance based off of a Manta response object.
     *
     * @param mantaObject Manta response object
     */
    public MantaFileStatus(final MantaObject mantaObject) {
        this(mantaObject, new Path(mantaObject.getPath()));
    }

    /**
     * Creates a new instance based off of a Manta response object and an
     * explicit path.
     *
     * @param mantaObject Manta response object
     * @param path path to resource
     */
    public MantaFileStatus(final MantaObject mantaObject, final Path path) {
        super(length(mantaObject), mantaObject.isDirectory(),
              replicationFactor(mantaObject), UNKNOWN_BLOCK_SIZE,
              modificationTime(mantaObject), path);
    }

    /**
     * Creates a new instance based off of a set of {@link Map} properties.
     *
     * @param props Map with keys set by a Manta response
     * @param path path to resource
     */
    public MantaFileStatus(final Map<String, Object> props, final Path path) {
        super(length(props), isDirectory(props), replicationFactor(props),
                UNKNOWN_BLOCK_SIZE, modificationTime(props), path);
    }

    /**
     * Finds the size of a file based on a Manta response object.
     *
     * @param mantaObject object to parse for file size
     * @return size of file
     */
    private static long length(final MantaObject mantaObject) {
        Preconditions.checkNotNull(mantaObject);

        return Objects.firstNonNull(mantaObject.getContentLength(), UNKNOWN_LENGTH);
    }

    /**
     * Finds the size of a file based on a Manta response as a {@link Map}.
     *
     * @param props properties to parse for file size
     * @return size of file
     */
    private static long length(final Map<String, Object> props) {
        Preconditions.checkNotNull(props);

        @SuppressWarnings("unchecked")
        Number number = (Number)props.get("size");

        if (number == null) {
            return UNKNOWN_LENGTH;
        }
        return number.longValue();
    }

    /**
     * Finds out if a given object is a directory based on a Manta response.
     *
     * @param props properties to parse for directory informations
     * @return true if a directory
     */
    private static boolean isDirectory(final Map<String, Object> props) {
        Preconditions.checkNotNull(props);

        Object type = props.get("type");

        if (type == null) {
            return false;
        }

        return type.toString().equals("directory");
    }

    /**
     * Translates durability level to replication factor from Manta response.
     *
     * @param mantaObject object to parse for durability level
     * @return durability level as replication factor
     */
    private static short replicationFactor(final MantaObject mantaObject) {
        Preconditions.checkNotNull(mantaObject);

        final Integer durability = mantaObject.getHttpHeaders().getDurabilityLevel();

        if (durability == null) {
            return UNKNOWN_REPLICATION_FACTOR;
        }

        return durability.shortValue();
    }

    /**
     * Translates durability level to replication factor from Manta response.
     *
     * @param props properties to parse for durability level
     * @return replication factor as represented by durability level
     */
    private static short replicationFactor(final Map<String, Object> props) {
        Preconditions.checkNotNull(props);

        @SuppressWarnings("unchecked")
        Number number = (Number)props.get("durability");

        if (number == null) {
            return UNKNOWN_REPLICATION_FACTOR;
        }
        return number.shortValue();
    }

    /**
     * Finds the modification time of a Manta object from the response.
     *
     * @param mantaObject response object to parse for modification time
     * @return last modification time of object
     */
    private static long modificationTime(final MantaObject mantaObject) {
        Preconditions.checkNotNull(mantaObject);

        if (mantaObject.getLastModifiedTime() == null) {
            return UNKNOWN_MOD_TIME;
        }

        return mantaObject.getLastModifiedTime().getTime();
    }

    /**
     * Finds the modification time of a Manta object from the response.
     *
     * @param props properties to parse for modification time
     * @return last modification time of object
     */
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
                    + "with pattern [%s]", mtime, MantaObjectResponse.PATTERN_ISO_8601);
            LOG.warn(msg, e);
            return UNKNOWN_MOD_TIME;
        }
    }
}
