package org.diffxml.diffxml.fmes;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static junit.framework.Assert.fail;

import java.io.IOException;
import java.util.logging.Level;

import org.diffxml.diffxml.DiffXML;
import org.diffxml.diffxml.TestDocHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Class to test matching algorithm.
 * 
 * TODO: Add more tests for similar docs and other node types.
 * 
 * @author Adrian Mouat
 *
 */
public class MatchTest {

    /** Test Doc 1a. */
    private static Document mTestDoc1a;
    
    /** Test Doc 1b. */
    private static Document mTestDoc1b;

    /** Test Doc 2a. */
    private static Document mTestDoc2a;

    /** Test Doc 2b. */
    private static Document mTestDoc2b;

    /** Test Doc 3a. */
    private static Document mTestDoc3a;

    /** Test Doc 3b. */
    private static Document mTestDoc3b;
    
    /** Test Doc 4a. */
    private static Document mTestDoc4a;
    
    /**
     * Set up logger and documents before test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        
        try {
            DiffXML.initLog();
            DiffXML.LOG.setLevel(Level.OFF);
        } catch (IOException e) {
            fail("Exception setting up logger");
        }
        mTestDoc1a = TestDocHelper.createDocument("<a><b><c/></b></a>");
        mTestDoc1b = TestDocHelper.createDocument("<a><b><c/></b></a>");
        
        mTestDoc2a = TestDocHelper.createDocument(
                "<a>text1<b attr='b'><!-- comment --></b></a>");
        mTestDoc2b = TestDocHelper.createDocument(
                "<a>text1<b attr='b'><!-- comment --></b></a>");
        
        mTestDoc3a = TestDocHelper.createDocument("<x><y><z/></y></x>");
        mTestDoc3b = TestDocHelper.createDocument(
                "<x>different<y><!-- different --></y></x>");
        
        mTestDoc4a = TestDocHelper.createDocument(
                "<a>newtext<b attr='c'><!-- comment --></b></a>");
    }

    /**
     * Just make sure a simple identical document with only elements matches
     * correctly. 
     */
    @Test
    public final void testSimpleIdenticalDoc() {
        
        NodePairs all = Match.easyMatch(mTestDoc1a, mTestDoc1b);
        
        Node aRoot = mTestDoc1a.getDocumentElement();
        Node partner = all.getPartner(aRoot);
        
        Node bRoot = mTestDoc1b.getDocumentElement();
        assertEquals(bRoot, partner);
        
        Node aB = aRoot.getFirstChild();
        partner = all.getPartner(aB);
        
        Node bB = bRoot.getFirstChild();
        assertEquals(bB, partner);
        
        Node aC = aB.getFirstChild();
        partner = all.getPartner(aC);
        
        Node bC = bB.getFirstChild();
        assertEquals(bC, partner);
    }
    
    /**
     * Now test identical doc with comments and text matches correctly. 
     */
    @Test
    public final void testIdenticalDocWithTextAndComments() {

        NodePairs all = Match.easyMatch(mTestDoc2a, mTestDoc2b);
        
        Node aRoot = mTestDoc2a.getDocumentElement();
        Node partner = all.getPartner(aRoot);
        
        Node bRoot = mTestDoc2b.getDocumentElement();
        assertEquals(bRoot, partner);
        
        Node aText = aRoot.getFirstChild();
        partner = all.getPartner(aText);
        
        Node bText = bRoot.getFirstChild();
        assertEquals(bText, partner);

        Node aB = aText.getNextSibling();
        partner = all.getPartner(aB);
        
        Node bB = bText.getNextSibling();
        assertEquals(bB, partner);

        Node aComment = aB.getFirstChild();
        partner = all.getPartner(aComment);
        
        Node bComment = bB.getFirstChild();
        assertEquals(bComment, partner);
    }
    
    /**
     * Test completely different docs - should be no matches.
     */
    @Test
    public final void testDifferentDocs() {
        
        NodePairs matches = Match.easyMatch(mTestDoc1a, mTestDoc3a);
        assertEquals(0, matches.size());
        
        matches = Match.easyMatch(mTestDoc2a, mTestDoc3b);
        assertEquals(0, matches.size());
    }

    /**
     * Test similar docs match partly.
     */
    @Test
    public final void testSimilarDocs() {
        
        //<a>text1<b attr='b'><!-- comment --></b></a>
        //<a>newtext<b attr='c'><!-- comment --></b></a>
        
        NodePairs matches = Match.easyMatch(mTestDoc2a, mTestDoc4a);
        
        Node aRoot = mTestDoc2a.getDocumentElement();
        Node partner = matches.getPartner(aRoot);
        
        Node bRoot = mTestDoc4a.getDocumentElement();
        assertEquals(bRoot, partner);
        
        Node aText = aRoot.getFirstChild();
        assertNull(matches.getPartner(aText));
        
        Node aB = aText.getNextSibling();
        assertNull(matches.getPartner(aB));

        Node aComment = aB.getFirstChild();
        partner = matches.getPartner(aComment);
        
        Node bComment = bRoot.getFirstChild().getNextSibling().getFirstChild();
        assertEquals(bComment, partner);
        
    }

}
