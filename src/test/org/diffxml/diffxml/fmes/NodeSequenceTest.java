package org.diffxml.diffxml.fmes;

import static org.junit.Assert.fail;

import org.diffxml.diffxml.TestDocHelper;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test for NodeSequence class.
 * 
 * @author Adrian Mouat
 *
 */
public class NodeSequenceTest {

    @Test
    public final void testGetSequence() {
        
        Document seq1 = TestDocHelper.createDocument(
            "<a><b/><c/><d/></a>");
        Document seq2 = TestDocHelper.createDocument(
            "<a><b/><c/><d/></a>");

        NodePairs pairs = Match.easyMatch(seq1, seq2);
        
        NodeSequence.getSequence(seq1.getDocumentElement().getChildNodes(), 
                seq2.getDocumentElement().getChildNodes(), pairs);
        
        fail("not written yet");
    }
    
}
