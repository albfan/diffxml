package org.diffxml.patchxml;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.diffxml.diffxml.DiffXML;
import org.diffxml.diffxml.fmes.NodeOps;
import org.diffxml.diffxml.fmes.PrintXML;
import org.diffxml.dul.DULConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import com.sun.org.apache.xpath.internal.XPathAPI;

public class DULPatch {
    /**
     * Perform update operation.
     *
     * TODO: Write!
     */
    private void doUpdate() {
        
    }

    /**
     * Get the parent node pointed to by the parent attributed.
     *
     * @param doc   document being patched
     * @param attrs attributes of operation node
     * @return the parent node
     */
    private Node getParentFromAttr(final Document doc, 
            final NamedNodeMap attrs) {
        
        Node parent = null;
        try {
            parent = XPathAPI.selectSingleNode(
                    doc.getDocumentElement(),
                    attrs.getNamedItem(DULConstants.PARENT).getNodeValue());
        } catch (TransformerException e) {
            System.err.println("Could not resolve XPath for parent for insert");
        }
        
        return parent;
    }

    /**
     * Get value of nodetype attribute.
     *
     * @param attrs attributes of operation node
     * @return the value of nodetype
     */
    private int getNodeTypeFromAttr(final NamedNodeMap attrs) {
        //TODO: Deal with cases when no node type attr
        return Integer.valueOf(
                attrs.getNamedItem(DULConstants.NODETYPE).getNodeValue());
    }

    /**
     * Get the DOM Child Number equivalent of the XPath childnumber.
     *
     * @param siblings the NodeList we are interested in
     * @param xpathcn  the XPath child number
     * @return the equivalent DOM child number
     */
    private int getDOMChildNoFromXPath(final NodeList siblings,
            final int xpathcn) {
        //Doesn't cope with node names instead of numbers
        /*
        int xPathIndex = 0;
        int j;
        for (j = 0; j < siblings.getLength(); j++)
            {
            index++;
            if (j > 0 && siblings.item(j).getNodeType() == Node.TEXT_NODE
                    && siblings.item(j - 1).getNodeType() == Node.TEXT_NODE)
                xPathIndex--;
            if (xPathIndex == xpathcn)
                break;
            }
        return j;
        */
        int domIndex = 0;
        int xPathIndex = 1;
        while ((xPathIndex < xpathcn) && (domIndex < siblings.getLength())) {
            if (!((prevNodeIsATextNode(siblings, domIndex))
                    && (siblings.item(domIndex).getNodeType() 
                            == Node.TEXT_NODE))) {
                xPathIndex++;
            }
            domIndex++;
        }
        //Handle appending nodes
        if (xPathIndex < xpathcn) {
            domIndex++;
        }
        //TODO: Assert or throw exception if xPathIndex still less than xpathcn
        return domIndex;
    }

    /**
     * Get the value associated with the operation node.
     *
     * @param op the operation node
     * @return the string value of the node
     */
    private String getOpValue(final Node op) {
        
        NodeList opKids = op.getChildNodes();

        String value = "";
        if (opKids.getLength() > 1) {
            System.err.println("Unexpected children in insert operation");
        } else if ((opKids.getLength() == 1)
                && (opKids.item(0).getNodeType() == Node.TEXT_NODE)) {
            value = opKids.item(0).getNodeValue();
        }

        return value;
    }

    /**
     * Get value of old_charpos attribute.
     *
     * Defaults to 1.
     *
     * @param opAttrs attributes of operation node
     * @return the value of new_charpos
     */
    private int getOldCharPos(final NamedNodeMap opAttrs) {
        
        int oldCharPos = 1;

        Node a = opAttrs.getNamedItem(DULConstants.OLD_CHARPOS);
        if (a != null) {
            oldCharPos = Integer.valueOf(a.getNodeValue());
        }

        return oldCharPos;
    }

    /**
     * Get value of new_charpos attribute.
     *
     * Defaults to 1.
     *
     * @param opAttrs attributes of operation node
     * @return the value of new_charpos
     */
    private int getNewCharPos(final NamedNodeMap opAttrs) {
        
        int newCharPos = 1;

        Node a = opAttrs.getNamedItem(DULConstants.NEW_CHARPOS);
        
        if (a != null) {
            newCharPos = Integer.valueOf(a.getNodeValue());
        }

        return newCharPos;
    }

    /**
     * Get value of charpos attribute.
     *
     * Defaults to 1.
     *
     * @param opAttrs attributes of operation node
     * @return the value of charpos
     */
    private int getCharPos(final NamedNodeMap opAttrs) {
        
        int charpos = 1;

        Node a = opAttrs.getNamedItem(DULConstants.CHARPOS);
        if (a != null) {
            charpos = Integer.valueOf(a.getNodeValue());
        }

        return charpos;
    }

