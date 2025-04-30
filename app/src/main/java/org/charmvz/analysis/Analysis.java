package org.charmvz.analysis;

import java.io.IOException;

import org.charmvz.analysis.StsReader;

public class Analysis {
  
  private StsReader stsReader;

  public void initAnalysis(String filename) throws IOException {

    try {
      
    stsReader = new StsReader(filename);
    } catch(LogLoadException e) {
      System.err.println(e);
      System.exit(-1);
    }
    
  }
}
