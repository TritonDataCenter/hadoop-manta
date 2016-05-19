package org.apache.hadoop.fs.manta;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.config.ConfigContext;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MantaFileSystemIT {
    private static final String TEST_DATA = "DATA GRAVITY CREATES DATA BLACK HOLES";

    private static String basePath;
    private static MantaFileSystem fs;
    private static MantaClient client;
    private static ConfigContext config;
    private static String testPathPrefix;

    @BeforeClass
    public static void setup() throws IOException {
        fs = instance();
        client = fs.getMantaClient();
        config = fs.getConfig();

        basePath = String.format("%s/stor/%s/",
                config.getMantaHomeDirectory(), UUID.randomUUID());
        client.putDirectory(basePath);
    }

    @AfterClass
    public static void cleanup() throws IOException {
        if (client != null) {
            client.deleteRecursive(basePath);
            client = null;
        }

        config = null;
        fs.close();
    }

    @Before
    public void before() throws IOException {
        testPathPrefix = String.format("%s%s/",
                basePath, UUID.randomUUID());
        client.putDirectory(testPathPrefix);
    }

    private static MantaFileSystem instance() {
        final Configuration config = new Configuration();
        URI uri = URI.create("manta:///");

        try {
            FileSystem fs = FileSystem.get(uri, config);
            @SuppressWarnings("unchecked")
            MantaFileSystem mfs = (MantaFileSystem)fs;

            return mfs;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean containsFile(final List<? extends FileStatus> list,
                                        final String path) {
        Path comparePath = new Path(FilenameUtils.normalize(path));

        for (FileStatus status : list) {
            if (status.getPath().equals(comparePath) && !status.isDirectory()) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsDir(final List<? extends FileStatus> list,
                                       final String path) {
        Path comparePath = new Path(FilenameUtils.normalize(path));

        for (FileStatus status : list) {
            if (status.getPath().equals(comparePath) && status.isDirectory()) {
                return true;
            }
        }

        return false;
    }

    @Test
    public void canListStatus() throws IOException {
        client.putDirectory(testPathPrefix + "dir-1");
        client.putDirectory(testPathPrefix + "dir-2");
        client.putDirectory(testPathPrefix + "dir-3");
        client.put(testPathPrefix + "file1.txt", TEST_DATA);
        client.put(testPathPrefix + "file2.txt", TEST_DATA);
        client.put(testPathPrefix + "file3.txt", TEST_DATA);

        Path path = new Path(testPathPrefix);
        List<FileStatus> results = Arrays.asList(fs.listStatus(path));

        assertEquals("Mismatch between size of results and entries added",
                6, results.size());

        assertTrue("File doesn't exist: " + testPathPrefix + "file1.txt",
                containsFile(results, testPathPrefix + "file1.txt"));
        assertTrue("File doesn't exist: " + testPathPrefix + "file2.txt",
                containsFile(results, testPathPrefix + "file2.txt"));
        assertTrue("File doesn't exist: " + testPathPrefix + "file3.txt",
                containsFile(results, testPathPrefix + "file3.txt"));

        assertTrue("Directory doesn't exist: " + testPathPrefix + "dir-1",
                containsDir(results, testPathPrefix + "dir-1"));
        assertTrue("Directory doesn't exist: " + testPathPrefix + "dir-2",
                containsDir(results, testPathPrefix + "dir-2"));
        assertTrue("Directory doesn't exist: " + testPathPrefix + "dir-3",
                containsDir(results, testPathPrefix + "dir-3"));
    }

    @Test
    public void canListMantaDirectory() throws IOException {
        client.putDirectory(testPathPrefix + "dir-1");
        client.putDirectory(testPathPrefix + "dir-2");
        client.putDirectory(testPathPrefix + "dir-3");
        client.put(testPathPrefix + "file1.txt", TEST_DATA);
        client.put(testPathPrefix + "file2.txt", TEST_DATA);
        client.put(testPathPrefix + "file3.txt", TEST_DATA);

        Path path = new Path(testPathPrefix);
        RemoteIterator<LocatedFileStatus> itr = fs.listFiles(path, false);
        List<LocatedFileStatus> results = new ArrayList<>(6);

        while (itr.hasNext()) {
            results.add(itr.next());
        }

        assertEquals("Mismatch between size of results and files added",
                3, results.size());

        assertTrue("File doesn't exist: " + testPathPrefix + "file1.txt",
                containsFile(results, testPathPrefix + "file1.txt"));
        assertTrue("File doesn't exist: " + testPathPrefix + "file2.txt",
                containsFile(results, testPathPrefix + "file2.txt"));
        assertTrue("File doesn't exist: " + testPathPrefix + "file3.txt",
                containsFile(results, testPathPrefix + "file3.txt"));

        assertFalse("Directory returned in file results: " + testPathPrefix + "dir-1",
                containsDir(results, testPathPrefix + "dir-1"));
        assertFalse("Directory returned in file results: " + testPathPrefix + "dir-2",
                containsDir(results, testPathPrefix + "dir-2"));
        assertFalse("Directory returned in file results: " + testPathPrefix + "dir-3",
                containsDir(results, testPathPrefix + "dir-3"));
    }

    @Test
    public void canMakeDirectory() throws IOException {
        Path newDirectory = new Path(testPathPrefix + "newDirectory-"  + UUID.randomUUID());
        boolean result = fs.mkdirs(newDirectory);

        assertTrue("Directory not indicated as created", result);
        boolean exists = client.existsAndIsAccessible(newDirectory.toString());
        assertTrue("Directory doesn't exist on Manta", exists);
    }

    @Test
    public void canDetermineIfDirectory() throws IOException {
        Path dir = new Path(testPathPrefix + "newDirectory-" + UUID.randomUUID());
        boolean added = client.putDirectory(dir.toString());
        assertTrue("Directory not indicated as created", added);

        boolean result = fs.isDirectory(dir);
        assertTrue("Directory not read as directory", result);
    }

    @Test
    public void canDetermineIfFile() throws IOException {
        Path file = new Path(testPathPrefix + "file-" + UUID.randomUUID() + ".txt");
        client.put(file.toString(), TEST_DATA);
        assertTrue("File wasn't added to Manta",
                client.existsAndIsAccessible(file.toString()));
        boolean result = fs.isFile(file);
        assertTrue("File not read as file", result);
    }

    @Test
    public void canReadWholeFileAsStream() throws IOException {
        Path file = new Path(testPathPrefix + "read-" + UUID.randomUUID() + ".txt");
        client.put(file.toString(), TEST_DATA);

        try (FSDataInputStream in = fs.open(file)) {
            String actual = IOUtils.toString(in, Charsets.UTF_8);
            assertEquals("Contents didn't match", TEST_DATA, actual);
        }
    }

//    @Test
    public void canAddFile() throws IOException {
        Path file = new Path(testPathPrefix + "upload-" + UUID.randomUUID() + ".txt");
        try (FSDataOutputStream out = fs.create(file)) {
            out.writeUTF(TEST_DATA);
        }

        boolean exists = client.existsAndIsAccessible(file.toString());
        assertTrue("File didn't get uploaded to path: " + file,
                exists);

        String actual = client.getAsString(file.toString());
        assertEquals("Uploaded contents didn't match", TEST_DATA, actual);
    }

    @Test
    public void canRenameFile() throws IOException {
        Path file1 = new Path(testPathPrefix + "original-" + UUID.randomUUID() + ".txt");
        Path file2 = new Path(testPathPrefix + "renamed-" + UUID.randomUUID() + ".txt");

        client.put(file1.toString(), TEST_DATA);
        assertTrue("Test file not uploaded",
                client.existsAndIsAccessible(file1.toString()));

        boolean renamed = fs.rename(file1, file2);
        assertTrue("File was indicated as not renamed successfully",
                renamed);

        assertFalse("Original file still exists",
                client.existsAndIsAccessible(file1.toString()));
        assertTrue("Renamed file is not available",
                client.existsAndIsAccessible(file2.toString()));
        String actual = client.getAsString(file2.toString());
        assertEquals("File contents do not match", TEST_DATA, actual);
    }

    @Test
    public void canRenameDirectory() throws IOException {
        Path dir1 = new Path(testPathPrefix + "original-" + UUID.randomUUID());
        Path dir2 = new Path(testPathPrefix + "renamed-" + UUID.randomUUID());

        client.putDirectory(dir1.toString());
        client.put(dir1 + "/file-1.txt", TEST_DATA);

        assertTrue("Test directory not created",
                client.existsAndIsAccessible(dir1.toString()));

        boolean renamed = fs.rename(dir1, dir2);
        assertTrue("Directory was indicated as not renamed successfully",
                renamed);

        assertFalse("Original directory still exists",
                client.existsAndIsAccessible(dir1.toString()));
        assertTrue("Renamed directory is not available",
                client.existsAndIsAccessible(dir2.toString()));

        assertTrue("File in directory was not preserved",
                client.existsAndIsAccessible(dir2.toString() + "/file-1.txt"));
    }

    @Test
    public void canDeleteFile() throws IOException {
        Path file = new Path(testPathPrefix + "delete-me-" + UUID.randomUUID() + ".txt");
        client.put(file.toString(), TEST_DATA);
        Assert.assertTrue("Test file not uploaded",
                client.existsAndIsAccessible(file.toString()));

        boolean result = fs.delete(file, false);
        Assert.assertTrue("File should be indicated as deleted", result);

        Assert.assertFalse("File not actually deleted",
                client.existsAndIsAccessible(file.toString()));
    }

    @Test
    public void canHandleDeletingNonexistentFile() throws IOException {
        Path file = new Path(testPathPrefix + "delete-me-" + UUID.randomUUID() + ".txt");

        Assert.assertFalse("Test file exists",
                client.existsAndIsAccessible(file.toString()));

        boolean result = fs.delete(file, false);
        Assert.assertFalse("File shouldn't be indicated as deleted", result);

        Assert.assertFalse("File didn't get created",
                client.existsAndIsAccessible(file.toString()));
    }

    @Test
    public void canDeleteEmptyDirectory() throws IOException {
        Path dir = new Path(testPathPrefix + "delete-me-" + UUID.randomUUID());
        client.putDirectory(dir.toString());
        Assert.assertTrue("Test directory not created",
                client.existsAndIsAccessible(dir.toString()));

        boolean result = fs.delete(dir, false);
        Assert.assertTrue("Directory should be indicated as deleted", result);

        Assert.assertFalse("Directory not actually deleted",
                client.existsAndIsAccessible(dir.toString()));
    }

    @Test
    public void canDeleteDirectoryRecursively() throws IOException {
        Path dir = new Path(testPathPrefix + "delete-me-" + UUID.randomUUID());
        client.putDirectory(dir.toString());
        client.put(dir.toString() + "/file-1.txt", TEST_DATA);

        Assert.assertTrue("Test directory not created",
                client.existsAndIsAccessible(dir.toString()));

        boolean result = fs.delete(dir, true);
        Assert.assertTrue("Directory should be indicated as deleted", result);

        Assert.assertFalse("Directory not actually deleted",
                client.existsAndIsAccessible(dir.toString()));
    }
}
