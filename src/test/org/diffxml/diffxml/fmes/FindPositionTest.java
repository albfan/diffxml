package org.diffxml.diffxml.fmes;

import static org.junit.Assert.assertEquals;

import org.diffxml.diffxml.TestDocHelper;
import org.junit.Test;
import org.w3c.dom.Document;


import org.w3c.dom.Node;

/**
 * Test FindPosition works properly
 * @author Adrian Mouat
 *
 */
public class FindPositionTest {

       
    @Test
    public void testSimpleInsert() {
        /** 
         * Complicated test. Need two doc fragments that have been matched.
         * Check insert position is as expected 
         */
        Document testDoc1 = TestDocHelper.createDocument("<a><b/></a>");
        Document testDoc2 = TestDocHelper.createDocument("<a><b/><c/></a>");
        
        NodePairs pairs = Match.easyMatch(testDoc1, testDoc2);
        
        Node c = testDoc2.getFirstChild().getFirstChild().getNextSibling();
        assertEquals("c,", c.getNodeName());
        FindPosition fp = new FindPosition(c, pairs);
        assertEquals(1, fp.getDOMInsertPosition());
        assertEquals(2, fp.getXPathInsertPosition());
        assertEquals(1, fp.getCharInsertPosition());
    }
 
}
