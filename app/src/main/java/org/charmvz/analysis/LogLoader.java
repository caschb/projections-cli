package org.charmvz.analysis;

import java.io.IOException;
import java.util.SortedSet;
import org.charmvz.misc.LogEntry;

public class LogLoader extends Defs {

  public long determineEndTime(SortedSet<Integer> validPEs, Analysis analysis) {
    long endTime = Long.MIN_VALUE;

    for (var pe : validPEs) {
      GenericLogReader reader = new GenericLogReader(pe, analysis);
      try {
        while (true) {
          LogEntry entry = reader.nextEvent();
          if (entry.time > endTime) {
            endTime = entry.time;
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(-1);
      } catch (EndOfLogSuccess e) {
      }
      try {
        reader.close();
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(-1);
      }
    }

    return endTime;
  }

}
