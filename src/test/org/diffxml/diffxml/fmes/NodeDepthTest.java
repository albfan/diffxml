package org.diffxml.diffxml.fmes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.fail;
import static junit.framework.Assert.assertEquals;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Test class for NodeDepth.
 * 
 * @author Adrian Mouat
 *
 */
public class NodeDepthTest {

    private static Document mTestDoc1;
    
    @BeforeClass
    public static void setUpTest() {
        
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        try {
            Fmes.initParser(fac);
        } catch (ParserInitialisationException e) {
            fail("Could not initialise parser");
        }
        
        String docString = "<x>  <y> <z/> </y></x>";
        
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(
                    docString.getBytes("utf-8"));
            mTestDoc1 = fac.newDocumentBuilder().parse(is);
        } catch (UnsupportedEncodingException e) {
            fail("No utf-8 encoder!");
        } catch (ParserConfigurationException e) {
            fail("Error configuring parser: " + e.getMessage());
        } catch (IOException e) {
            fail("Caught IOException: " + e.getMessage());
        } catch (SAXException e) {
            fail("Caught SAXexception: " + e.getMessage());
        }

    }
    
    @Test
    public void testCorrectDepthCalculated() {
        
        NodeDepth rootTest = new NodeDepth(mTestDoc1.getDocumentElement());
        assertEquals(rootTest.getDepth(), 0);
        assertEquals(rootTest.getNode(), mTestDoc1.getDocumentElement());
        assertEquals(rootTest.getTag(), 
                mTestDoc1.getDocumentElement().getNodeName());
        
    }
}
