package org.diffxml.diffxml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.diffxml.diffxml.fmes.Fmes;
import org.diffxml.diffxml.fmes.ParserInitialisationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.junit.*;
import junit.framework.TestCase;

public class DiffXMLTest extends TestCase {

    @Test
	public final void testSimpleDiff() {
		Diff d = DiffFactory.createDiff();
		try {
            d.diff(new File("test1.xml"), new File("test2.xml"));
        } catch (DiffException e) {
            fail("An exception was thrown during the diff");
        }
		//Need to extend API with method to return document and actually
		//create a patchxml api!
		
	}
	
	/**
	 * Tests outputXML method.
	 */
    @Test
	public final void testOutputXML()	{
	    DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        try {
            Fmes.initParser(fac);
        } catch (ParserInitialisationException e) {
            fail("Could not initialise parser");
        }
        
            try {
                String docString = "<?xml version=\"1.0\" encoding=\"UTF-8\""
                    + " standalone=\"yes\"?><x>  <y> "
                    + " <z/> </y></x>";
                ByteArrayInputStream is = new ByteArrayInputStream(
                        docString.getBytes("utf-8"));
                Document doc = fac.newDocumentBuilder().parse(is);
                ByteArrayOutputStream os = new ByteArrayOutputStream(); 
                DiffXML.outputXML(doc, os);
                assertEquals(os.toString(), docString);
            } catch (SAXException e) {
                fail("Caught SAX Exception");
            } catch (IOException e) {
                fail("Caught IOException");
            } catch (ParserConfigurationException e) {
                fail("Caught ParserConfigurationException");
            }

	}
}
