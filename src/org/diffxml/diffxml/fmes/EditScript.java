/*
Program to difference two XML files
 
Copyright (C) 2002-2004  Adrian Mouat
 
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

import org.diffxml.diffxml.DiffXML;
import org.diffxml.diffxml.DiffFactory;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.dom.NodeImpl;

public class EditScript
{
    private Document makeEmptyEditScript()
        {
        Document editScript = new DocumentImpl();

        Element root = editScript.createElement("delta");

        //Append any context information
        if (DiffFactory.CONTEXT)
            {
            root.setAttribute("sib_context", "" + DiffFactory.SIBLING_CONTEXT);
            root.setAttribute("par_context", "" + DiffFactory.PARENT_CONTEXT);
            root.setAttribute("par_sib_context",
                    "" + DiffFactory.PARENT_SIBLING_CONTEXT);
            }

        if (DiffFactory.REVERSE_PATCH)
            root.setAttribute("reverse_patch", "true");

        if (!DiffFactory.ENTITIES)
            root.setAttribute("resolve_entities", "false");

        if (!DiffFactory.DUL)
            {
            //Change root to xupdate style
            root = editScript.createElement("modifications");
            root.setAttribute("version", "1.0");
            root.setAttribute("xmlns:xupdate", "http://www.xmldb.org/xupdate");
            }

        editScript.appendChild(root);

        return editScript;
        }

    private void matchRoots(Document editScript, Document doc1, Document doc2)
        {

        //We need to match roots if not already matched
        //Algortihm says to create dummy node, but this will muck up xpaths.
        //Use update operation to match the two nodes.

        /*
           if (!doc1.getDocumentElement().getNodeName()
               .equals(doc2.getDocumentElement()))
           {
        //Add update operation
        //Need to add to Delta.java to get proper handling of args
        //But this is a quick hack to avoid probs
        //Need to add attributes
        Element upd=es.createElement("update");
        upd.setAttribute("node","/node()[1]");
        upd.setAttribute("name",doc2.getDocumentElement().getNodeName());
        root.appendChild(upd);

        //Set Matched
        ((Node3)doc1.getDocumentElement()).setUserData("matched","true",null);
        ((Node3)doc2.getDocumentElement()).setUserData("matched","true",null);
        }
        */
        }

    private void addChildrenToFifo(Node x, Fifo fifo)
        {
        NodeList kids = x.getChildNodes();

        if (kids != null)
            {
            for (int i = 0; i < kids.getLength(); i++)
                {
                if (Fmes.isBanned(kids.item(i)))
                    continue;
                    
                fifo.push(kids.item(i));
                }
            }
        }

    private void insertAsChild(int childNum, Node parent, Node insNode)
        {
        NodeList kids = parent.getChildNodes();
        
        if (kids.item(childNum) != null)
            parent.insertBefore(insNode, kids.item(childNum));
        else
            parent.appendChild(insNode);
        }

    private void addAttrsToDelta(NamedNodeMap attrs, String path, 
            Document editScript)
        {

        int numAttrs;
        if (attrs == null)
            numAttrs = 0;
        else
            numAttrs = attrs.getLength();

        for (int i = 0; i < numAttrs; i++)
            {
            Delta.Insert(attrs.item(i), path, 0, -1, editScript);
            }
        }

    private Node doInsert(NodeImpl x, NodeImpl z, Document doc1, 
            Document editScript, NodeSet matchings)
        {
        InsertPosition pos = findPos(x, matchings);
        Pos zPath = NodePos.get(z);

        //Apply insert to doc1
        //The node we want to insert is the import of x with all
        //its text node children
        //Need to make sure this imports attrs - they should be

        Node w = doc1.importNode(x, false);
        ((NodeImpl) w).setUserData("matched", "true", null);
        ((NodeImpl) w).setUserData("inorder", "true", null);

        //Take match of parent (z), and insert
        insertAsChild(pos.insertBefore, z, w);

        //Add to matching set
        x.setUserData("matched", "true", null);
        matchings.add(w, x);

        Delta.Insert(w, zPath.path, pos.numXPath, pos.charPosition, editScript);

        addAttrsToDelta(x.getAttributes(), NodePos.get(w).path, editScript);

        return w;
        }

    private Node doMove(NodeImpl x, NodeImpl z, 
            Document editScript, NodeSet matchings)
        {
        DiffXML.log.fine("In move");

        InsertPosition pos = new InsertPosition();

        Node w = matchings.getPartner(x);
        NodeImpl v = (NodeImpl) w.getParentNode();
        NodeImpl y = (NodeImpl) x.getParentNode();

        //UPDATE would be here if implemented
        
        //Apply move if parents not matched
        NodeImpl partnerY = (NodeImpl) matchings.getPartner(y);
        if (!v.isSameNode(partnerY))
            {
            pos = findPos(x, matchings);
            Pos wPath = NodePos.get(w);
            Pos zPath = NodePos.get(z);

            //Following two statements may be unnecessary
            ((NodeImpl) w).setUserData("inorder", "true", null);
            ((NodeImpl) x).setUserData("inorder", "true", null);

            Element context = editScript.createElement("mark");
            if (DiffFactory.CONTEXT)
                {
                context.appendChild(editScript.importNode(w, true));
                context = Delta.addContext(w, context);
                }

            //Apply move to T1
            insertAsChild(pos.insertBefore, z, w);

            Delta.Move(context, w, wPath.path, zPath.path, pos.numXPath,
                     wPath.charpos, pos.charPosition, wPath.length, editScript);
            }
        return w;
        }

    private void logNodes(Node x, Node y, Node z)
        {
        DiffXML.log.finer("x=" + x.getNodeName() + " " + x.getNodeValue());
        DiffXML.log.finer("y=" + y.getNodeName() + " " + y.getNodeValue());
        DiffXML.log.finer("z=" + z.getNodeName() + " " + z.getNodeValue());

        if (z == null)
            DiffXML.log.warning("Your matchings don't work you dumb"
                    + "mutha fucka \n or root");
        }


    public Document create(Document doc1, Document doc2,
            NodeSet matchings)
        {
        Document editScript = makeEmptyEditScript();

        if (!doc1.getDocumentElement().getNodeName()
                .equals(doc2.getDocumentElement()))
            matchRoots(editScript, doc1, doc2);

        //Fifo used to do a breadth first traversal of doc2
        Fifo fifo = new Fifo();
        fifo.push(doc2.getDocumentElement());

        while (!fifo.isEmpty())
            {
            DiffXML.log.fine("In breadth traversal");

            NodeImpl x = (NodeImpl) fifo.pop();
            addChildrenToFifo(x, fifo);

            //May need to do more with root
            if (x.isSameNode(doc2.getDocumentElement()))
                {
                x.setUserData("inorder", "true", null);
                continue;
                }

            Node y = x.getParentNode();
            Node z = matchings.getPartner(y);

            logNodes(x, y, z);
            Node w;

            if (x.getUserData("matched").equals("false"))
                w = doInsert(x, (NodeImpl) z, doc1, editScript, matchings);
            else
                w = doMove(x, (NodeImpl) z, editScript, matchings);

            //May want to check value of w
            alignChildren(w, x, editScript, matchings);
            }

        deletePhase(doc1.getDocumentElement(), editScript);

        //Post-Condition es is a minimum cost edit script,
        //Matchings is a total matching and
        //doc1 is isomorphic to doc2

        return editScript;
        }

    private void deletePhase(Node n, Document editScript)
        {
        //Deletes nodes in Post-order traversal
        NodeList kids = n.getChildNodes();
        if (kids != null)
            {
            //Note that we loop *backward* through kids
            for (int i = (kids.getLength() - 1); i >= 0; i--)
                {
                //Don't call delete phase for ignored ndoes
                if (Fmes.isBanned(kids.item(i)))
                    continue;

                deletePhase(kids.item(i), editScript);
                }
            }

        //If node isn't matched, delete it
        if (((NodeImpl) n).getUserData("matched").equals("false"))
            {
            Element par = (Element) n.getParentNode();
            Pos del_pos = NodePos.get(n);

            Delta.Delete(n, del_pos.path, del_pos.charpos, del_pos.length, editScript);
            par.removeChild(n);
            }

        }

    private void markChildrenOutOfOrder(Node n)
        {
        //Possibly should be ignoring banned nodes
        //Be very careful as ignored nodes will mess up moves
        NodeList kids = n.getChildNodes();

        for (int i = 0; i < kids.getLength(); i++)
            {
            if (Fmes.isBanned(kids.item(i)))
                continue;
            ((NodeImpl) kids.item(i)).setUserData("inorder", "false", null);
            DiffXML.log.fine("Node: " + kids.item(i).getNodeName()
                    + " user data: " 
                    + ((NodeImpl) kids.item(i)).getUserData("inorder"));
            }
        }

    private void setInOrderNodes(Node[] seq)
        {
        for (int i = 0; i < seq.length; i++)
            {
            if (seq[i] != null)
                {
                DiffXML.log.finer("seq" + seq[i].getNodeName()
                        + " " + seq[i].getNodeValue());
                ((NodeImpl) seq[i]).setUserData("inorder", "true", null);
                }
            }
        }

    private Node[] getSequence(NodeList set1, NodeList set2, 
            NodeSet matchings)
        {
        //Faster ways to do this
        //May be a problem with returned length
        
        Node[] resultSet = new Node[set1.getLength()];

        int index = 0;
        for (int i = 0; i < set1.getLength(); i++)
            {
            NodeImpl wanted = (NodeImpl) matchings.getPartner(set1.item(i));

            for (int j = 0; j < set2.getLength(); j++)
                {
                if (wanted.isSameNode(set2.item(j)))
                    {
                    resultSet[index++] = wanted;
                    break;
                    }
                }
            }
        return resultSet;
        }

    private void moveMisalignedNodes(NodeList xKids, Node w, 
            Document editScript, NodeSet matchings)
        {
        for (int i = 0; i < xKids.getLength(); i++)
            {
            if (((NodeImpl) xKids.item(i)).getUserData("inorder")
                    .equals("false")
                    && ((NodeImpl) xKids.item(i)).getUserData("matched")
                    .equals("true"))
                {
                //Get childno for move
                InsertPosition pos = new InsertPosition();
                pos = findPos(xKids.item(i), matchings);

                //Get partner
                Node a = matchings.getPartner(xKids.item(i));
                Pos aPos = NodePos.get(a);

                //Get a's explicit DOM position
                Node aParent = a.getParentNode();
                NodeList aSiblings = aParent.getChildNodes();

                int childNum = 0;
                for (childNum = 0; childNum < aSiblings.getLength(); childNum++)
                    {
                    if (((NodeImpl) aSiblings.item(childNum)).isSameNode(a))
                        break;
                    }

                Pos wPos = NodePos.get(w);
                //For programming ease we actually want to get any old
                //context now

                Element context = editScript.createElement("mark");
                if (DiffFactory.CONTEXT)
                    {
                    context.appendChild(editScript.importNode(a, true));
                    context = Delta.addContext(a, context);
                    }

                insertAsChild(pos.insertBefore, w, a);

                //Mark inorder
                ((NodeImpl) xKids.item(i)).setUserData(
                                                       "inorder", "true", null);
                ((NodeImpl) a).setUserData("inorder", "true", null);

                //Note that now a is now at new position
                Delta.Move(context, a, aPos.path, wPos.path, pos.numXPath,
                        aPos.charpos, pos.charPosition, aPos.length, editScript);
                }
            }
        }

    private void alignChildren(Node w, Node x, Document editScript,
            NodeSet matchings)
        {
        //How does alg deal with out of order nodes not matched with
        //children of partner?
        //Calls LCS algorithm

        //Order of w and x is important
        //Mark children of w and x "out of order"
        NodeList wKids = w.getChildNodes();
        NodeList xKids = x.getChildNodes();

        DiffXML.log.finer("no wKids" + wKids.getLength());
        DiffXML.log.finer("no xKids" + xKids.getLength());

        //build will break if following statement not here!
        //PROBABLY SHOULDN'T BE NEEDED - INDICATIVE OF BUG
        //Theory - single text node children are being matched,
        //Need to be moved, but aren't.
        if ((wKids.getLength() == 0) || (xKids.getLength()==0))
            return;

        markChildrenOutOfOrder(w);
        markChildrenOutOfOrder(x);

        Node[] wSeq = getSequence(wKids, xKids, matchings);
        Node[] xSeq = getSequence(xKids, wKids, matchings);

        Node[] seq = Lcs.find(wSeq, xSeq, matchings);
        setInOrderNodes(seq);

        //Go through children of w.
        //If not inorder but matched, move
        //Need to be careful if want xKids or wKids
        //Check
        
        moveMisalignedNodes(xKids, w, editScript, matchings);
        }

    //Findpos returns childnumber of node to insert BEFORE
    //Change to return XPath childnum as well
    //Change to return charpos as well
    private InsertPosition findPos(final Node x, final NodeSet matchings)
        {
        DiffXML.log.fine("Entered findPos");
        Node xParent = x.getParentNode();
        NodeList xSiblings = xParent.getChildNodes();

        InsertPosition pos = new InsertPosition();

        //Loop through childnodes
        //get rightmost left sibling of x marked "inorder"

        //Default v to x
        NodeImpl v = (NodeImpl) x;
        for (int i = 0; i < xSiblings.getLength(); i++)
            {
            NodeImpl test = (NodeImpl) xSiblings.item(i);

            if (test.isSameNode(x))
                break;

            if (test.getUserData("inorder").equals("true"))
                v = test;
            }

        if (v.isSameNode(x))
            {
            DiffXML.log.fine("Exiting findPos normally1");
            //SHOULD CHAR be 1 or -1? depends on next node. Safest at 1?
            pos.insertBefore = 0;
            pos.numXPath = 1;
            pos.charPosition = 1;
            return pos;
            }

        //Get partner of v
        NodeImpl u = (NodeImpl) matchings.getPartner((Node) v);
        Node par_u = u.getParentNode();

        //Find "in order" index of u
        int dom_index = 0;
        int xpath_index = 1;
        int last_node = -1;
        NodeList children = par_u.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
            {
            NodeImpl test = (NodeImpl) children.item(i);
            if (u.isSameNode(children.item(i)))
                break;

            if (test.getUserData("inorder").equals("true"))
                {
                dom_index++;
                //Want to increment XPath index if not
                //both this (inorder) node and last (inorder) node text nodes
                if ((test.getNodeType() == Node.TEXT_NODE) && (last_node != -1)
                        && (children.item(last_node).getNodeType()
                            == Node.TEXT_NODE))
                    {
                    xpath_index--;
                    }

                xpath_index++;
                last_node = i;

                //Want to increment XPath index if not both this
                //(inorder) node and last node text nodes
                }
            }
        //Need i+1 child
        pos.insertBefore = ++dom_index;

        //If this is a text node, and last node was a text node,
        //don't increment xpath

        //Get charpos
        //Can't use NodePos func as node may not exist
        int tmp_index = dom_index;
        int last_ind = children.getLength() - 1;

        if (dom_index > last_ind)
            tmp_index = last_ind;

        int charpos = 1;
        NodeImpl test = (NodeImpl) children.item(dom_index - 1);
        if (children.item(dom_index - 1).getNodeType() != Node.TEXT_NODE
                && test.getUserData("inorder").equals("true"))
            charpos = -1;
        else
            {
            for (int j = (dom_index - 1); j >= 0; j--)
                {
                test = (NodeImpl) children.item(j);
                if (children.item(j).getNodeType() == Node.TEXT_NODE
                        && test.getUserData("inorder").equals("true"))
                    {
                    charpos = charpos
                            + children.item(j).getNodeValue().length();
                    DiffXML.log.finer(children.item(j).getNodeValue()
                            + " charpos " + charpos);
                    }
                else
                    break;
                }
            }

        //If this is a text node, and last node was a text node,
        //don't increment xpath
        if (!(x.getNodeType() == Node.TEXT_NODE
                    && (children.item(dom_index - 1).getNodeType()
                        == Node.TEXT_NODE)))
            xpath_index++;

        pos.numXPath = xpath_index;
        pos.charPosition = charpos;

        DiffXML.log.fine("Exiting findPos normally");
        return pos;
        }
}
