package org.diffxml.diffxml.fmes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.diffxml.diffxml.TestDocHelper;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Test main EditScript class and methods.
 * 
 * @author Adrian Mouat
 *
 */
public class EditScriptTest {
    
   /**
    * Test hanlding documents with different roots.
    */
    @Test
    public void testMatchingRootNodes() {
        
        //Shouldn't do anything if roots match
        Document test1 = TestDocHelper.createDocument("<a><b/></a>");
        Document test2 = TestDocHelper.createDocument("<a><b/></a>");
        NodePairs matchings = Match.easyMatch(test1, test2);
        (new EditScript(test1, test2, matchings)).matchRoots();
        
        assertEquals("a", test1.getDocumentElement().getNodeName());
        assertEquals("b", 
                test1.getDocumentElement().getFirstChild().getNodeName());
        assertEquals("a", test2.getDocumentElement().getNodeName());
        assertEquals("b", 
                test2.getDocumentElement().getFirstChild().getNodeName());
        
        //Should add dummy node if they don't
        test1 = TestDocHelper.createDocument("<a><b/></a>");
        test2 = TestDocHelper.createDocument("<c><b/></c>");
        matchings = Match.easyMatch(test1, test2);
        (new EditScript(test1, test2, matchings)).matchRoots();
        
        assertEquals("DUMMY", test1.getDocumentElement().getNodeName());
        assertEquals("a", 
                test1.getDocumentElement().getFirstChild().getNodeName());
        assertEquals("b", 
                test1.getDocumentElement().getFirstChild().getFirstChild(
                        ).getNodeName());

        assertEquals("DUMMY", test2.getDocumentElement().getNodeName());
        assertEquals("c", 
                test2.getDocumentElement().getFirstChild().getNodeName());
        assertEquals("b", 
                test2.getDocumentElement().getFirstChild().getFirstChild(
                        ).getNodeName());
        
        assertEquals(matchings.getPartner(test2.getDocumentElement()), 
                test1.getDocumentElement());
    }
    
    /**
     * Test the simple addition of an element.
     */
    @Test
    public final void testSimpleInsert() {

        Document doc1 = TestDocHelper.createDocument("<a><b/></a>");
        Document doc2 = TestDocHelper.createDocument("<a><b/><c/></a>");
        NodePairs matchings = Match.easyMatch(doc1, doc2);
        assertEquals(4, matchings.size());
        assertNull(matchings.getPartner(doc2.getFirstChild().getFirstChild(
                ).getNextSibling()));
        EditScript es = new EditScript(doc1, doc2, matchings);
        Document res = null;
        try {
            res = es.create();
        } catch (DocumentCreationException e) {
            fail("Caught Exception");
        }
        Node insert = res.getFirstChild().getFirstChild();
        assertEquals("insert", insert.getNodeName());
        NamedNodeMap attrs = insert.getAttributes();
        assertEquals("2", attrs.getNamedItem("childno").getNodeValue());
        assertEquals("c", attrs.getNamedItem("name").getNodeValue());
        assertEquals("/a", attrs.getNamedItem("parent").getNodeValue());
        assertEquals("1", attrs.getNamedItem("nodetype").getNodeValue());
    }

    /**
     * Test the simple deletion of an element.
     */
    @Test
    public final void testSimpleDeletion() {

        Document doc1 = TestDocHelper.createDocument("<a><b/><c/></a>");
        Document doc2 = TestDocHelper.createDocument("<a><b/></a>");
        NodePairs matchings = Match.easyMatch(doc1, doc2);
        assertEquals(4, matchings.size());
        assertNull(matchings.getPartner(doc1.getFirstChild().getFirstChild(
                ).getNextSibling()));
        EditScript es = new EditScript(doc1, doc2, matchings);
        Document res = null;
        try {
            res = es.create();
        } catch (DocumentCreationException e) {
            fail("Caught Exception");
        }
        Node delete = res.getFirstChild().getFirstChild();
        assertEquals("delete", delete.getNodeName());
        NamedNodeMap attrs = delete.getAttributes();
        assertEquals("/a/node()[2]", attrs.getNamedItem("node").getNodeValue());
    }
    
    /**
     * Test the simple deletion of an element.
     */
    @Test
    public final void testSimpleMove() {

        Document doc1 = TestDocHelper.createDocument("<a><b><c/></b><d/></a>");
        Document doc2 = TestDocHelper.createDocument("<a><b/><d><c/></d></a>");
        NodePairs matchings = Match.easyMatch(doc1, doc2);
        assertEquals(8, matchings.size());
        EditScript es = new EditScript(doc1, doc2, matchings);
        Document res = null;
        try {
            res = es.create();
        } catch (DocumentCreationException e) {
            fail("Caught Exception");
        }
        Node move = res.getFirstChild().getFirstChild();
        assertEquals("move", move.getNodeName());
        NamedNodeMap attrs = move.getAttributes();
        assertEquals("/a/node()[1]/node()[1]", 
                attrs.getNamedItem("node").getNodeValue());
        assertEquals("1", 
                attrs.getNamedItem("childno").getNodeValue());
        assertEquals("/a/node()[2]", 
                attrs.getNamedItem("parent").getNodeValue());    

    }

}
