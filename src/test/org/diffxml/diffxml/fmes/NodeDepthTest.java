package org.diffxml.diffxml.fmes;

import org.diffxml.diffxml.TestDocHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Test class for NodeDepth.
 * 
 * @author Adrian Mouat
 *
 */
public class NodeDepthTest {

    /** Test document. */
    private static Document mTestDoc1;
    
    /**
     * Initialises the test doc.
     */
    @BeforeClass
    public static void setUpTest() {
        
        String docString = "<x>text1<y><!-- comment --><z/>text2</y></x>";
        mTestDoc1 = TestDocHelper.createDocument(docString);
        
    }
    
    /** 
     * Helper method for testing nodes.
     *  
     * @param n The node to test
     * @param expectedDepth The expected depth of the node
     */
    private void testCreatingNodeDepth(final Node n, final int expectedDepth) {
        
        NodeDepth depthTest = new NodeDepth(n);
        assertEquals(expectedDepth, depthTest.getDepth());
        assertEquals(n, depthTest.getNode());
        assertEquals(n.getNodeName(), depthTest.getTag());
    }
    
    /**
     * Test calculating depth of nodes in document.
     */
    @Test
    public void testCorrectDepthCalculated() {
        
        //Try root node
        Element root = mTestDoc1.getDocumentElement();
        testCreatingNodeDepth(root, 0);
        
        //Try first text node
        Node text1 = root.getFirstChild();
        testCreatingNodeDepth(text1, 1);
        
        //y Node
        Node y = text1.getNextSibling();
        testCreatingNodeDepth(y, 1);
        
        //Comment node
        Node comment = y.getFirstChild();
        testCreatingNodeDepth(comment, 2);
        
        //z Node
        Node z = comment.getNextSibling();
        testCreatingNodeDepth(z, 2);
        
        //second text node
        Node text2 = z.getNextSibling();
        testCreatingNodeDepth(text2, 2);                
    }
    
    /**
     * Check a NullPointerException is thrown if handed a null node.
     */
    @Test(expected = NullPointerException.class)  
    public void testNull() {
        NodeDepth nullTest = new NodeDepth(null);
    }
}
