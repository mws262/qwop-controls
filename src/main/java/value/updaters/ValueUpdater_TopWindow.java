//package value.updaters;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import game.action.Action.Keys;
//import org.apache.avro.generic.GenericData;
//import org.jcodec.common.Preconditions;
//import tree.node.NodeQWOPBase;
//
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Map;
//
//import static java.util.stream.Collectors.groupingBy;
//
//public class ValueUpdater_TopWindow implements IValueUpdater {
//
//    public final int windowSize;
//
//    public ValueUpdater_TopWindow(@JsonProperty("windowSize") int windowSize) {
//        Preconditions.checkArgument(windowSize > 0, "Window size should be at least 1.", windowSize);
//        Preconditions.checkArgument(windowSize % 2 == 1, "Window size should be odd to be symmetric about one action" +
//                ".", windowSize);
//
//        this.windowSize = windowSize;
//    }
//
//    @Override
//    public float update(float valueUpdate, NodeQWOPBase<?> node) {
//        if (node.getChildCount() == 0) {
//            return valueUpdate;
//        } else {
//            List<NodeQWOPBase<?>> children = new ArrayList<>();
//            node.applyToThis(n -> children.addAll(n.getChildren())); // Trick to get around type erasure.
//
//            children.sort(Comparator.comparing(NodeQWOPBase::getAction)); // Sort by actions. Separated first by key,
//            // then by duration, ascending.
//
//            for (int i = 0; i < children.size(); i++) {
//                NodeQWOPBase<?> child = children.get(i);
//
//
//
//
//
//
//            }
//
//
//            Map<Keys, List<NodeQWOPBase<?>>> actionsPerKeyCombo = children.stream()
//                    .collect(groupingBy(n -> n.getAction().getKeys()));
//
//            float bestValue = -Float.MAX_VALUE;
//            NodeQWOPBase<?> bestNode;
//
////            // If there's a window of actions already big
////            for (Map.Entry<Keys, List<NodeQWOPBase<?>>> entry : actionsPerKeyCombo.entrySet()) {
////                List<NodeQWOPBase<?>> nodeList = entry.getValue();
////                if (nodeList.size() >= windowSize) {
////                    int halfWindow = windowSize / 2;
////                    for (int i = halfWindow; i < nodeList.size() - halfWindow - 1; i++) {
////                        float val = nodeList.subList(i - halfWindow, i + halfWindow)
////                                        .stream()
////                                        .map(NodeQWOPBase::getValue)
////                                        .reduce(0f, Float::sum);
////
////                        if (val > bestValue) {
////                            bestValue = val;
////                            bestNode = nodeList.get(i);
////                        }
////                    }
////                }
////            }
//        }
//        return 0;
//    }
//
//    private List<List<NodeQWOPBase<?>>> findWindowInSortedList(List<NodeQWOPBase<?>> nlist, int windowSize) {
//        List<List<NodeQWOPBase<?>>> windowListList = new ArrayList<>();
//
//        int halfWindow = windowSize/2;
//        for (int i = halfWindow; i < nlist.size() - halfWindow - 1; i++) {
//        }
//    }
//}
