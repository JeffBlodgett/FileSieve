package FileSieve.gui;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


/** Example of a simple static TreeModel. It contains a
*    (java.io.File) directory structure.
*    (C) 2001 Christian Kaufhold (ch-kaufhold@gmx.de)
*    taken from www.chka.de/swing/tree/FileTreeModel.java
*    fixed to include only directories
*/
public class FileTreeModel
    implements TreeModel, Serializable{
    
    private static final Object LEAF = new Serializable() { };
    private Map map;
    private File root;

    FileTreeModel(File root){
        this.root = root;

        if (!root.isDirectory()){
            map.put(root, LEAF);
        }

        this.map = new HashMap();
    }


    @Override
    public Object getRoot(){
        return root;
    }

    @Override
    public boolean isLeaf(Object node){
        return ((File) node).isFile();
    }

    @Override
    public int getChildCount(Object node){
        List children = children(node);

        if (children == null){
            return 0;
        }

        return children.size();
    }

    @Override
    public Object getChild(Object parent, int index){
        return children(parent).get(index);
    }

    @Override
    public int getIndexOfChild(Object parent, Object child){
        return children(parent).indexOf(child);
    }

    protected List children(Object node){
        File f = (File)node;

        Object value = map.get(f);

        if (value == LEAF){
            return null;
        }

        List children = (List)value;

        if (children == null){
            File[] c = f.listFiles();

            if (c != null){
                children = new ArrayList(c.length);

                for (int len = c.length, i = 0; i < len; i++){
                    if(c[i].isDirectory() && !c[i].isHidden()){
                        children.add(c[i]);
                        if (!c[i].isDirectory()){
                            map.put(c[i], LEAF);
                        }
                    }
                }
            } else {
                children = new ArrayList(0);
            }

            map.put(f, children);       
        }

        return children;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object value){}

    @Override
    public void addTreeModelListener(TreeModelListener l){}

    @Override
    public void removeTreeModelListener(TreeModelListener l){}

}