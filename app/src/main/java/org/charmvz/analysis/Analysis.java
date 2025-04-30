package org.charmvz.analysis;

import java.io.File;
import java.io.IOException;

import org.charmvz.misc.FileUtils;

public class Analysis {

    private StsReader stsReader;
    private FileUtils fileNameHandler;

    public void initAnalysis(String filename) throws IOException {
        try {
            stsReader = new StsReader(filename);
        } catch (LogLoadException e) {
            System.err.println(e);
            System.exit(-1);
        }
        fileNameHandler = new FileUtils(stsReader);

    }

    public StsReader getStsReader() {
        return stsReader;
    }

    public File getLog(int peNum) {
        return fileNameHandler.getLogFile(peNum);
    }

    public int getEntryCount() {
        return getStsReader().getEntryCount();
    }

    public String getEntryFullNameByID(int id) {
        return getStsReader().getEntryFullNameByID(id);
    }
}
