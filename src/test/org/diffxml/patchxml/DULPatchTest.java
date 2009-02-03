package org.diffxml.patchxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static junit.framework.Assert.fail;

import org.diffxml.diffxml.TestDocHelper;
import org.diffxml.patchxml.DULPatch;
import org.diffxml.patchxml.PatchFormatException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

/**
 * Class to test applying DUL Patches.
 * 
 * @author Adrian Mouat
 *
 */
public class DULPatchTest {

    /**
     * Simple insert operation.
     */
    @Test
    public final void testSimpleInsert() {
        
        Document doc1 = TestDocHelper.createDocument("<a></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<insert parent=\"/a\" nodetype=\"1\" "
                + "childno=\"1\" name=\"b\"/>"
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            assertEquals("b", 
                    doc1.getDocumentElement().getFirstChild().getNodeName());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }

    /**
     * Insert element after text.
     */
    @Test
    public final void testInsertAfterText() {
        
        Document doc1 = TestDocHelper.createDocument("<a>text</a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<insert parent=\"/a\" nodetype=\"1\" "
                + "childno=\"2\" name=\"b\" charpos=\"5\" />"
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            Node textNode = doc1.getDocumentElement().getFirstChild();
            assertEquals("text", textNode.getNodeValue());
            assertEquals("b", textNode.getNextSibling().getNodeName());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }
    
    /**
     * Insert element before text.
     */
    @Test
    public final void testInsertBeforeText() {
        
        Document doc1 = TestDocHelper.createDocument("<a>text</a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<insert parent=\"/a\" nodetype=\"1\" "
                + "childno=\"1\" name=\"b\" charpos=\"1\" />"
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            Node b = doc1.getDocumentElement().getFirstChild();
            assertEquals("b", b.getNodeName());
            assertEquals("text", b.getNextSibling().getNodeValue());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }

    /**
     * Insert element into text.
     */
    @Test
    public final void testInsertIntoText() {
        
        Document doc1 = TestDocHelper.createDocument("<a>text</a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<insert parent=\"/a\" nodetype=\"1\" "
                + "childno=\"2\" name=\"b\" charpos=\"2\" />"
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            Node text1 = doc1.getDocumentElement().getFirstChild();
            assertEquals("t", text1.getNodeValue());
            assertEquals("b", text1.getNextSibling().getNodeName());
            assertEquals("ext", 
                    text1.getNextSibling().getNextSibling().getNodeValue());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }

    /**
     * Insert element into text.
     */
    @Test
    public final void testInsertIntoText2() {
        
        Document doc1 = TestDocHelper.createDocument("<a>xy<b/></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<insert charpos=\"2\" childno=\"2\" name=\"p\" "
                + "nodetype=\"1\" parent=\"/node()[1]\"/>"
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            Node text1 = doc1.getDocumentElement().getFirstChild();
            assertEquals("x", text1.getNodeValue());
            assertEquals("p", text1.getNextSibling().getNodeName());
            assertEquals("y", 
                    text1.getNextSibling().getNextSibling().getNodeValue());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }
    
    /**
     * Test inserting attribute.
     */
    @Test
    public final void testInsertingAttr() {
        
        Document doc1 = TestDocHelper.createDocument("<a><b/></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<insert parent=\"/a/node()[1]\" nodetype=\"2\" "
                + "name=\"attr\">val</insert>"
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            Element b = (Element) doc1.getDocumentElement().getFirstChild();
            assertEquals("b", b.getNodeName());
            assertEquals("val", b.getAttribute("attr"));
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }

    /**
     * Test inserting comment.
     */
    @Test
    public final void testInsertingComment() {
        
        Document doc1 = TestDocHelper.createDocument("<a></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<insert parent=\"/a\" nodetype=\"" + Node.COMMENT_NODE + "\""
                + ">val</insert>"
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            Node comment = doc1.getDocumentElement().getFirstChild();
            assertEquals(Node.COMMENT_NODE, comment.getNodeType());
            assertEquals("val", comment.getNodeValue());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }

    /**
     * Test simple delete operation.
     */
    @Test
    public final void testSimpleDelete() {

        Document doc1 = TestDocHelper.createDocument("<a><b/></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<delete node=\"/a/node()[1]\" />" 
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            assertNull(doc1.getDocumentElement().getFirstChild());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }

    /**
     * Test deleting attribute.
     */
    @Test
    public final void testDeleteAttribute() {

        Document doc1 = TestDocHelper.createDocument(
                "<a><b attr=\"val\"/></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<delete node=\"/a/node()[1]/@attr\" />" 
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            assertEquals("", ((Element) doc1.getDocumentElement(
                    ).getFirstChild()).getAttribute("attr"));
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }

    /**
     * Test deleting comment.
     */
    @Test
    public final void testDeleteComment() {

        Document doc1 = TestDocHelper.createDocument(
                "<a><!-- comment --></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<delete node=\"/a/node()[1]\" />" 
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            assertNull(doc1.getDocumentElement().getFirstChild());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }

    /**
     * Test deleting text.
     *
     * Test deleting whole text node and part of.
     */
    @Test
    public final void testDeleteText() {

        Document doc1 = TestDocHelper.createDocument(
                "<a>12<b/>3456</a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<delete node=\"/a/node()[1]\" />" 
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            assertEquals("b", doc1.getDocumentElement().getFirstChild(
                        ).getNodeName());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }

        patch = TestDocHelper.createDocument(
                "<delta>"
                + "<delete node=\"/a/node()[2]\" "
                + "charpos=\"2\" length=\"2\"/>" 
                + "</delta>");
        try {
            (new DULPatch()).apply(doc1, patch);
            assertTrue(doc1.getDocumentElement().getFirstChild(
                        ).getNextSibling() != null);
            assertEquals("36", doc1.getDocumentElement().getFirstChild(
                        ).getNextSibling().getNodeValue());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }

        patch = TestDocHelper.createDocument(
                "<delta>"
                + "<delete node=\"/a/node()[2]\" "
                + "charpos=\"1\" length=\"1\"/>" 
                + "</delta>");
        try {
            (new DULPatch()).apply(doc1, patch);
            assertTrue(doc1.getDocumentElement().getFirstChild(
                        ).getNextSibling() != null);
            assertEquals("6", doc1.getDocumentElement().getFirstChild(
                        ).getNextSibling().getNodeValue());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }

    /**
     * Test simple move operation.
     */
    @Test
    public final void testSimpleMove() {

        Document doc1 = TestDocHelper.createDocument("<a><b/><c><d/></c></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<move node=\"/a/node()[2]/node()[1]\" " 
                + "parent=\"/a/node()[1]\" childno=\"1\" />" 
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            assertEquals("d", 
                    doc1.getDocumentElement().getFirstChild().getFirstChild(
                        ).getNodeName());
            assertNull(doc1.getDocumentElement().getFirstChild(
                        ).getNextSibling().getFirstChild());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }

    /**
     * Test moving into text.
     */
    @Test
    public final void testMoveIntoText() {

        Document doc1 = TestDocHelper.createDocument(
                "<a><b>text</b><c><d/></c></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<move node=\"/a/node()[2]/node()[1]\" " 
                + "parent=\"/a/node()[1]\" childno=\"2\" "
                + "new_charpos=\"3\"/>" 
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            Node b = doc1.getDocumentElement().getFirstChild();
            assertNull(b.getNextSibling().getFirstChild());
            assertEquals("te", b.getFirstChild().getNodeValue());
            assertEquals("d", 
                    b.getFirstChild().getNextSibling().getNodeName());
            assertEquals("xt", b.getFirstChild().getNextSibling(
                        ).getNextSibling().getNodeValue());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }

    /**
     * Test moving part of text.
     */
    @Test
    public final void testMovePartOfText() {

        Document doc1 = TestDocHelper.createDocument(
                "<a><b></b><c>text</c></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<move node=\"/a/node()[2]/node()[1]\" " 
                + "parent=\"/a/node()[1]\" childno=\"1\" "
                + "old_charpos=\"2\" length=\"2\" new_charpos=\"3\"/>" 
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            Node b = doc1.getDocumentElement().getFirstChild();
            assertEquals("tt", 
                    b.getNextSibling().getFirstChild().getNodeValue());
            assertEquals("ex", b.getFirstChild().getNodeValue());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }
    
    /**
     * Test moves don't count moved node.
     */
    @Test
    public final void testMoveCount() {

        Document doc1 = TestDocHelper.createDocument(
                "<a><b/><c/><d/></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<move node=\"/a/node()[1]\" " 
                + "parent=\"/a\" childno=\"2\"/>" 
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            Node c = doc1.getDocumentElement().getFirstChild();
            assertEquals("c", c.getNodeName());
            assertEquals("b", c.getNextSibling().getNodeName());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }
    
    /**
     * Test update of element.
     */
    @Test
    public final void testUpdateElement() {

        Document doc1 = TestDocHelper.createDocument(
                "<a><b/></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<update node=\"/a/b\">c</update>" 
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            Node c = doc1.getDocumentElement().getFirstChild();
            assertEquals("c", c.getNodeName());
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }
    
    /**
     * Test update of attribute.
     */
    @Test
    public final void testUpdateAttribute() {

        Document doc1 = TestDocHelper.createDocument(
                "<a><b attr=\"test\"/></a>");
        Document patch = TestDocHelper.createDocument(
                "<delta>"
                + "<update node=\"/a/b/@attr\">newval</update>"  
                + "</delta>");

        try {
            (new DULPatch()).apply(doc1, patch);
            Node c = doc1.getDocumentElement().getFirstChild();
            assertEquals("newval", ((Element) c).getAttribute("attr"));
        } catch (PatchFormatException e) {
            fail("Caught exception " + e);
        }
    }
}
