package org.diffxml.diffxml;

import static junit.framework.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.diffxml.diffxml.fmes.Fmes;
import org.diffxml.diffxml.fmes.ParserInitialisationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Helper class for XML tests.
 * 
 * @author Adrian Mouat
 *
 */
public final class TestDocHelper {

    /**
     * Private constructor.
     */
    private TestDocHelper() {
        //SHouldn't be called
    }
    
    /**
     * Create an XML Document from a string of XML.
     * 
     * Calls fail if any exception is thrown.
     * 
     * @param xml The XML to turn into a document.
     * @return A DOM document representing the string.
     */
    public static Document createDocument(final String xml) {
        
        Document ret = null;
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        try {
            Fmes.initParser(fac);
        } catch (ParserInitialisationException e) {
            fail("Could not initialise parser");
        }
                
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(
                    xml.getBytes("utf-8"));
            ret = fac.newDocumentBuilder().parse(is);
        } catch (UnsupportedEncodingException e) {
            fail("No utf-8 encoder!");
        } catch (ParserConfigurationException e) {
            fail("Error configuring parser: " + e.getMessage());
        } catch (IOException e) {
            fail("Caught IOException: " + e.getMessage());
        } catch (SAXException e) {
            fail("Caught SAXexception: " + e.getMessage());
        }
        
        return ret;

    }
}
