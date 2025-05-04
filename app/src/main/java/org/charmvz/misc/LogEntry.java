package org.charmvz.misc;

import org.charmvz.analysis.Defs;

public class LogEntry extends Defs {

  static private int myRun = 0;

  private boolean isValid = true;

  /** type of the event eg: BEGIN_PROCESSING */
  public int type;

  /** determines */
  public int mtype;

  /** timestamp */
  public long time;

  /**
   * used for bracketed user supplied notes, and all bracketed events in the
   * future
   */
  public long endTime;

  /** EntryPoint number found in sts file */
  public int entry;

  /**
   * Unique sequence number assigned to Events. This is a unique sequence number
   * set by the sender for BEGIN_PROCESSING
   */
  public int event;

  /** processor number where the event occurred */
  public int pe;

  /**
   * Number of processors a message was sent to. Used for CREATION_BCAST and
   * CREATION_MULTICAST
   */
  public int numPEs;

  // version 2.0 constructs
  public int msglen; // only for CREATION events

  public int userEventID; // for USER_EVENT_PAIR events only
  public long sendTime; // sendTime

  // version 4.0 constructs
  public long recvTime; // the time the processor *actually* received
                        // the message.
  public int id[]; // the thread id (3D array tuple).
                   // as of ver 9.0, it is a 6-tuple

  public long cpuStartTime; // start of cpu timer
  public long cpuEndTime; // end of cpu timer

  public int numPerfCounts; // number of performance counters
  public long perfCounts[]; // the array of performance counts

  public int destPEs[]; /// < list of multicast destination processors

  public Integer userSupplied;

  public long memoryUsage;

  /// An arbitrary string provided by the user. Should be displayed as a user
  /// event
  public String note;

  public int nestedID; // Nested thread ID, e.g. virtual AMPI ranks

  public LogEntry() {
    // this is fixed (since it is based on a 3D tuple)
    // As of version 9.0, it is a 6-tuple which includes array ID.
    id = new int[6];
  }

  // 6/7/16 - added User Stats support
  public double stat;
  public double userTime;

  public boolean isValid() {
    return isValid;
  }

  public void setValid(boolean flag) {
    isValid = flag;
  }

  public boolean isBeginType() {
    return ((type == BEGIN_IDLE) ||
        (type == BEGIN_PACK) ||
        (type == BEGIN_UNPACK) ||
        (type == BEGIN_PROCESSING) ||
        (type == BEGIN_TRACE) ||
        (type == BEGIN_INTERRUPT));
  }

  private String typeToString() {
    if (type == CREATION) {
      return "CREATION";
    } else if (type == BEGIN_PROCESSING) {
      return "BEGIN_PROCESSING";
    } else if (type == END_PROCESSING) {
      return "END_PROCESSING";
    } else if (type == ENQUEUE) {
      return "ENQUEUE";
    } else if (type == DEQUEUE) {
      return "DEQUEUE";
    } else if (type == BEGIN_COMPUTATION) {
      return "BEGIN_COMPUTATION";
    } else if (type == END_COMPUTATION) {
      return "END_COMPUTATION";
    } else if (type == BEGIN_INTERRUPT) {
      return "BEGIN_INTERRUPT";
    } else if (type == END_INTERRUPT) {
      return "END_INTERRUPT";
    } else if (type == MESSAGE_RECV) {
      return "MESSAGE_RECV";
    } else if (type == BEGIN_TRACE) {
      return "BEGIN_TRACE";
    } else if (type == END_TRACE) {
      return "END_TRACE";
    } else if (type == USER_EVENT) {
      return "USER_EVENT";
    } else if (type == BEGIN_IDLE) {
      return "END_IDLE";
    } else if (type == BEGIN_PACK) {
      return "BEGIN_PACK";
    } else if (type == END_PACK) {
      return "END_PACK";
    } else if (type == BEGIN_UNPACK) {
      return "BEGIN_UNPACK";
    } else if (type == END_UNPACK) {
      return "END_UNPACK";
    } else if (type == CREATION_BCAST) {
      return "CREATION_BCAST";
    } else if (type == CREATION_MULTICAST) {
      return "CREATION_MULTICAST";
    } else if (type == USER_SUPPLIED) {
      return "USER_SUPPLIED";
    } else if (type == MEMORY_USAGE) {
      return "MEMORY_USAGE";
    } else if (type == USER_SUPPLIED_NOTE) {
      return "USER_SUPPLIED_NOTE";
    } else if (type == USER_SUPPLIED_BRACKETED_NOTE) {
      return "USER_SUPPLIED_BRACKETED_NOTE";
    } else if (type == BEGIN_USER_EVENT_PAIR) {
      return "BEGIN_USER_EVENT_PAIR";
    } else if (type == END_USER_EVENT_PAIR) {
      return "END_USER_EVENT_PAIR";
    } else if (type == USER_EVENT_PAIR) {
      return "USER_EVENT_PAIR";
    } else if (type == USER_STAT) {
      return "USER_STAT";
    } else if (type == NEW_CHARE_MSG) {
      return "NEW_CHARE_MSG";
    } else if (type == FOR_CHARE_MSG) {
      return "FOR_CHARE_MSG";
    } else if (type == BOC_INIT_MSG) {
      return "BOC_INIT_MSG";
    } else if (type == LDB_MSG) {
      return "LDB_MSG";
    } else if (type == QD_BOC_MSG) {
      return "QD_BOC_MSG";
    } else if (type == QD_BROADCAST_BOC_MSG) {
      return "QD_BROADCAST_BOC_MSG";
    } else {
      return "";
    }
  }

  public String toString() {
    String output = type + "";
    output += "," + typeToString();
    output += "," + mtype;
    output += "," + time;
    output += "," + endTime;
    output += "," + entry;
    output += "," + event;
    output += "," + pe;
    output += "," + numPEs;
    output += "," + msglen;
    output += "," + userEventID;
    output += "," + sendTime; // sendTime
    output += "," + recvTime; // the time the processor *actually* received the message.
    // public int id[]; // the thread id (3D array tuple). as of ver 9.0, it is a
    // 6-tuple
    output += "," + cpuStartTime; // start of cpu timer
    output += "," + cpuEndTime; // end of cpu timer
    output += "," + numPerfCounts; // number of performance counters
    // output += perfCounts[]; // the array of performance counts
    // output += destPEs[]; /// < list of multicast destination processors
    output += "," + userSupplied;
    output += "," + memoryUsage;
    // An arbitrary string provided by the user. Should be displayed as a user event
    // output += note;
    output += "," + nestedID; // Nested thread ID, e.g. virtual AMPI ranks
    return output;
  }

  public boolean isEndType() {
    return ((type == END_IDLE) ||
        (type == END_PACK) ||
        (type == END_UNPACK) ||
        (type == END_PROCESSING) ||
        (type == END_TRACE) ||
        (type == END_INTERRUPT));
  }

}
