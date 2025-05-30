package org.charmvz.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.InputMismatchException;
import java.util.zip.GZIPInputStream;

import org.charmvz.misc.LogEntry;
import org.charmvz.analysis.EndOfLogSuccess;
import org.charmvz.analysis.Defs;

public class GenericLogReader extends ProjectionsReader {

  // Temporary hardcode. This variable will be assigned appropriate
  // meaning in future versions of Projections that support multiple
  // runs.
  private static int myRun = 0;

  /**
   * How many bytes should be read at a time from the file. This should be big
   * enough to keep the disks from thrashing.
   */
  private int bufferSize = 256 * 1024;

  private double version;

  private BufferedReader reader;

  /**
   * Technically, lastRecordedTime is not required, but because this
   * class cannot control what client modules do to the "data" object
   * passed in, it is much safer to record the lastRecordedTime locally.
   */
  private long lastRecordedTime = 0;

  /**
   * Book-keeping data. Used for consistency when event-blocks
   * happen to straddle user-specified time-boundaries.
   */
  private LogEntry lastBeginEvent = null;
  private Analysis analysis;

  private boolean endComputationOccurred;

  long shiftAmount = 0;

  public GenericLogReader(int peNum, Analysis analysis) {
    super(analysis.getLog(peNum), String.valueOf(analysis.getVersion()));
    this.analysis = analysis;
    sourceFile = analysis.getLog(peNum);
    // shiftAmount = analysis.tachyonShifts.getShiftAmount(peNum);

    lastBeginEvent = new LogEntry();
    lastBeginEvent.setValid(false);
    endComputationOccurred = false;

    reader = createBufferedReader(sourceFile);
    version = analysis.getVersion();
    try {
      reader.readLine(); // skip over the header (already read)
    } catch (IOException e) {
      System.err.println("Error reading file");
    }
  }

  private BufferedReader createBufferedReader(File file) {
    BufferedReader r = null;
    String filename = file.getAbsolutePath();
    String s3 = filename.substring(filename.length() - 3); // last 3 characters of filename

    try {
      if (s3.compareTo(".gz") == 0) {
        // Try loading the gz version of the log file
        InputStream fis = new FileInputStream(file);
        InputStream gis = new GZIPInputStream(fis);
        r = new BufferedReader(new InputStreamReader(gis), bufferSize);
      } else {
        // Try loading the log file uncompressed
        FileReader fr = new FileReader(file);
        r = new BufferedReader(fr, bufferSize);
      }

    } catch (IOException e2) {
      System.err.println("Error reading file " + filename);
      return null;
    }

    return r;

  }

  public LogEntry nextEvent() throws InputMismatchException, IOException, EndOfLogSuccess {
    LogEntry data = new LogEntry();
    return nextEvent(data);
  }

