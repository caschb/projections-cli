package org.charmvz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.charmvz.analysis.Analysis;

public class App {
// ** System-level variables ** 
    // CUR_VERSION indicates what logs this version of Projections
    // is capable of reading. Any logs that are of a higher version
    // cannot be read and this will be indicated by an unrecoverable
    // error when attempted.
    public static double CUR_VERSION = 11.0;
    public static boolean IGNORE_IDLE = false;
    public static boolean BLUEGENE = false;
    public static int BLUEGENE_SIZE[] = new int[3];

    // **CW** kind of a hack to allow users to specify initial summary
    // window data.
    public static int SUM_START_INT = 0;
    public static int SUM_END_INT = 0;
    public static long SUM_INT_SIZE = 0;
    public static boolean SUM_OVERRIDE = false;

    // **CW** workaround to print details on system usage where too many
    // entry methods prevent proper analysis (like in cpaimd).
    public static boolean PRINT_USAGE = false;

    // **CW** My little going-away joke.
    public static boolean FUNNY = false;

    // Analysis-specific global constants
    public static final int NUM_TYPES = 5;
    public static final int LOG = 0;
    public static final int SUMMARY = 1;
    public static final int SUMDETAIL = 2;
    public static final int DOP = 3;
    public static final int SUMACC = 4;

    private static void startup(String[] args) {
        String stsFilePath = null;
        ArrayList<String> logFilePaths = new ArrayList<>();
        if(args.length == 0) {
            System.err.println("Not enough arguments");
            System.exit(-1);
        }

        File logDirectory = new File(args[0]);
        File [] logDirChildren = logDirectory.listFiles();
        for(File child : logDirChildren) {
            String filename = child.getName();
            if(filename.endsWith(".sts")) {
                stsFilePath = child.getAbsolutePath();
            }
            else if (filename.endsWith(".log.gz") || filename.endsWith(".log")) {
                logFilePaths.add(child.getAbsolutePath());
            }
        }
        assert(stsFilePath != null);
        assert(logFilePaths.size() > 0);
        System.out.println(stsFilePath);
        for(String logfile : logFilePaths) {
            System.out.println(logfile);
        }
        Analysis analysis = new Analysis();

        try {
            analysis.initAnalysis(stsFilePath);
        } catch (IOException e) {
            System.err.println(e);
        }

    }


    public static void main(String[] args) {
        startup(args);
    }
}
