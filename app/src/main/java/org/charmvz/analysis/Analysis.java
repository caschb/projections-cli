package org.charmvz.analysis;

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;

import org.charmvz.misc.FileUtils;
import org.charmvz.App;

public class Analysis {

  private StsReader stsReader;
  private FileUtils fileNameHandler;
  private TachyonShifts tachyonShifts;
  private LogLoader logLoader;
  private SumAnalyzer sumAnalyzer;
  private IntervalData intervalData;
  private PoseDopReader dopReader;
  private long totalTime;

  public void initAnalysis(String filename) throws IOException {
    try {
      stsReader = new StsReader(filename);
    } catch (LogLoadException e) {
      System.err.println(e);
      System.exit(-1);
    }
    fileNameHandler = new FileUtils(stsReader);
    tachyonShifts = new TachyonShifts(getLogDirectory());

    // Build Summary Data
    if (hasSumFiles()) {
      sumAnalyzer = new SumAnalyzer();
    }

    // Build Summary Detail Data
    if (hasSumDetailFiles()) {
      if (intervalData == null) {
        intervalData = new IntervalData();
      }
    }

    // Initialize Log Data
    if (hasLogFiles()) {
      logLoader = new LogLoader();
    }

    // Build POSE dop Data
    if (hasPoseDopFiles()) {
      dopReader = new PoseDopReader();
    }

    findEndTime();
  }

  private boolean hasPoseDopFiles() {
    return fileNameHandler.hasPoseDopFiles();
  }

  private boolean hasLogFiles() {
    return fileNameHandler.hasLogFiles();
  }

  private boolean hasSumDetailFiles() {
    return fileNameHandler.hasSumDetailFiles();
  }

  private boolean hasSumFiles() {
    return fileNameHandler.hasSumFiles();
  }

  private String getLogDirectory() {
    return fileNameHandler.dirFromFile();
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

  public int getEntryIndex(int id) {
    Integer result = getStsReader().getEntryIndex(id);
    if (result != null)
      return result;
    else
      throw new RuntimeException("ERROR: log files and sts file are inconsistent. The log files refer to EP " + id
          + " but the sts file doesn't contain an entry for that \"ENTRY CHARE\"\n");
  }

  public int getNumProcessors() {
    return getStsReader().getProcessorCount();
  }

  public int getNumUserEntries() {
    return getStsReader().getEntryCount();
  }

  public SortedSet<Integer> getValidProcessorList() {
    if (hasLogFiles()) {
      return getValidProcessorList(App.LOG);
    } else if (hasSumFiles()) {
      return getValidProcessorList(App.SUMMARY);
    } else if (hasSumDetailFiles()) {
      return getValidProcessorList(App.SUMDETAIL);
    } else if (hasPoseDopFiles()) {
      return getValidProcessorList(App.DOP);
    } else {
      return null;
    }
  }

  private SortedSet<Integer> getValidProcessorList(int type) {
    return fileNameHandler.getValidProcessorList(type);
  }

  public double getVersion() {
    return getStsReader().getVersion();
  }

  private void findEndTime() {
    if (hasLogFiles()) {
      long temp = logLoader.determineEndTime(getValidProcessorList(App.LOG), this);
      if (temp > totalTime) {
        totalTime = temp;
      }
    }
  }

  public long getEndTime() {
    return totalTime;
  }

}
