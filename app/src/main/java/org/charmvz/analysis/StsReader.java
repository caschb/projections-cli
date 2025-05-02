package org.charmvz.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class StsReader {
  private double version;
  private int NumPe;
  private int NodeSize;
  private int NumNodes;
  private boolean isSMP;
  private ZonedDateTime timestamp;
  private String charmVersion;
  private int TotalChares;
  private int EntryCount;
  private int TotalMsgs;
  private long[] MsgTable;
  private String[] userStatNames;
  private boolean hasPAPI;
  private int numPapiEvents;
  private String[] papiEventNames;
  private String Machine;
  private String commandline;
  private String username;
  private String hostname;
  private Chare[] Chares;

  private int entryIndex = 0;

  private Map<Integer, String> entryNames = new TreeMap<Integer, String>();
  public Map<Integer, Chare> entryChares = new TreeMap<Integer, Chare>();
  private Map<Integer, Integer> entryFlatToID = new TreeMap<Integer, Integer>();
  private Map<Integer, Integer> entryIDToFlat = new TreeMap<Integer, Integer>();

  private Hashtable<Integer, Integer> userEventIndices = new Hashtable<Integer, Integer>();
  private int userEventIndex = 0;

  private TreeMap<Integer, String> userEvents = new TreeMap<Integer, String>();
  private String userEventNames[];

  private HashMap<Integer, Integer> userStatIndices = new HashMap<Integer, Integer>();
  private int userStatIndex = 0;

  private TreeMap<Integer, String> userStats = new TreeMap<Integer, String>();
  private String baseName;

  private class Chare {
    protected String name;
    protected int dimensions;

    public Chare(String name, int dimensions) {
      this.name = name;
      this.dimensions = dimensions;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public StsReader(String filename)
      throws LogLoadException {
    try {
      BufferedReader InFile = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
      if (filename.endsWith(".sum.sts")) {
        baseName = filename.substring(0, filename.length() - 8);
      } else if (filename.endsWith(".sts")) {
        baseName = filename.substring(0, filename.length() - 4);
      } else {
        System.err.println("Invalid sts filename! Exiting ...");
        System.exit(-1);
      }
      int ID, ChareID;
      String Line, Name;
      while ((Line = InFile.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(Line);
        String s1 = st.nextToken();
        if (s1.equals("VERSION")) {
          version = Double.parseDouble(st.nextToken());
        } else if (s1.equals("MACHINE")) {
          Machine = matchQuotes(st);
        } else if (s1.equals("PROCESSORS")) {
          NumPe = Integer.parseInt(st.nextToken());
        } else if (s1.equals("SMPMODE")) {
          NodeSize = Integer.parseInt(st.nextToken());
          NumNodes = Integer.parseInt(st.nextToken());
          isSMP = true;
        } else if (s1.equals("TIMESTAMP")) {
          timestamp = ZonedDateTime.ofInstant(Instant.parse(st.nextToken()), ZoneId.systemDefault());
          String result = timestamp.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG));
        } else if (s1.equals("COMMANDLINE")) {
          commandline = matchQuotes(st);
        } else if (s1.equals("CHARMVERSION")) {
          charmVersion = st.nextToken();
        } else if (s1.equals("USERNAME")) {
          username = matchQuotes(st);
        } else if (s1.equals("HOSTNAME")) {
          hostname = matchQuotes(st);
        } else if (s1.equals("TOTAL_CHARES")) {
          TotalChares = Integer.parseInt(st.nextToken());
          Chares = new Chare[TotalChares];
        } else if (s1.equals("TOTAL_EPS")) {
          EntryCount = Integer.parseInt(st.nextToken());
        } else if (s1.equals("TOTAL_MSGS")) {
          TotalMsgs = Integer.parseInt(st.nextToken());
          MsgTable = new long[TotalMsgs];
        } else if (s1.equals("CHARE") || Line.equals("BOC")) {
          ID = Integer.parseInt(st.nextToken());
          String name = matchQuotes(st);
          int dimensions = -1;
          if (version >= 9.0) {
            dimensions = Integer.parseInt(st.nextToken());
          }
          Chares[ID] = new Chare(name, dimensions);
        } else if (s1.equals("ENTRY")) {
          st.nextToken(); // type
          ID = Integer.parseInt(st.nextToken());
          StringBuilder nameBuf = new StringBuilder(matchQuotes(st));
          Name = nameBuf.toString();
          if (-1 != Name.indexOf('(') && -1 == Name.indexOf(')')) {
            // Parse strings until we find the close-paren
            while (true) {
              String tmp = st.nextToken();
              nameBuf.append(" ");
              nameBuf.append(tmp);
              if (tmp.endsWith(")"))
                break;
            }
          }
          Name = nameBuf.toString();
          ChareID = Integer.parseInt(st.nextToken());
          st.nextToken(); // msgid

          entryFlatToID.put(entryIndex, ID);
          entryIDToFlat.put(ID, entryIndex);
          entryIndex++;
          getEntryNames().put(ID, Name);
          getEntryChare().put(ID, Chares[ChareID]);
        } else if (s1.equals("MESSAGE")) {
          ID = Integer.parseInt(st.nextToken());
          int Size = Integer.parseInt(st.nextToken());
          MsgTable[ID] = Size;
        } else if (s1.equals("EVENT")) {
          int key = Integer.parseInt(st.nextToken());
          if (!userEvents.containsKey(key)) {
            String eventName = "";
            while (st.hasMoreTokens()) {
              eventName = eventName + st.nextToken() + " ";
            }
            userEvents.put(key, eventName);
            userEventNames[userEventIndex] = eventName;
            userEventIndices.put(key, userEventIndex++);
          }
        } else if (s1.equals("TOTAL_EVENTS")) {
          // restored by Chee Wai - 7/29/2002
          userEventNames = new String[Integer.parseInt(st.nextToken())];
        } else if (s1.equals("STAT")) {
          int key = Integer.parseInt(st.nextToken());
          if (!userStats.containsKey(key)) {
            String statName = "";
            while (st.hasMoreTokens()) {
              statName = statName + st.nextToken() + " ";
            }
            userStats.put(key, statName);
            userStatNames[userStatIndex] = statName;
            userStatIndices.put(key, userStatIndex++);
          }
          // Read in number of stats
        } else if (s1.equals("TOTAL_STATS")) {
          userStatNames = new String[Integer.parseInt(st.nextToken())];
        } else if (s1.equals("TOTAL_PAPI_EVENTS")) {
          hasPAPI = true;
          numPapiEvents = Integer.parseInt(st.nextToken());
          papiEventNames = new String[numPapiEvents];
        } else if (s1.equals("PAPI_EVENT")) {
          hasPAPI = true;
          papiEventNames[Integer.parseInt(st.nextToken())] = st.nextToken();
        } else if (s1.equals("END")) {
          break;
        }
      }

      InFile.close();
    } catch (FileNotFoundException e) {
      throw new LogLoadException(filename);
    } catch (IOException e) {
      throw new LogLoadException(filename);
    }
  }

  private static String matchQuotes(StringTokenizer st) {
    String current = st.nextToken();
    // If string doesn't start with a quote, then we've already matched
    if (!current.startsWith("\"")) {
      return current;
    }

    // If it starts and ends with a quote, then we've already matched
    if (current.endsWith("\"") && current.length() >= 2) {
      return current.substring(1, current.length() - 1);
    }

    // Otherwise, start concatenating strings until we find a closing quote
    StringBuilder value = new StringBuilder(current.substring(1));
    while (st.hasMoreTokens()) {
      current = st.nextToken();
      value.append(" " + current);
      if (current.endsWith("\"")) {
        return value.substring(0, value.length() - 1);
      }
    }

    return "";
  }

  public Map<Integer, String> getEntryNames() {
    return entryNames;
  }

  public Map<Integer, Chare> getEntryChare() {
    return entryChares;
  }

  public String getBaseName() {
    return baseName;
  }

  public int getProcessorCount() {
    return NumPe;
  }

  public int getEntryCount() {
    return EntryCount;
  }

  public String getEntryFullNameByID(int id) {
    return getEntryChareNameByID(id) + "::" + getEntryNameByID(id);
  }

  private String getEntryNameByID(int id) {
    return getEntryNames().get(id);
  }

  private String getEntryChareNameByID(int id) {
    return getEntryChare().get(id).name;
  }

  public int getEntryChareDimensionsByID(int entry) {
    return getEntryChare().get(entry).dimensions;
  }

  public int getNumPerfCounts() {
    if (hasPAPI) {
      return numPapiEvents;
    } else {
      return 0;
    }
  }

  public Integer getEntryIndex(int id) {
    if (id < 0)
      return id;
    return entryIDToFlat.get(id);
  }

  public double getVersion() {
    return version;
  }
}
