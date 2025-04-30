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

    public String toString() {
        return "";
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