    /**
     * Tests if previous node is a text node.
     *
     * @param siblings siblings of current node
     * @param index    index of current node
     * @return true if previous node is a text node, false otherwise
     */
    private boolean prevNodeIsATextNode(final NodeList siblings,
            final int index) {
        
        return (index > 0
                && siblings.item(index - 1).getNodeType() == Node.TEXT_NODE);
    }

    /**
     * Inserts a node at the given character position.
     *
     * @param charpos  the character position to insert at
     * @param siblings the NodeList to insert the node into
     * @param domcn    the child number to insert the node as
     * @param ins      the node to insert
     * @param parent   the node to become the parent of the inserted node
     */
    private void insertAtCharPos(final int charpos, final NodeList siblings,
            final int domcn, final Node ins, final Node parent) {
        //TODO: Allow inserting into middle of text node
        //TODO: Handle errors better
        //TODO: What about appending text nodes

        //we know text node at domcn -1
        int cp = charpos;
        int textNodeIndex = domcn - 1;
        boolean append = false;

        while (prevNodeIsATextNode(siblings, textNodeIndex)) {
            textNodeIndex--;
        }

        while (siblings.item(textNodeIndex).getNodeType() == Node.TEXT_NODE
                && cp > siblings.item(textNodeIndex).getNodeValue().length()) {
            cp = cp - siblings.item(textNodeIndex).getNodeValue().length();
            textNodeIndex++;

            if (textNodeIndex == siblings.getLength()) {
                append = true;
                parent.appendChild(ins);
                break;
            }
        }
        if (!append) {
            parent.insertBefore(ins, siblings.item(textNodeIndex));
        }

    }

    /**
     * Insert a node under parent node at given position.
     *
     * @param siblings the NodeList to insert the node into
     * @param parent   the parent to insert the node under
     * @param domcn    the child number to insert the node as
     * @param charpos  the character position at which to insert the node
     * @param ins      the node to be inserted
     */

    private void insertNode(final NodeList siblings, final Element parent,
            final int domcn, final int charpos, final Node ins) {
        
        //Note siblings(domcn) is node currently at the position we want
        //to put the node, not the node itself.

        if ((siblings.getLength() > 0) && (domcn <= siblings.getLength())) {
            //Check if inserting into text
            //TODO: Change to use prevNodeIsATextNode method
            boolean prevNodeTextNode = (domcn >= 1 && 
                    siblings.item(domcn - 1).getNodeType() == Node.TEXT_NODE);
            boolean firstNodeTextNode = 
                (siblings.item(0).getNodeType() == Node.TEXT_NODE);
            
            if (prevNodeTextNode) {
                insertAtCharPos(charpos, siblings, domcn, ins, parent);
            } else if (domcn == 0 && firstNodeTextNode) {
                
                //TODO: This is a poor hack as insertAtCharPos doesn't work
                if (charpos > 1) {
                    DiffXML.LOG.fine("here " + ins.getNodeValue());
                    parent.insertBefore(ins, siblings.item(1));
                } else {
                    parent.insertBefore(ins, siblings.item(0));
                }
            } else if (domcn == siblings.getLength()) {
                parent.appendChild(ins);
            } else {
                DiffXML.LOG.fine("Applying insertBefore");
                parent.insertBefore(ins, siblings.item(domcn));
            }
        } else {
            parent.appendChild(ins);
        }
    }

    /**
     * Get the DOM Child number of a node using "childno" attribute.
     *
     * @param opAttrs  the attributes of the operation
     * @param nodeType the nodeType to be inserted
     * @param siblings the siblings of the node
     * @return the DOM Child number of the node
     */
    private int getDOMChildNo(final NamedNodeMap opAttrs,
            final int nodeType, final NodeList siblings) {
        
        //Note init to zero
        int xpathcn = 0;
        int domcn = 0;

        if (opAttrs.getNamedItem(DULConstants.CHILDNO) != null) {
            xpathcn = Integer.valueOf(opAttrs.getNamedItem(
                    DULConstants.CHILDNO).getNodeValue());
        }

        //Convert xpath childno to DOM childno
        if (nodeType != Node.ATTRIBUTE_NODE) {
            domcn = getDOMChildNoFromXPath(siblings, xpathcn);
        }

        return domcn;
    }

