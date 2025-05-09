package org.charmvz.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.charmvz.misc.LogEntry;

public class LogReader
    extends Defs {
  // Temporary hardcode. This variable will be assigned appropriate
  // meaning in future versions of Projections that support multiple
  // runs.
  private int myRun = 0;

  // sysUsgData[SYS_Q] is length of message queue
  private static final int SYS_Q = 0;
  // sysUsgData[SYS_CPU] is percent processing time
  private static final int SYS_CPU = 1;
  // sysUsgData[SYS_IDLE] is percent idle time
  private static final int SYS_IDLE = 2;

  // Magic entry method (indicates "idle")
  private static final int IDLE_ENTRY = -1;
  private static final int NO_ENTRY = -100;

  // Number of creations bin
  private static final int CREATE = 0;
  // Number of invocations bin
  private static final int PROCESS = 1;
  // Time (us) spent processing bin
  public static final int TIME = 2;

  private int[][][] sysUsgData;
  private int[][][][] userEntries;
  private int[][][][] categorized;
  private int numProcessors;
  private int numUserEntries; // Number of user entry points
  private long startTime; // Time the current entry method started
  private int currentEntry; // Currently executing entry method
  private int currentMtype; // Current message type
  private int interval;// Current interval number
  private int curPeIdx;// Current source processor #
  private int numIntervals;// Total number of intervals
  private long intervalSize;// Length of an interval (us)
  private int intervalStart;// start of range of interesting intervals
  private int intervalEnd; // end of range of interesting intervals

  private int processing;
  private boolean byEntryPoint;

  // **CW** 8/23/2005 Make LogReader use GenericLogReader instead
  private LogEntry curData;
  private Analysis analysis;

  public long getIntervalSize() {
    return intervalSize;
  }

  public LogReader(Analysis analysis) {
    curData = new LogEntry();
    this.analysis = analysis;
  }

  /**
   * Add the given amount of time to the current entry for interval j if it is
   * within the selected interval range.
   */
  final private void addToInterval(long extra, int j) {
    startTime += extra; // Former bug put this at the end of this function

    if (processing <= 0 || j < intervalStart || j > intervalEnd || currentEntry == NO_ENTRY) {
      return;
    }

    if (currentEntry == IDLE_ENTRY) { // Idle time
      sysUsgData[SYS_IDLE][curPeIdx][j - intervalStart] += extra;
    } else { // Processing an entry method
      sysUsgData[SYS_CPU][curPeIdx][j - intervalStart] += extra;
      if (byEntryPoint) {
        userEntries[analysis.getEntryIndex(currentEntry)][TIME][curPeIdx][j - intervalStart] += extra;
        int catIdx = mtypeToCategoryIdx(currentMtype);
        if (catIdx != -1) {
          categorized[catIdx][TIME][curPeIdx][j - intervalStart] += extra;
        }
      }
    }
  }

  /**
   * Add this entry point to the counts
   */
  final private void count(int mtype, int entryID, int TYPE) {
    int entry = analysis.getEntryIndex(entryID);

    if (!byEntryPoint) {
      return;
    }
    if (userEntries[entry][TYPE][curPeIdx] == null) {
      userEntries[entry][TYPE][curPeIdx] = new int[numIntervals + 1];
    }
    if (TYPE == PROCESS &&
        userEntries[entry][TIME][curPeIdx] == null) {
      // Add a time array, too
      userEntries[entry][TIME][curPeIdx] = new int[numIntervals + 1];
    }
    userEntries[entry][TYPE][curPeIdx][interval - intervalStart]++;

    int catIdx = mtypeToCategoryIdx(mtype);
    if (catIdx != -1) {
      if (categorized[catIdx][TYPE][curPeIdx] == null) {
        categorized[catIdx][TYPE][curPeIdx] = new int[numIntervals + 1];
        if (TYPE == PROCESS) { // Add a time array, too
          categorized[catIdx][TIME][curPeIdx] = new int[numIntervals + 1];
        }
      }
      categorized[catIdx][TYPE][curPeIdx][interval - intervalStart]++;
    }
  }

  /**
   * This is called when a log entry crosses a timing interval boundary
   */
  private void fillToInterval(int newinterval) {
    if (interval >= newinterval) {
      return; // Nothing to do in this case, we're already past!
    }

    // Finish off the current interval
    long extra = (interval + 1) * intervalSize - startTime;

    addToInterval(extra, interval);

    // Fill in any intervals we would skip over completely
    for (int j = interval + 1; j < newinterval; j++) {
      addToInterval((int) intervalSize, j);
    }

    interval = newinterval;
  }

  public int[][][][] getSystemMsgs() {
    return categorized;
  }

  public int[][][] getSystemUsageData() {
    return sysUsgData;
  }

  public int[][][][] getUserEntries() {
    return userEntries;
  }

  // the interval values used are absolute. Only when array access is
  // required would it be offset by intervalStart.
  private void intervalCalc(int type, int mtype, int entry, long time) {
    fillToInterval((int) (time / intervalSize));
    switch (type) {
      case ENQUEUE:
        if (interval >= intervalStart && interval <= intervalEnd) {
          // Lengthen system queue
          sysUsgData[SYS_Q][curPeIdx][interval - intervalStart]++;
        }
        break;
      case CREATION:
        if (interval >= intervalStart && interval <= intervalEnd) {
          // Lengthen system queue
          sysUsgData[SYS_Q][curPeIdx][interval - intervalStart]++;
          count(mtype, entry, CREATE);
        }
        break;
      case CREATION_MULTICAST:
        // do nothing for now.
        break;
      case BEGIN_PROCESSING:
        processing++;
        startTime = time;
        currentEntry = entry;
        currentMtype = mtype;
        if (interval >= intervalStart && interval <= intervalEnd) {
          // Shorten system queue
          sysUsgData[SYS_Q][curPeIdx][interval - intervalStart]--;
          count(currentMtype, currentEntry, PROCESS);
        }
        break;
      case END_PROCESSING:
        if (mtype == -5) {
          // the "ignore" flag
          break;
        }
        addToInterval((int) (time - startTime), interval);
        processing--;
        break;
      case BEGIN_IDLE:
        processing++;
        startTime = time;
        currentEntry = IDLE_ENTRY;
        break;
      case END_IDLE:
        addToInterval((int) (time - startTime), interval);
        processing--;
        break;
      case END_TRACE:
        break;
      default:
        System.err.println("Unhandled type " + type + " in LogReader.intervalCalc() !");
        break;
    }
  }

  /**
   * Maps a message type to a categorized system message number
   */
  final private int mtypeToCategoryIdx(int mtype) {
    switch (mtype) {
      case NEW_CHARE_MSG:
        return 0;
      case FOR_CHARE_MSG:
        return 1;
      case BOC_INIT_MSG:
        return 2;
      case LDB_MSG:
        return 3;
      case QD_BROADCAST_BOC_MSG:
      case QD_BOC_MSG:
        return 4;
      default:
        return -1;
    }
  }

  public void print_logs() {
    SortedSet<Integer> processorList = new TreeSet<Integer>(analysis.getValidProcessorList());
    for (Integer pe : processorList) {
      GenericLogReader reader = new GenericLogReader(pe, analysis);
      // PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out), false);
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out), 8192);
      long count = 0;
      try {
        while (true) { // EndOfLogException will terminate loop
          count += 1;
          var curData = reader.nextEvent();
          // System.out.println(curData);
          // out.write(curData.toString() + "\n");
          writer.write(curData.toString() + "\n");
          if(count % 1_000 == 0) {
            writer.flush();
            count = 0;
          }
        }
      } catch (EndOfLogSuccess e) {
        System.out.println("Cool!");
      } catch (IOException e) {
        System.err.println("Oh no");
        System.exit(-1);
      }
      try {
        writer.flush();
      } catch (IOException e) {
        System.err.println("Oh no");
        System.exit(-1);
      }
    }
  }

  /**
   * Read log files for a list of processors. If the list of PEs is null, load all
   * available PEs.
   * 
   * Stores phase markers into phaseMarkers in a synchronized manner into
   * phaseMarkers if it is non-null.
   */
  public void read(long reqIntervalSize,
      int NintervalStart, int NintervalEnd,
      boolean NbyEntryPoint, SortedSet<Integer> processorList,
      TreeMap<Double, String> phaseMarkers) {

    numProcessors = analysis.getNumProcessors();
    numUserEntries = analysis.getNumUserEntries();
    intervalSize = reqIntervalSize;
    intervalStart = NintervalStart;
    intervalEnd = NintervalEnd;
    numIntervals = intervalEnd - intervalStart + 1;
    byEntryPoint = NbyEntryPoint;

    // assume full range of processors if null
    if (processorList == null) {
      processorList = new TreeSet<Integer>(analysis.getValidProcessorList());
    }

    numProcessors = processorList.size();

    sysUsgData = new int[3][numProcessors][];
    if (byEntryPoint) {
      userEntries = new int[numUserEntries][3][numProcessors][numIntervals];
      categorized = new int[5][3][numProcessors][];
    }

    curPeIdx = 0;
    for (Integer pe : processorList) {
      // gzheng: allocate sysUsgData only when needed.
      sysUsgData[0][curPeIdx] = new int[numIntervals + 1];
      sysUsgData[1][curPeIdx] = new int[numIntervals + 1];
      sysUsgData[2][curPeIdx] = new int[numIntervals + 1];

      processing = 0;
      interval = 0;
      currentEntry = NO_ENTRY;
      startTime = intervalStart * intervalSize;

      int nLines = 2;

      GenericLogReader reader = new GenericLogReader(pe, analysis);

      boolean isProcessing = false;
      LogEntry lastBeginData = null;
      PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out), false);
      long count = 0;

      try {
        while (true) { // EndOfLogException will terminate loop
          count += 1;
          curData = reader.nextEvent();
          // System.out.println(curData);
          out.write(curData.toString() + "\n");
          if(count % 2 == 0) {
            out.flush();
          }
          nLines++;
          switch (curData.type) {
            case BEGIN_IDLE:
            case END_IDLE:
              intervalCalc(curData.type, 0, 0, curData.time);
              break;
            case CREATION:
              intervalCalc(CREATION, curData.mtype,
                  curData.entry, curData.time);
              break;
            case BEGIN_PROCESSING:
              if (isProcessing) {
                // We add a "pretend" end event to accomodate
                // the prior begin processing event.
                intervalCalc(END_PROCESSING, lastBeginData.mtype, lastBeginData.entry, curData.time);
              }
              // Normal case of handling EPs
              isProcessing = true;
              lastBeginData = null;
              intervalCalc(curData.type, curData.mtype, curData.entry, curData.time);
              lastBeginData = curData;
              break;
            case END_PROCESSING:
              if (isProcessing) {
                intervalCalc(curData.type, curData.mtype, curData.entry, curData.time);
              }
              isProcessing = false;
              break;
            case ENQUEUE:
              intervalCalc(curData.type, curData.mtype,
                  0, curData.time);
              break;
            case END_COMPUTATION:
              if (isProcessing) {
                // If the last begin_processing has no end event,
                // add a "pretend" end event.
                intervalCalc(END_PROCESSING, lastBeginData.mtype, lastBeginData.entry,
                    lastBeginData.time);
              }
              isProcessing = false;
              fillToInterval(numIntervals);
              break;
            case BEGIN_TRACE:
              // Sayantan: I think we do-not really need to do anything
              // for begin_trace. However, that may not be the case for a
              // complex series of begin and end traces
              break;
            case END_TRACE:
              intervalCalc(curData.type, curData.mtype, 0, curData.time);
              break;
            case USER_SUPPLIED_NOTE:
              double timeInBinUnits = (double) curData.time / intervalSize - intervalStart;
              if (phaseMarkers != null) {
                synchronized (phaseMarkers) {
                  phaseMarkers.put(timeInBinUnits, curData.note);
                }
              }
              break;
          }
        } // end while loop
      } catch (EndOfLogSuccess e) {
        // Do nothing
      } catch (IOException e) {
        System.err.println("Error: Failure to read log file!");
        System.exit(-1);
      }
      // out.flush();

      try {
        reader.close();
      } catch (IOException e1) {
        System.err.println("Error: could not close log file reader for processor " + pe);
      }

      curPeIdx++;
    } // for loop

    rescale();
  }

  // Convert system usage and idle time from us to percent of an interval
  private void rescale() {
    int l = sysUsgData[SYS_CPU].length;
    for (int pp = 0; pp < l; pp++) {
      int l1 = sysUsgData[SYS_CPU][pp].length;
      for (int i = 0; i < l1; i++) {
        sysUsgData[SYS_CPU][pp][i] = (int) (sysUsgData[SYS_CPU][pp][i] * 100 / intervalSize);
      }
      int l2 = sysUsgData[SYS_IDLE][pp].length;
      for (int i = 0; i < l2; i++) {
        sysUsgData[SYS_IDLE][pp][i] = (int) (sysUsgData[SYS_IDLE][pp][i] * 100 / intervalSize);
      }
    }
  }

}
