package org.charmvz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.charmvz.analysis.Analysis;

public class App {

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