    /**
     * Apply insert operation to document.
     *
     * @param doc the document to be patched
     * @param op  the insert operation node
     */
    private void doInsert(final Document doc, final Node op) {
        
        DiffXML.LOG.fine("Applying insert");
        Node ins;

        //Get various variables need for insert
        NamedNodeMap opAttrs = op.getAttributes();
        Node parent = getNamedParent(doc, opAttrs);
        int charpos = getCharPos(opAttrs);

        //TODO: handle null better
        if (parent == null) {
            return;
        }

        NodeList siblings = parent.getChildNodes();
        int nodeType = getNodeTypeFromAttr(opAttrs);

        int domcn = getDOMChildNo(opAttrs, nodeType, siblings);

        switch (nodeType) {
            case Node.TEXT_NODE:

                ins = doc.createTextNode(getOpValue(op));
                //System.err.println("domcn: " + domcn + " parent:" +parent.getNodeName());
                insertNode(siblings, (Element) parent, domcn, charpos, ins);
                break;

            case Node.ELEMENT_NODE:

                ins = doc.createElement(
                        opAttrs.getNamedItem(DULConstants.NAME).getNodeValue());
                insertNode(siblings, (Element) parent, domcn, charpos, ins);
                break;

            case Node.COMMENT_NODE:

                ins = doc.createComment(getOpValue(op));
                insertNode(siblings, (Element) parent, domcn, charpos, ins);
                break;

            case Node.ATTRIBUTE_NODE:

                String name = opAttrs.getNamedItem(DULConstants.NAME).getNodeValue();
                ((Element) parent).setAttribute(name, getOpValue(op));
                break;

            default:
                //TODO: consider throwing exception or exiting here
                System.err.println("Unknown NodeType " + nodeType);
                return;
        }
    }

    /**
     * Find the correct text node to delete.
     *
     * @param delNode the first text node pointed to
     * @param charpos the character position at which to delete
     * @return the text node which should be deleted
     */
    private Node getDelTextNode(final Node delNode, final int charpos) {
        
        int cp = charpos;
        //TODO: Allow deleting part of nodes, not just whole nodes
        NodeList siblings = delNode.getParentNode().getChildNodes();
        int i = 0;
        while (!NodeOps.checkIfSameNode(delNode, siblings.item(i))) {
            i++;
        }

        //TODO: Check conditional, consider charpos > 0
        while (cp > siblings.item(i).getNodeValue().length()) {
            cp = cp - siblings.item(i).getNodeValue().length();
            i++;
        }
        return siblings.item(i);
    }

    /**
     * Log various attributes of delete operation.
     *
     * @param delNode the node to be deleted
     * @param opAttrs the attributes of the operation node
     */
    private void logDeleteVariables(final Node delNode,
            final NamedNodeMap opAttrs) {
        
        DiffXML.LOG.finer("Deleting node "
                + delNode.getNodeName() + " " + delNode.getNodeValue());
        if (opAttrs.getNamedItem(DULConstants.LENGTH) != null) {
            DiffXML.LOG.finer("Supposed length "
                    + opAttrs.getNamedItem(DULConstants.LENGTH).getNodeValue());
            DiffXML.LOG.finer("Actual length "
                    + delNode.getNodeValue().length());
        }
    }

    /**
     * Gets the node pointed to by the "parent" attribute.
     *
     * @param doc     document being patched
     * @param opAttrs attributes of operation node
     * @return        node pointed to by "parent" attribute
     */

    private Node getNamedParent(final Document doc, 
            final NamedNodeMap opAttrs) {
        
        String xPath = opAttrs.getNamedItem(DULConstants.PARENT).getNodeValue();
        return getNodeFromXPath(doc, xPath);
    }

    /**
     * Returns the node pointed to by a given xPath.
     *
     * @param doc   document being patched
     * @param xPath xPath to the node
     * @return      the node pointed to by the xPath
     */

    private Node getNodeFromXPath(final Document doc, final String xPath) {
        Node n = null;
        try {
            //According to API returns *first* match,
            //so should be first text node if text node matched
            n = XPathAPI.selectSingleNode(doc.getDocumentElement(), xPath);
        } catch (TransformerException e) {
            //Consider more fault tolerant behaviour
            System.err.println("Could not resolve XPath for node");
            System.exit(1);
        }
        return n;
    }

    /**
     * Gets the node pointed to by the "node" attribute.
     *
     * @param doc     document being patched
     * @param opAttrs attributes of operation node
     * @return        node pointed to by "node" attribute
     */
    private Node getNamedNode(final Document doc, final NamedNodeMap opAttrs) {
        String xPath = opAttrs.getNamedItem(DULConstants.NODE).getNodeValue();
        return getNodeFromXPath(doc, xPath);
    }

    /**
     * Apply delete operation.
     *
     * @param doc document to be patched
     * @param op  node holding details of delete
     */

