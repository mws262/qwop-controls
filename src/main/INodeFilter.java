package main;

import java.util.List;

public interface INodeFilter {

    /**
     * Decide if this node should be included or filtered out. False means filtered out.
     **/
    boolean filter(Node node);

    /**
     * Decide which of these should be kept. Alters the list in place.
     **/
    void filter(List<Node> nodes);
}
