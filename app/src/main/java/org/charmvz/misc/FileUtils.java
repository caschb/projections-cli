package org.charmvz.misc;

import java.io.File;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.charmvz.analysis.StsReader;
import org.charmvz.App;

public class FileUtils {
    private class FileTreeMap extends TreeMap<Integer, File> {
    }

    private ArrayList<TreeSet<Integer>> validPEs;
    private String validPEStrings[];
    private boolean hasFiles[];

    private StsReader sts;
    private String baseName;

    /** The file for the log for each specified pe */
    private FileTreeMap logFiles[];

    public FileUtils(StsReader stsReader) {
        this.sts = stsReader;
        baseName = sts.getBaseName();
        detectFiles();
    }

    private void detectFiles() {
        hasFiles = new boolean[App.NUM_TYPES];
        validPEs = new ArrayList<TreeSet<Integer>>(App.NUM_TYPES);
        validPEStrings = new String[App.NUM_TYPES];

        // Scan for log files and record what we find
        logFiles = new FileTreeMap[App.NUM_TYPES];

        for (int type = 0; type < App.NUM_TYPES; type++) {
            validPEs.add(type, new TreeSet<Integer>());
            logFiles[type] = new FileTreeMap();

            detectFiles(type);
            validPEStrings[type] = Util.listToString(validPEs.get(type));
        }

        for (int type = 0; type < App.NUM_TYPES; type++) {
            int numfiles = logFiles[type].size();
            if (numfiles != 0)
                System.out.println("Found " + numfiles + " " + getTypeExtension(type) + " files");
        }
    }

    private String getTypeExtension(int type) {
        String fileExt = null;
        switch (type) {
            case App.SUMMARY:
                fileExt = "sum";
                break;
            case App.SUMDETAIL:
                fileExt = "sumd";
                break;
            case App.LOG:
                fileExt = "log";
                break;
            case App.DOP:
                fileExt = "poselog";
                break;
            default:
                System.err.println("Internal Error: Unknown file type " +
                        "index " + type);
                System.exit(-1);
        }
        return fileExt;
    }

    private void detectFiles(int type) {
        File testFile = null;
        // special condition for SUMACC (and any future, single-file
        // log types) only
        if (type == App.SUMACC) {
            testFile = new File(getSumAccumulatedName(baseName));
            if (testFile.isFile() &&
                    testFile.length() > 0 &&
                    testFile.canRead()) {
                hasFiles[type] = true;
            }
            return;
        }

        findFilesInDirectory(new File(dirFromFile()), type);
    }

    private String dirFromFile() {
        // pre condition - filename is a full path name
        int index = baseName.lastIndexOf(File.separator);
        if (index > -1) {
            return baseName.substring(0, index);
        }
        return ("./"); // present directory
    }

    private void findFilesInDirectory(File myDir, int type) {
        if (!myDir.isDirectory()) {
            System.err.println("Internal Error: Path [" + myDir.getAbsolutePath() + "] " +
                    "supplied for file detection is not a " +
                    "directory! Please report to developers!");
            System.exit(-1);
        }

        File prefix = new File(baseName);
        String prefix_s = prefix.getName();
        String extension = getTypeExtension(type);
        final int prefixNumSplits = prefix_s.split("\\.").length;
        final int numPEs = sts.getProcessorCount();

        // System.out.println("FileUtils.dirFromFile(baseName) = " +
        // FileUtils.dirFromFile(baseName) );

        for (File f : myDir.listFiles()) {
            String filename = f.getName();

            // System.out.println("Examining " + filename + " with extension "+extension);

            if (filename.startsWith(prefix_s)) {

                // System.out.println("File "+ filename + " does start with " + prefix_s);

                if (f.isDirectory()) {

                    String[] splits = filename.split("\\.");
                    int numSplits = splits.length;
                    if (numSplits > 1 && splits[numSplits - 2].equals("projdir")) {

                        if (type == App.LOG) {
                            System.out.println("Looking for logs in subdirectory: " + f.getAbsolutePath());
                        }

                        // Look inside the directory
                        findFilesInDirectory(f, type);
                    }

                } else if (f.isFile()) {
                    String[] splits = filename.split("\\.");
                    int numSplits = splits.length;
                    if (numSplits > prefixNumSplits) {
                        if (splits[numSplits - 1].equals(extension)) {
                            int pe = Integer.parseInt(splits[numSplits - 2]);
                            if (pe < numPEs) {
                                validPEs.get(type).add(pe);
                                hasFiles[type] = true;
                                logFiles[type].put(pe, f);
                            }
                            // System.out.println("Found " + extension + " for pe " + pe);
                        } else if (splits[numSplits - 2].equals(extension) && splits[numSplits - 1].equals("gz")) {
                            int pe = Integer.parseInt(splits[numSplits - 3]);
                            if (pe < numPEs) {
                                validPEs.get(type).add(pe);
                                hasFiles[type] = true;
                                logFiles[type].put(pe, f);
                            }
                            // System.out.println("Found " + extension + ".gz for pe " + pe);
                        } else {
                            // The file does not appear to match the desired names
                        }
                    }
                }
            } else {
                // System.out.println("File "+ filename + " does not start with " + prefix_s);
            }

        }
    }

    private String getSumAccumulatedName(String baseName) {
        return baseName + ".sum";
    }

    public File getLogFile(int peNum) {
        return logFiles[App.LOG].get(peNum);
    }

}
