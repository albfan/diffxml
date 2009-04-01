package org.diffxml.dul;

/**
 * Constants used in DUL Deltas.
 * 
 * @author Adrian Mouat
 *
 */
public final class DULConstants {
   
    /**
     * Private constructor - shouldn't be instantiated.
     */
    private DULConstants() {
        
    }
    
    /** If the delta was created as a "reverse patch". **/
    public static final String REVERSE_PATCH = "reverse_patch";
    
    /** The amount of parent sibling context. **/
    public static final String PARENT_SIBLING_CONTEXT = "par_sib_context";
    
    /** The amount of parent context. **/
    public static final String PARENT_CONTEXT = "par_context";
    
    /** The amount of sibling context. **/
    public static final String SIBLING_CONTEXT = "sib_context";
    
    /** Document element of a DUL EditScript. **/
    public static final String DELTA = "delta";
    
    /** Character position of the "new" node. **/
    public static final String NEW_CHARPOS = "new_charpos";
    
    /** Character position of the "old" node. **/
    public static final String OLD_CHARPOS = "old_charpos";
    
    /** Move operation element. **/
    public static final String MOVE = "move";
    
    /** Number of characters to extract from a text node. **/
    public static final String LENGTH = "length";
    
    /** The node for the operation. **/
    public static final String NODE = "node";
    
    /** Delete operation element. **/
    public static final String DELETE = "delete";
    
    /** Character position in text of the node. **/
    public static final String CHARPOS = "charpos";
    
    /** Name of the node. **/
    public static final String NAME = "name";
    
    /** Child number of parent node. **/
    public static final String CHILDNO = "childno";
    
    /** DOM type of the node. **/
    public static final String NODETYPE = "nodetype";
    
    /** Parent of the node. **/
    public static final String PARENT = "parent";
    
    /** Insert operation element. **/ 
    public static final String INSERT = "insert";

    /** Update operation element. **/ 
    public static final String UPDATE = "update";
    
    /** If entities were resolved when creating the delta. **/
    public static final String RESOLVE_ENTITIES = "resolve_entities";
    
    /** False constant. **/
    public static final String FALSE = "false";
    
    /** True constant. **/
    public static final String TRUE = "true";
    
}
