package com.joyent.hadoop.fs.manta;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaSeekableByteChannel;
import com.joyent.manta.config.ConfigContext;
import com.joyent.manta.config.SystemSettingsConfigContext;
import com.joyent.manta.org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class MantaSeekableInputStreamIT {
    private static final Logger LOG = LoggerFactory.getLogger(MantaSeekableInputStreamIT.class);
    private static final String TEST_DATA = "DATA GRAVITY CREATES DATA BLACK HOLES";

    private static MantaClient mantaClient;
    private static String testPathPrefix;

    @BeforeClass
    public static void before() throws IOException {

        // Let TestNG configuration take precedence over environment variables
        ConfigContext config = new SystemSettingsConfigContext();

        mantaClient = new MantaClient(config);
        testPathPrefix = String.format("/%s/stor/%s/",
                config.getMantaHomeDirectory(), UUID.randomUUID());
        mantaClient.putDirectory(testPathPrefix, null);
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        if (mantaClient != null) {
            mantaClient.deleteRecursive(testPathPrefix);
            mantaClient.closeQuietly();
        }
    }

    @Test
    public void canDoSequentialRead() throws IOException {
        String path = testPathPrefix + "sequential-read.txt";
        mantaClient.put(path, TEST_DATA);

        final MantaSeekableByteChannel seekable = mantaClient.getSeekableByteChannel(path);
        try (MantaSeekableInputStream in = new MantaSeekableInputStream(seekable)) {
            final String actual = IOUtils.toString(in, Charset.defaultCharset());
            assertEquals("Object data isn't equal", TEST_DATA, actual);
        }
    }

    @Test
    public void canReadPositionallyForwardWithoutIncrementingPosition() throws IOException {
        String path = testPathPrefix + "positional-forward-read.txt";
        mantaClient.put(path, TEST_DATA);

        final MantaSeekableByteChannel seekable = mantaClient.getSeekableByteChannel(path);
        try (MantaSeekableInputStream in = new MantaSeekableInputStream(seekable)) {
            final long startPos = in.getPos();

            byte[] buffer = new byte[5];

            in.readFully(4, buffer, 0, 5);

            assertEquals("Position hasn't changed", startPos, in.getPos());

            String actual = new String(buffer);
            String expected = TEST_DATA.substring(4, 9);

            assertEquals("Substring doesn't match", expected, actual);

            // The last operation shouldn't have moved the position, so we are
            // safe to read the whole stream now and get the whole thing
            final String all = IOUtils.toString(in, Charset.defaultCharset());
            assertEquals("Object data isn't equal", TEST_DATA, all);
        }
    }

    @Test
    public void canReadPositionallyBackwardWithoutIncrementingPosition() throws IOException {
        String path = testPathPrefix + "positional-forward-read.txt";
        mantaClient.put(path, TEST_DATA);

        final MantaSeekableByteChannel seekable = mantaClient.getSeekableByteChannel(path);
        try (MantaSeekableInputStream in = new MantaSeekableInputStream(seekable)) {
            in.skip(5);
            final long startPos = in.getPos();

            byte[] buffer = new byte[10];

            in.readFully(0, buffer, 0, 10);

            assertEquals("Position hasn't changed", startPos, in.getPos());

            String actual = new String(buffer);
            String expected = TEST_DATA.substring(0, 10);

            assertEquals("Substring doesn't match", expected, actual);

            // The last operation shouldn't have moved the position, so we are
            // safe to read the whole stream now and get the whole thing
            final String all = IOUtils.toString(in, Charset.defaultCharset());
            assertEquals("Object data isn't equal", TEST_DATA.substring(5), all);
        }
    }
}
