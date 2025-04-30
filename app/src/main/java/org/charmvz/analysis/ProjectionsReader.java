package org.charmvz.analysis;

import java.io.File;


/**
 *  The base class for all future projections readers. It provides basic
 *  status flags and information that will facilitate future implementations 
 *  of caching of projections data reading.
 *
 *  UNAVAILABLE indicates that the source either does not exist or is not
 *  readable.
 *
 *  The use of ProjDefs as a superclass allows the reader to deal with 
 *  event types.
 *
 *  Base Assumption
 *  ---------------
 *  Each Reader is meant to be used for each file, typically a processor.
 *
 */
abstract class ProjectionsReader
extends Defs 
{ 
    protected String expectedVersion = null;

    // this can be any identifying string - full path name (default),
    // network address, URL etc ...
//	protected String sourceString;

    protected File sourceFile;
    
    /**
     *  INHERITANCE NOTE:
     *
     *  Any subclass *must* call this base class constructor for
     *  correctness.
     *
     *  any decision to read data at construction time must be made
     *  by the subclass' constructor. The default constructor will
     *  only perform the necessary checks.
     */
    protected ProjectionsReader(File sourceFile, String versionOverride) {
        expectedVersion = versionOverride;
        this.sourceFile = sourceFile;
        checkAvailable();
    }


    /**
     *  Implementing Subclasses should implement the code for determining
     *  if a specified source is available.
     *
     *  Here's an example using a File source:
     *
     *     protected boolean checkAvailable() {
     *        File sourceFile = new File(sourceString);
     *        // canRead() checks for existence by default.
     *        return sourceFile.canRead();
     *     }
     */
    protected abstract boolean checkAvailable();


}
