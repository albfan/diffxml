
package org.diffxml.patchxml;
import junit.framework.*;
import org.diffxml.patchxml.PatchXML;

public class PatchXMLTest extends TestCase 
{

    public PatchXMLTest(String name)
        {
        super(name);
        }

    public void setUp()
        {
        //PatchXML._docFile = "tree";
        //PatchXML._patchFile = "notree";
        }

    public void testFilesExist()
        {
        //assertEquals(true, PatchXML.checkFilesExistAndWarn());
        }

    public static Test suite()
        {
        return new TestSuite(PatchXMLTest.class);
        }

    public static void main(String args[])
        {
        junit.textui.TestRunner.run(suite());
        }
}
