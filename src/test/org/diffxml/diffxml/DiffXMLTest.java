package org.diffxml.diffxml;

import junit.framework.TestCase;

public class DiffXMLTest extends TestCase {

	public void testSimpleDiff()
	{
		Diff d = DiffFactory.createDiff();
		d.diff("/home/adz/diffxml/r1.xml", "/home/adz/diffxml/r2.xml");
		//Need to extend API with method to return document and actually
		//create a patchxml api!
		
	}
}