    private void doDelete(final Document doc, final Node op) {

        //TODO: test behaviour with text nodes
        NamedNodeMap opAttrs = op.getAttributes();
        DiffXML.LOG.fine("Applying delete");

        Node delNode = getNamedNode(doc, opAttrs);

        int charpos = getCharPos(opAttrs);

        //Text node may actually be different node
        if (delNode.getNodeType() == Node.TEXT_NODE) {
            delNode = getDelTextNode(delNode, charpos);
        }

        logDeleteVariables(delNode, opAttrs);

        delNode.getParentNode().removeChild(delNode);
    }

    /**
     * Log various attributes of move.
     *
     * @param opAttrs attributes of move
     */
    private void logMoveVars(final NamedNodeMap opAttrs) {
        
        DiffXML.LOG.finer("Node: "
                + opAttrs.getNamedItem(DULConstants.NODE).getNodeValue()
                + " Parent" + opAttrs.getNamedItem(
                        DULConstants.PARENT).getNodeValue());
        DiffXML.LOG.finer(DULConstants.CHILDNO
                + opAttrs.getNamedItem(DULConstants.CHILDNO).getNodeValue());

        if (opAttrs.getNamedItem(DULConstants.LENGTH) != null) {
            DiffXML.LOG.finer("length "
                    + opAttrs.getNamedItem(DULConstants.LENGTH).getNodeValue());
        }
    }

    /**
     * Apply move operation.
     *
     * @param doc document to be patched
     * @param op  node holding details of move
     */
    private void doMove(final Document doc, final Node op) {
        
        //TODO: Thorough testing - pretty sure not currently working properly

        NamedNodeMap opAttrs = op.getAttributes();
        logMoveVars(opAttrs);

        Node moveNode = getNamedNode(doc, opAttrs);
        if (moveNode == null) {
            System.err.println("Error applying patch.\n"
                    + "Node to move doesn't exist.");
            System.exit(1);
        }
        DiffXML.LOG.fine("moveNode: " + moveNode.getNodeName());

        int oldCharPos = getOldCharPos(opAttrs);

        //Currently only consider deleting nodes, not part of nodes
        if (moveNode.getNodeType() == Node.TEXT_NODE) {
            moveNode = getDelTextNode(moveNode, oldCharPos);
        }

        //Find position to move to
        //Get parent
        Node parent = getNamedParent(doc, opAttrs);
        DiffXML.LOG.fine("parent: " + parent.getNodeName());

        NodeList newSiblings = parent.getChildNodes();
        int domcn = getDOMChildNo(opAttrs, moveNode.getNodeType(), newSiblings);

        //Get new charpos
        int newCharPos = getNewCharPos(opAttrs);

        //Perform insert
        //TODO: Check old node removed and children properly dealt with

        moveNode = moveNode.getParentNode().removeChild(moveNode);
        DiffXML.LOG.fine("newCharPos: " + newCharPos + " domcn: " + domcn);
        insertNode(newSiblings, (Element) parent, domcn, newCharPos, moveNode);
    }

    /**
     * Apply DUL patch to XML document.
     *
     * @param doc   the XML document to be patched
     * @param patch the DUL patch
     */
    public final void apply(final Document doc, final Document patch) {
        NodeIterator ni = ((DocumentTraversal) patch).createNodeIterator(
                patch.getDocumentElement(), NodeFilter.SHOW_ELEMENT,
                null, false);

        Node op = ni.nextNode();
        //Check we have a delta
        if (!op.getNodeName().equals(DULConstants.DELTA)) {
            System.err.println("Not a delta document!");
            return;
        }

        //Cycle through elements applying ops
        op = ni.nextNode();

        while (op != null) {
            String opName = op.getNodeName();
            //NamedNodeMap opAttrs = op.getAttributes();

            if (opName.equals(DULConstants.UPDATE)) {
                doUpdate();
            } else if (opName.equals(DULConstants.INSERT)) {
                doInsert(doc, op);
            } else if (opName.equals(DULConstants.DELETE)) {
                doDelete(doc, op);
            } else if (opName.equals(DULConstants.MOVE)) {
                doMove(doc, op);
            } else {
                System.err.println("Do not recognise element: " + opName);
                System.exit(2);
            }
            if (PatchXML.OUTPUT_DEBUG) {
                try {
                    System.out.println("Applying: ");
                    PrintXML.print(op);
                    System.out.println();
                    Transformer serializer = TransformerFactory.newInstance()
                            .newTransformer();
                    serializer.transform(new DOMSource(doc), new StreamResult(
                            System.out));
                    System.out.println();
                } catch (javax.xml.transform.TransformerException e) {
                    System.err.println("Failed to do debug output");
                    System.exit(1);
                }
            }
            op = ni.nextNode();
        }
    }

}
