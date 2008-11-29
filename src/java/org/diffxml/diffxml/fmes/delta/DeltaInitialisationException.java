package org.diffxml.diffxml.fmes.delta;

/**
 * Indicates the Delta document could not be created.
 * 
 * @author Adrian Mouat
 *
 */
public class DeltaInitialisationException extends Exception {
 
    /**
     * Serial ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param e Chained exception
     */
    DeltaInitialisationException(final Exception e) {
        super(e);
    }
    
    /**
     * Constructor.
     * 
     * @param s Description of error
     * @param e Chained exception
     */
    DeltaInitialisationException(final String s, final Exception e) {
        super(s, e);
    }
}
