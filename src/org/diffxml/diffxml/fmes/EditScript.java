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

    private Node doInsert(NodeImpl x, NodeImpl z, Document doc1, 
            Document editScript, NodeSet matchings)
        {
        InsertPosition pos = new InsertPosition();
        Pos ins_pos;
        //Easier to mark as matched now than at end
        x.setUserData("matched", "true", null);
        x.setUserData("inorder", "true", null);

        pos = FindPos(x, matchings);

        //Get XPath for z
        ins_pos = NodePos.get(z);

        //Create XPath for node we are about to insert
        //(Needed to insert attrs if any)
        NamedNodeMap attrs = x.getAttributes();
        int a_len = (attrs != null) ? attrs.getLength() : 0;
        String path;

        //XPath different if expanding tagnames
        if (DiffFactory.TAGNAMES && (a_len != 0))
            {
            //Find in order index of element
            NodeList k = x.getParentNode().getChildNodes();
            String tag = x.getNodeName();
            int in = 0;
            for (int ii = 0; ii < k.getLength(); ii++)
                {
                NodeImpl tmp = (NodeImpl) k.item(ii);
                if (tmp.getNodeName().equals(tag)
                        && tmp.getUserData("inorder").equals("true"))
                    in++;

                if (tmp.isSameNode(x))
                    break;
                }
            path = ins_pos.path + "/" + x.getNodeName()
            + "[" + in + "]";
            }
        else
            path = ins_pos.path + "/node()[" + pos.numXPath + "]";

        //Apply insert to doc1
        //The node we want to insert is the import of x with all
        //its text node children
        //Need to make sure this imports attrs - they should be

        Node w = doc1.importNode(x, false);
        ((NodeImpl) w).setUserData("matched", "true", null);
        //Not sure if inorder should be true or false
        ((NodeImpl) w).setUserData("inorder", "true", null);

        //Take match of parent (z), and insert
        //get the kids
        NodeList babes = z.getChildNodes();
        //If Node exists we want to insert before it
        //if (babes.item(index[DOM]) != null)
        if (babes.item(pos.insertBefore) != null)
            //z.insertBefore(w, babes.item(index[DOM]));
            z.insertBefore(w, babes.item(pos.insertBefore));
        else
            {
            //Last node
            z.appendChild(w);
            }

        //Add to matching set
        matchings.add(w, x);

        //Add to delta
        //Delta.Insert(w, ins_pos.path, index[XPATH], index[CHAR], editScript);
        Delta.Insert(w, ins_pos.path, pos.numXPath, pos.charPosition, editScript);

        //Add attributes to delta
        for (int i = 0; i < a_len; i++)
            {
            Delta.Insert(attrs.item(i), path, 0, -1, editScript);
            }

        return w;
        }

    private Node doMove(NodeImpl x, NodeImpl y, NodeImpl z, 
            Document editScript, NodeSet matchings)
        {
        DiffXML.log.fine("In move");

        //int index[] = new int[3];
        InsertPosition pos = new InsertPosition();

        Node w = matchings.getPartner(x);
        Node v = w.getParentNode();
        NodeImpl tmp1 = (NodeImpl) v;
        //Apply move if parents not matched
        NodeImpl tmp2 = (NodeImpl) matchings.getPartner(y);
        if (!tmp1.isSameNode(tmp2))
            {
            //index = FindPos(x, matchings);
            pos = FindPos(x, matchings);
            //Element mov=es.createElement("move");
            //db.p("In move line 236");
            Pos w_pos = NodePos.get(w);
            //mov.setAttribute("node",w_pos.path);
            //  if (w_pos.charpos!=-1)
            //  mov.setAttribute("old_charpos", (""+w_pos.charpos) );

            // if (w_pos.length!=-1)
            // mov.setAttribute("length", (""+w_pos.length) );

            Pos z_pos = NodePos.get(z);
            //mov.setAttribute("parent",z_pos.path);
            // if ( index[CHAR]!=-1 )
            //mov.setAttribute("new_charpos",(""+index[CHAR]));

            //mov.setAttribute("childno", ( ""+index[XPATH] ) );

            //db.p("MOVE" + w.getNodeName() + " " + w.getNodeValue()
            //    + " , " + z.getNodeName() + " , " + index[XPATH]);
            //root.appendChild(mov);

            //Get domcn w is of v
            NodeList k = v.getChildNodes();
            int domcn = 0;
            for (domcn = 0; domcn < k.getLength(); domcn++)
                {
                if (((NodeImpl) w).isSameNode(k.item(domcn)))
                    break;
                }


            Element mark = editScript.createElement("mark");
            if (DiffFactory.CONTEXT)
                {
                mark.appendChild(editScript.importNode(w, true));
                mark = Delta.addContext(w, mark);
                }
            //Apply move to T1

            //get the kids
            NodeList babes2 = z.getChildNodes();
            //If Node exists we want to insert before it
            //if (babes2.item(index[DOM]) != null)
            //    z.insertBefore(w, babes2.item(index[DOM]));
            if (babes2.item(pos.insertBefore) != null)
                z.insertBefore(w, babes2.item(pos.insertBefore));
            else
                {
                //Last node
                z.appendChild(w);
                }

            //Delta.Move(mark, w, w_pos.path, z_pos.path, index[XPATH],
            //        w_pos.charpos, index[CHAR], w_pos.length, editScript);
            Delta.Move(mark, w, w_pos.path, z_pos.path, pos.numXPath,
                     w_pos.charpos, pos.charPosition, w_pos.length, editScript);
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
                //Mark "in order"
                x.setUserData("inorder", "true", null);
                continue;
                }

            //Set y to be parent of x
            Node y = x.getParentNode();

            //Get z, y's partner
            Node z = matchings.getPartner(y);

            logNodes(x, y, z);

            Node w;

            if (x.getUserData("matched").equals("false"))
                {
                w = doInsert(x, (NodeImpl) z, doc1, editScript, matchings);
                }
            else
                {
                w = doMove(x, (NodeImpl) y, (NodeImpl) z, editScript, matchings);
                }

            //Call AlignChildren
            //May want to check value of w
            AlignChildren(w, x, editScript, matchings);

            }

        DeletePhase(doc1.getDocumentElement(), editScript);

        //Post-Condition es is a minimum cost edit script,
        //Matchings is a total matching and
        //doc1 is isomorphic to doc2

        return editScript;
        }

    private void DeletePhase(Node n, Document editScript)
        {
        //System.out.println("Entered Delete Phase");
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

                DeletePhase(kids.item(i), editScript);
                }
            }

        //If node isn't matched, delete it
        if (((NodeImpl) n).getUserData("matched").equals("false"))
            {
            /*
               ============
               Apply Delete
               ============
               */

            Element par = (Element) n.getParentNode();
            Pos del_pos = NodePos.get(n);

            //Add to es
            Delta.Delete(n, del_pos.path, del_pos.charpos, del_pos.length, editScript);
            //Apply remove
            par.removeChild(n);
            }

        //System.out.println("Exited Delete Phase");
        }

    private void AlignChildren(Node w, Node x, Document editScript,
            NodeSet matchings)
        {
        //Calls LCS algorithm

        //Order of w and x is important
        //Mark all children of w and x "out of order"
        //Immediate children only? Assume so.

        NodeList w_kids = w.getChildNodes();
        NodeList x_kids = x.getChildNodes();

        //Only have something to do if we have kids
        DiffXML.log.finer("no w_kids" + w_kids.getLength());
        DiffXML.log.finer("no x_kids" + x_kids.getLength());
        if (w_kids.getLength() == 0)
            return;

        for (int i = 0; i < w_kids.getLength(); i++)
            {
            //Ignored nodes should be left inorder?
            //Ignored nodes are probably going to foul up moves etc.
            //May need to changed to nodes are only ignored when
            //outputting delta
            if (Fmes.isBanned(w_kids.item(i)))
                continue;
            /*
               if (Table.ign_ws_nodes
                   && w_kids.item(i).getNodeType()==Node.TEXT_NODE)
               {
               StringTokenizer st =
                   new StringTokenizer(w_kids.item(i).getNodeValue());
               if (!st.hasMoreTokens())
               continue;
               }
               */
            ((NodeImpl) w_kids.item(i)).setUserData("inorder", "false", null);
            }

        for (int i = 0; i < x_kids.getLength(); i++)
            {
            //Ignored nodes should be left inorder
            if (Fmes.isBanned(x_kids.item(i)))
                continue;
            /*
               if (Table.ign_ws_nodes
                   && x_kids.item(i).getNodeType()==Node.TEXT_NODE)
               {
               StringTokenizer st =
                   new StringTokenizer(x_kids.item(i).getNodeValue());
               if (!st.hasMoreTokens())
               continue;
               }
               */
            ((NodeImpl) x_kids.item(i)).setUserData("inorder", "false", null);
            }

        Node[] seq = Lcs.find(w_kids, x_kids, matchings);
        //Step through array, marking in order

        for (int i = 0; i < seq.length; i++)
            {
            if (seq[i] != null)
                {
                DiffXML.log.finer("seq" + seq[i].getNodeName()
                        + " " + seq[i].getNodeValue());
                ((NodeImpl) seq[i]).setUserData("inorder", "true", null);
                }
            }

        //Go through children of w.
        //If not inorder but matched, move
        //Need to be careful if want x_kids or w_kids
        //Check
        for (int i = 0; i < x_kids.getLength(); i++)
            {
            DiffXML.log.finer("x item i" + x_kids.item(i).getNodeName());
            DiffXML.log.finer("inorder"
                    + ((NodeImpl) x_kids.item(i)).getUserData("inorder"));
            DiffXML.log.finer("matched"
                    + ((NodeImpl) x_kids.item(i)).getUserData("matched"));
            if (((NodeImpl) x_kids.item(i)).getUserData("inorder")
                        .equals("false")
                    && ((NodeImpl) x_kids.item(i)).getUserData("matched")
                        .equals("true"))
                {
                /*
                   ==========
                   Apply Move
                   ==========
                   */

                //Get childno for move
                InsertPosition pos = new InsertPosition();
                //int index[] = FindPos(x_kids.item(i), matchings);
                pos = FindPos(x_kids.item(i), matchings);
                //Get partner
                Node a = matchings.getPartner(x_kids.item(i));
                //Element mov=es.createElement("move");
                Pos a_pos = NodePos.get(a);

                //Get a's explicit DOM position
                Node a_par = a.getParentNode();
                NodeList k = a_par.getChildNodes();

                int domcn = 0;
                for (domcn = 0; domcn < k.getLength(); domcn++)
                    {
                    if (((NodeImpl) k.item(domcn)).isSameNode(a))
                        break;
                    }

                /*mov.setAttribute("node",a_pos.path);
                  if (a_pos.charpos!=-1)
                  mov.setAttribute("old_charpos", (""+a_pos.charpos) );

                  if (a_pos.length!=-1)
                  mov.setAttribute("length", (""+a_pos.length) );
                  */
                Pos w_pos = NodePos.get(w);
                //mov.setAttribute("parent",w_pos.path);
                //if ( index[CHAR]!=-1 )
                //    mov.setAttribute("new_charpos",(""+index[CHAR]));

                //mov.setAttribute("childno", ( ""+index[XPATH] ) );

                //es.getDocumentElement().appendChild(mov);

                //For programming ease we actually want to get any old
                //context now

                Element mark = editScript.createElement("mark");
                if (DiffFactory.CONTEXT)
                    {
                    mark.appendChild(editScript.importNode(a, true));
                    mark = Delta.addContext(a, mark);
                    }

                //get the kids
                NodeList babes = w.getChildNodes();
                //If Node exists we want to insert before it
                //if (babes.item(index[DOM]) != null)
                //    w.insertBefore(a, babes.item(index[DOM]));
                if (babes.item(pos.insertBefore) != null)
                    w.insertBefore(a, babes.item(pos.insertBefore));
                else
                    {
                    //Last node
                    w.appendChild(a);
                    }


                //Mark inorder
                ((NodeImpl) x_kids.item(i)).setUserData(
                        "inorder", "true", null);
                ((NodeImpl) a).setUserData("inorder", "true", null);

                //Note that now a is now at new position
                Delta.Move(mark, a, a_pos.path, w_pos.path, pos.numXPath,
                        a_pos.charpos, pos.charPosition, a_pos.length, editScript);
                }
            }
        }

    //Findpos returns childnumber of node to insert BEFORE
    //Change to return XPath childnum as well
    //Change to return charpos as well
    private InsertPosition FindPos(final Node x, final NodeSet matchings)
        {
        DiffXML.log.fine("Entered FindPos");
        Node x_par = x.getParentNode();
        NodeList kids = x_par.getChildNodes();

        //int index[] = new int[3];
        InsertPosition pos = new InsertPosition();
        //Loop through childnodes
        //get rightmost left sibling of x marked "inorder"

        //Default v to x
        NodeImpl v = (NodeImpl) x;
        for (int i = 0; i < kids.getLength(); i++)
            {
            NodeImpl test = (NodeImpl) kids.item(i);

            if (test.isSameNode(x))
                break;

            if (test.getUserData("inorder").equals("true"))
                v = test;
            }

        if (v.isSameNode(x))
            {
            DiffXML.log.fine("Exiting FindPos normally1");
            //Return DOM childno of 0, XPath childno of 1
            //SHOULD CHAR be 1 or -1? depends on next node. Safest at 1?
            //index[DOM] = 0;
            pos.insertBefore = 0;
            //index[XPATH] = 1;
            pos.numXPath = 1;
            //index[CHAR] = 1;
            pos.charPosition = 1;
            //return index;
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


            /*db.on=true;
              db.p(test.getNodeName() + test.getUserData("matched"));
              if (test.getUserData("inorder")==null)
              db.p("WTF?");
              db.on=false;
              */
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
        //childno[0]=++dom_index;
        //index[DOM] = ++dom_index;
        pos.insertBefore = ++dom_index;
        //childno[1]=++xpath_index;
        //childno[XPATH]=++xpath_index;

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

        //index[XPATH] = xpath_index;
        pos.numXPath = xpath_index;
        //index[CHAR] = charpos;
        pos.charPosition = charpos;

        DiffXML.log.fine("Exiting FindPos normally");
        //return index;
        return pos;
        }

}
