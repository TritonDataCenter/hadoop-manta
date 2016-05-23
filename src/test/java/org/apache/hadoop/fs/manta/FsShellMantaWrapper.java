package org.apache.hadoop.fs.manta;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.util.ToolRunner;

public class FsShellMantaWrapper extends FsShell {
    public static void main(final String[] argv) throws Exception {
        FsShell shell = newShellInstance();
        Configuration conf = new Configuration(false);
        conf.setQuietMode(false);
        shell.setConf(conf);
        int res;
        try {
            res = ToolRunner.run(shell, argv);
        } finally {
            shell.close();
        }
        System.exit(res);
    }
}
