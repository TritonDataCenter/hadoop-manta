package com.joyent.hadoop.fs.manta;

import com.google.common.base.Preconditions;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.hadoop.fs.FileChecksum;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Simple md5 implementation of {@link FileChecksum} for getting the md5s of
 * remote files on Manta.
 */
public class MantaChecksum extends FileChecksum {
    /**
     * Number of bytes in a MD5 checksum.
     */
    private static final int MD5_LENGTH = 16;

    /**
     * Plain-text name of checksum algorithm.
     */
    private static final String ALGO_NAME = "md5";

    /**
     * MD5 contents.
     */
    private byte[] md5bytes;

    /**
     * Creates a new checksum based off of the passed bytes.
     *
     * @param md5bytes non-null byte buffer
     */
    public MantaChecksum(final byte[] md5bytes) {
        Preconditions.checkNotNull(md5bytes, "MD5 must be present");
        Preconditions.checkArgument(md5bytes.length == MD5_LENGTH,
                "Invalid number of bytes [%d] for MD5 checksum",
                md5bytes.length);

        this.md5bytes = md5bytes;
    }

    /**
     * Creates a new checksum based off of the plain text hex string.
     *
     * @param hexString plain text hex string
     * @throws DecoderException thrown when hex string can't be parsed
     */
    public MantaChecksum(final String hexString) throws DecoderException {
        Preconditions.checkNotNull(hexString, "MD5 hexstring must be present");
        final byte[] bytes = Hex.decodeHex(hexString.toCharArray());

        Preconditions.checkArgument(bytes.length == MD5_LENGTH,
                "Invalid number of bytes [%d] for MD5 checksum",
                bytes.length);

        this.md5bytes = bytes;
    }

    /**
     * The checksum algorithm name - md5.
     */
    @Override
    public String getAlgorithmName() {
        return ALGO_NAME;
    }

    /**
     * The length of the checksum in bytes - always 16.
     */
    @Override
    public int getLength() {
        return MD5_LENGTH;
    }

    /**
     * The value of the md5 checksum in bytes.
     */
    @Override
    public byte[] getBytes() {
        return md5bytes;
    }

    /**
     * Serialize the fields of this object to <code>out</code>.
     *
     * @param out <code>DataOuput</code> to serialize this object into.
     * @throws IOException thrown when bytes can be written
     */
    @Override
    public void write(final DataOutput out) throws IOException {
        for (byte b : md5bytes) {
            out.writeByte(b);
        }
    }

    /**
     * Deserialize the fields of this object from <code>in</code>.
     *
     * <p>For efficiency, implementations should attempt to re-use storage in the
     * existing object where possible.</p>
     *
     * @param in <code>DataInput</code> to deseriablize this object from.
     * @throws IOException thrown when bytes can be read
     */
    @Override
    public void readFields(final DataInput in) throws IOException {
        byte[] md5 = new byte[MD5_LENGTH];

        for (int i = 0; i < MD5_LENGTH; i++) {
            md5[i] = in.readByte();
        }

        this.md5bytes = md5;
    }
}