  public LogEntry nextEvent(LogEntry data) throws InputMismatchException, IOException, EndOfLogSuccess {
    StsReader stsinfo = analysis.getStsReader();
    // StsReader stsinfo = MainWindow.runObject[myRun].getSts();

    String line = reader.readLine();

    if (line == null)
      throw new EndOfLogSuccess();

    AsciiLineParser sc = new AsciiLineParser(line);

    // We can't keep reading once we've past the END_COMPUTATION record
    if (endComputationOccurred) {
      throw new EndOfLogSuccess();
    }

    // If at end of file and we haven't s3een an END_COMPUTATION yet
    /*
     * if (line == null) {
     * // Generate a fake END_COMPUTATION if no legitimate one was found
     * // This is to deal with partial truncated projections logs.
     * endComputationOccurred = true;
     * data.type = END_COMPUTATION;
     * data.time = lastRecordedTime;
     * System.err.println("[" + sourceFile.getAbsolutePath()
     * +
     * "] WARNING: Partial or Corrupted Projections log. Faked END_COMPUTATION entry added for last recorded time of "
     * + data.time);
     * return data;
     * }
     */

    data.type = (int) sc.nextLong();
    switch (data.type) {
      case BEGIN_IDLE:
        lastBeginEvent.time = data.time = sc.nextLong() + shiftAmount;
        lastBeginEvent.pe = data.pe = (int) sc.nextLong();
        lastBeginEvent.setValid(true);
        break;
      case END_IDLE:
        data.time = sc.nextLong() + shiftAmount;
        data.pe = (int) sc.nextLong();
        lastBeginEvent.setValid(false);
        break;
      case BEGIN_PACK:
      case END_PACK:
      case BEGIN_UNPACK:
      case END_UNPACK:
        data.time = sc.nextLong() + shiftAmount;
        data.pe = (int) sc.nextLong();
        break;
      case USER_SUPPLIED:
        data.userSupplied = (int) sc.nextLong();
        break;
      case USER_SUPPLIED_NOTE:
        data.time = sc.nextLong() + shiftAmount;
        int strLen = (int) sc.nextLong(); // strlen
        data.note = interpretNote(sc.nextString(strLen));
        break;
      case USER_SUPPLIED_BRACKETED_NOTE:
        data.time = sc.nextLong() + shiftAmount;
        data.endTime = sc.nextLong();
        data.userEventID = (int) sc.nextLong();
        data.entry = data.userEventID;
        int brStrLen = (int) sc.nextLong(); // strlen
        data.note = interpretNote(sc.nextString(brStrLen));
        break;
      case MEMORY_USAGE:
        data.memoryUsage = sc.nextLong();
        data.time = sc.nextLong() + shiftAmount;
        break;
      case CREATION:
        data.mtype = (int) sc.nextLong();
        data.entry = (int) sc.nextLong();
        data.time = sc.nextLong() + shiftAmount;
        data.event = (int) sc.nextLong();
        data.pe = (int) sc.nextLong();
        if (version >= 2.0) {
          data.msglen = (int) sc.nextLong();
        } else {
          data.msglen = -1;
        }
        if (version >= 5.0) {
          data.sendTime = sc.nextLong() + shiftAmount;
        }
        break;
      case CREATION_BCAST:
        data.mtype = (int) sc.nextLong();
        data.entry = (int) sc.nextLong();
        data.time = sc.nextLong() + shiftAmount;
        data.event = (int) sc.nextLong();
        data.pe = (int) sc.nextLong();
        if (version >= 2.0) {
          data.msglen = (int) sc.nextLong();
        } else {
          data.msglen = -1;
        }
        if (version >= 5.0) {
          data.sendTime = sc.nextLong() + shiftAmount;
        }
        data.numPEs = (int) sc.nextLong();
        break;
      case CREATION_MULTICAST:
        data.mtype = (int) sc.nextLong();
        data.entry = (int) sc.nextLong();
        data.time = sc.nextLong() + shiftAmount;
        data.event = (int) sc.nextLong();
        data.pe = (int) sc.nextLong();
        if (version >= 2.0) {
          data.msglen = (int) sc.nextLong();
        } else {
          data.msglen = -1;
        }
        if (version >= 5.0) {
          data.sendTime = sc.nextLong() + shiftAmount;
        }
        data.numPEs = (int) sc.nextLong();
        data.destPEs = new int[data.numPEs];
        for (int i = 0; i < data.numPEs; i++) {
          data.destPEs[i] = (int) sc.nextLong();
        }
        break;
      case BEGIN_PROCESSING:
        lastBeginEvent.mtype = data.mtype = (int) sc.nextLong();
        lastBeginEvent.entry = data.entry = (int) sc.nextLong();
        lastBeginEvent.time = data.time = sc.nextLong() + shiftAmount;
        lastBeginEvent.event = data.event = (int) sc.nextLong();
        lastBeginEvent.pe = data.pe = (int) sc.nextLong();
        if (version >= 2.0) {
          lastBeginEvent.msglen = data.msglen = (int) sc.nextLong();
        } else {
          lastBeginEvent.msglen = data.msglen = -1;
        }
        if (version >= 4.0) {
          lastBeginEvent.recvTime = data.recvTime = sc.nextLong() + shiftAmount;
          if (version < 9.0) {
            lastBeginEvent.id[0] = data.id[0] = (int) sc.nextLong();
            lastBeginEvent.id[1] = data.id[1] = (int) sc.nextLong();
            lastBeginEvent.id[2] = data.id[2] = (int) sc.nextLong();

            if (version >= 7.0) {
              lastBeginEvent.id[3] = data.id[3] = (int) sc.nextLong();
            }
          } else {
            // In version 9.0 and above, IDs for chare arrays have exactly as many values as
            // they
            // have dimensions. So if the current event corresponds to a chare array, check
            // how
            // many dimensions it has and read that many values
            int dimensions = stsinfo.getEntryChareDimensionsByID(data.entry);
            for (int i = 0; i < dimensions; i++) {
              lastBeginEvent.id[i] = data.id[i] = (int) sc.nextLong();
            }
          }
        }
        if (version >= 6.5) {
          lastBeginEvent.cpuStartTime = data.cpuStartTime = sc.nextLong() + shiftAmount;
        }
        if (version >= 6.6) {
          // lastBeginEvent.numPerfCounts = data.numPerfCounts = (int) sc.nextLong();
          lastBeginEvent.numPerfCounts = data.numPerfCounts = stsinfo.getNumPerfCounts();
          lastBeginEvent.perfCounts = new long[data.numPerfCounts];
          data.perfCounts = new long[data.numPerfCounts];
          for (int i = 0; i < data.numPerfCounts; i++) {
            lastBeginEvent.perfCounts[i] = data.perfCounts[i] = sc.nextLong();
          }
        }
        lastBeginEvent.setValid(true);
        break;
      case END_PROCESSING:
        data.mtype = (int) sc.nextLong();
        data.entry = (int) sc.nextLong();
        data.time = sc.nextLong() + shiftAmount;
        data.event = (int) sc.nextLong();
        data.pe = (int) sc.nextLong();
        if (version >= 2.0) {
          data.msglen = (int) sc.nextLong();
        } else {
          data.msglen = -1;
        }
        if (version >= 6.5) {
          data.cpuEndTime = sc.nextLong() + shiftAmount;
        }
        if (version >= 6.6) {
          // data.numPerfCounts = (int) sc.nextLong();
          data.numPerfCounts = stsinfo.getNumPerfCounts();
          data.perfCounts = new long[data.numPerfCounts];
          for (int i = 0; i < data.numPerfCounts; i++) {
            data.perfCounts[i] = sc.nextLong();
          }
        }
        lastBeginEvent.setValid(false);
        break;
      case BEGIN_TRACE:
        data.time = sc.nextLong() + shiftAmount;
        // invalidates the last Begin Event. BEGIN_TRACE happens
        // in the context of an entry method that is *not* traced.
        // Hence when a BEGIN_TRACE event is encountered, no
        // information is actually known about the entry method
        // context.
        lastBeginEvent.setValid(false);
        break;
      case END_TRACE:
        data.time = sc.nextLong() + shiftAmount;
        // END_TRACE happens in the context of an existing
        // entry method and hence should logically "end" it.
        // This means any client taking note of END_TRACE must
        // take into account lastBeginEvent in order to get
        // reasonable data.
        break;
      case MESSAGE_RECV:
        data.mtype = (int) sc.nextLong();
        data.time = sc.nextLong() + shiftAmount;
        data.event = (int) sc.nextLong();
        data.pe = (int) sc.nextLong();
        data.msglen = (int) sc.nextLong();
        break;
      case ENQUEUE:
      case DEQUEUE:
        data.mtype = (int) sc.nextLong();
        data.time = sc.nextLong() + shiftAmount;
        data.event = (int) sc.nextLong();
        data.pe = (int) sc.nextLong();
        break;
      case BEGIN_INTERRUPT:
      case END_INTERRUPT:
        data.time = sc.nextLong() + shiftAmount;
        data.event = (int) sc.nextLong();
        data.pe = (int) sc.nextLong();
        break;
      case BEGIN_COMPUTATION:
        data.time = sc.nextLong() + shiftAmount;
        break;
      case END_COMPUTATION:
        data.time = sc.nextLong() + shiftAmount;
        endComputationOccurred = true;
        break;
      case USER_EVENT:
        data.userEventID = (int) sc.nextLong();
        data.entry = data.userEventID;
        data.time = sc.nextLong() + shiftAmount;
        data.event = (int) sc.nextLong();
        data.pe = (int) sc.nextLong();
        break;
      case USER_EVENT_PAIR:
        data.userEventID = (int) sc.nextLong();
        data.entry = data.userEventID;
        data.time = sc.nextLong() + shiftAmount;
        data.event = (int) sc.nextLong();
        data.pe = (int) sc.nextLong();

        // Charm++ before 6.8 did not have a nestedID for USER_EVENT_PAIR
        if (sc.hasNextField())
          data.nestedID = (int) sc.nextLong();
        break;
      case BEGIN_USER_EVENT_PAIR:
      case END_USER_EVENT_PAIR:
        data.userEventID = (int) sc.nextLong();
        data.entry = data.userEventID;
        data.time = sc.nextLong() + shiftAmount;
        data.event = (int) sc.nextLong();
        data.pe = (int) sc.nextLong();
        data.nestedID = (int) sc.nextLong();
        break;
      case USER_STAT:
        data.time = sc.nextLong() + shiftAmount; // Wall Time
        data.userTime = sc.nextDouble(); // User time
        data.stat = sc.nextDouble();
        data.pe = (int) sc.nextLong();
        data.userEventID = (int) sc.nextLong();
        break;
      default:
        data.type = -1;
        break;

    }

    lastRecordedTime = data.time;
    return data;

  }

  private String interpretNote(String input) {
    String modified = input;
    if (modified.contains("<EP")) {
      int numEntries = analysis.getEntryCount();
      for (int i = 0; i < numEntries; i++) {
        String name = analysis.getEntryFullNameByID(i);
        modified = modified.replace("<EP " + i + ">", name);
      }
    }
    return modified;
  }

  @Override
  protected boolean checkAvailable() {
    return true;
  }

  public void close() throws IOException {
    if (reader != null) {
      reader.close();
    }
  }

}
