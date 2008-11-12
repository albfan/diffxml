/*
Program to difference two XML files
 
Copyright (C) 2002  Adrian Mouat
 
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 
Author: Adrian Mouat
email: amouat@postmaster.co.uk
*/

package org.diffxml.diffxml.fmes;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.diffxml.diffxml.DiffFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element; 

/**
 * Handle operations related to creating a DUL delta.
 * 
 * TODO: Create interface to allow different formats to be plugged in
 * 
 * @author Adrian Mouat
 */
public class DULDelta {

    private static final String RESOLVE_ENTITIES = "resolve_entities";
    private static final String FALSE = "false";
    private static final String TRUE = "true";
    private static final String REVERSE_PATCH = "reverse_patch";
    private static final String PARENT_SIBLING_CONTEXT = "par_sib_context";
    private static final String PARENT_CONTEXT = "par_context";
    private static final String SIBLING_CONTEXT = "sib_context";
    private static final String DELTA = "delta";
    
    private static final String CONTEXT = "context";
    private static final String NEW_CHARPOS = "new_charpos";
    private static final String OLD_CHARPOS = "old_charpos";
    private static final String MOVE = "move";
    private static final String LENGTH = "length";
    private static final String NODE = "node";
    private static final String DELETE = "delete";
    private static final String CHARPOS = "charpos";
    private static final String NAME = "name";
    private static final String CHILDNO = "childno";
    private static final String NODETYPE = "nodetype";
    private static final String PARENT = "parent";
    private static final String INSERT = "insert";

    private Document mEditScript;
    
    public DULDelta() throws DeltaInitialisationException {
        
        try {
            mEditScript = makeEmptyEditScript();
        } catch (ParserConfigurationException e) {
            throw new DeltaInitialisationException(e);
        }
    }
    
    public Document getDocument() {
        return mEditScript;
    }
    
    /** Prepares an empty Edit Script document.
    *
    * Makes root element, appends any necessary attributes
    * and context information.
    *
    * @return a properly formatted, empty edit script
    * @throws ParserConfigurationException If a new document can't be created
    */

   private static Document makeEmptyEditScript() 
   throws ParserConfigurationException {

       DocumentBuilder builder = 
           DocumentBuilderFactory.newInstance().newDocumentBuilder();
       Document editScript = builder.newDocument();

       Element root = editScript.createElement(DELTA);

       //Append any context information
       if (DiffFactory.isContext()) {
           root.setAttribute(SIBLING_CONTEXT, 
                   Integer.toString(DiffFactory.getSiblingContext()));
           root.setAttribute(PARENT_CONTEXT,
                   Integer.toString(DiffFactory.getParentContext()));
           root.setAttribute(PARENT_SIBLING_CONTEXT,
                   Integer.toString(DiffFactory.getParentSiblingContext()));
       }

       if (DiffFactory.isReversePatch()) {
           root.setAttribute(REVERSE_PATCH, TRUE);
       }

       if (!DiffFactory.isResolveEntities()) {
           root.setAttribute(RESOLVE_ENTITIES, FALSE);
       }

       editScript.appendChild(root);

       return editScript;
   }
   
    /**
     * Adds inserts for attributes of a node to an EditScript.
     * 
     * @param attrs
     *            the attributes to be added
     * @param path
     *            the path to the node they are to be added to
     * @param editScript
     *            the Edit Script to add the inserts to
     */
    public void addAttrsToDelta(final NamedNodeMap attrs,
            final String path) {

        int numAttrs;
        if (attrs == null) {
            numAttrs = 0;
        } else {
            numAttrs = attrs.getLength();
        }

        for (int i = 0; i < numAttrs; i++) {
            insert(attrs.item(i), path, 0, 1);
        }
    }

    /**
     * Appends an insert operation to the EditScript given the inserted node, 
     * XPath to parent, character position & child number.
     * 
     * @param n The node to insert
     * @param parent The path to the node to be parent of n
     * @param childno The child number of the parent node that n will become
     * @param charpos The character position to insert at
     * @param es The EditScript 
     */
    public void insert(final Node n, final String parent, 
            final int childno, final int charpos) {

        Element ins = mEditScript.createElement(INSERT);
        
        ins.setAttribute(PARENT, parent);
        ins.setAttribute(NODETYPE, ("" + n.getNodeType()));

        if (n.getNodeType() != Node.ATTRIBUTE_NODE) {
            ins.setAttribute(CHILDNO, "" + childno);
        }

        if (n.getNodeType() == Node.ATTRIBUTE_NODE 
                || n.getNodeType() == Node.ELEMENT_NODE 
                || n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
            ins.setAttribute(NAME, n.getNodeName());
        }

        //XPath character position starts at 1
        assert charpos >= 1 : charpos;
        
        if (charpos != 1) {
            ins.setAttribute(CHARPOS, ("" + charpos));
        }

        String value = n.getNodeValue();
        if (value != null) {
            Node txt = mEditScript.createTextNode(value);
            ins.appendChild(txt);
        }

        mEditScript.getDocumentElement().appendChild(ins);
        
        // Add any attributes
        if (n.getNodeType() == Node.ELEMENT_NODE) {
            //TODO: Update for using element names instead of node()
            addAttrsToDelta(n.getAttributes(), 
                    parent + "/node()[" + childno + "]");
        }
    }

    public void delete(Node n) {
        
        Element del = mEditScript.createElement(DELETE);
        del.setAttribute(NODE, NodeOps.getXPath(n));
        
        if (n.getNodeType() == Node.TEXT_NODE) {
            
            ChildNumber cn = new ChildNumber(n);
            int charpos = cn.getXPathCharPos();
            
            if (charpos >= 1) {
                del.setAttribute(CHARPOS, Integer.toString(charpos));
            }

            del.setAttribute(LENGTH, 
                    Integer.toString(n.getTextContent().length()));
        }

        mEditScript.getDocumentElement().appendChild(del);
    }

    // Moves are a little more complicated
    // With context info we need old parent and DOM cn
    // CHANGED
    // now only need mark element
    public void move(Node n, String path, String parent,
            int childno, int ocharpos, int ncharpos) {
        
        if (ocharpos < 1) {
            throw new IllegalArgumentException(
                    "Old Character position must be >= 1");
        }
        
        if (ncharpos < 1) {
            throw new IllegalArgumentException(
                    "New Character position must be >= 1");
        }
        
        Element mov = mEditScript.createElement(MOVE);
        mov.setAttribute(NODE, path);
        
        mov.setAttribute(OLD_CHARPOS, ("" + ocharpos));
        mov.setAttribute(NEW_CHARPOS, ("" + ncharpos));

        if (n.getNodeType() == Node.TEXT_NODE) {
            mov.setAttribute(LENGTH, "" + n.getNodeValue().length());
        }

        mov.setAttribute(PARENT, parent);
        mov.setAttribute(CHILDNO, ("" + childno));

        mEditScript.getDocumentElement().appendChild(mov);
    }

}
