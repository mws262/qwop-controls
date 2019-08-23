package game.action;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import distributions.Distribution;
import distributions.Distribution_Equal;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * An ActionList acts like an {@link java.util.ArrayList} for {@link Action game.action}, except it allows for sampling
 * from the list on a {@link Distribution}. DOES NOT ALLOW DUPLICATES!!
 *
 * @author Matt
 * @see Action
 * @see ActionQueue
 * @see IActionGenerator
 * @see Distribution
 */
//@JsonSerialize(using = ActionList.ActionListSerializer.class)
//@JsonDeserialize(using = ActionList.ActionListDeserializer.class)
@JacksonXmlRootElement(localName = "ActionList")
public class ActionList<C extends Command<?>> extends ArrayList<Action<C>> {

    private final Distribution<Action<C>> samplingDistribution;

    /**
     * Create a new ActionList which can sample according to the rules of a {@link Distribution}. It may otherwise be
     * treated as an {@link ArrayList}.
     *
     * @param samplingDistribution Distribution that samples of the action set will be pulled when calling
     * {@link ActionList#sampleDistribution}.
     */
    public ActionList(@JsonProperty("samplingDistribution") Distribution<Action<C>> samplingDistribution) {
        this.samplingDistribution = samplingDistribution;
    }

    /**
     * Get an element from this ActionList at random.
     *
     * @return A random element of this ActionList.
     * @see Distribution#randSample(List)
     */
    @JsonIgnore
    public Action<C> getRandom() {
        return samplingDistribution.randSample(this);
    }

    /**
     * Get a random sample from the defined distribution.
     *
     * @return An action sampled from this ActionList according to its defined {@link Distribution}.
     **/
    public Action<C> sampleDistribution() {
        return samplingDistribution.randOnDistribution(this);
    }

    /**
     * Duplicate this ActionList, producing a copy with the same {@link Action} elements and same sampling
     * {@link Distribution}.
     *
     * @return A getCopy of this ActionList.
     */
    public ActionList<C> getCopy() {
        ActionList<C> duplicate = new ActionList<>(samplingDistribution);
        duplicate.addAll(this);
        return duplicate;
    }

    /**
     * Same but for one set of keys and multiple durations.
     *
     * @param durations Timestep duration of the game.action to be created.
     * @param dist Selection distribution for sampling over the {@link ActionList}.
     * @return ActionList created with one {@link Action} per duration specified.
     */
    public static <C extends Command<?>> ActionList<C> makeActionList(@NotNull int[] durations, @NotNull C command,
                                                                      @NotNull Distribution<Action<C>> dist) {
        ActionList<C> set = new ActionList<>(dist);
        for (int duration : durations) {
            set.add(new Action<>(duration, command));
        }
        return set;
    }

    /**
     * Make an {@link ActionList} containing all key combinations for all durations between the specified bounds.
     * @param minDuration Inclusive bound. Minimum {@link Action} duration to be found in the list.
     * @param maxDuration Exclusive bound. Upper bound on durations of game.action found in this list.
     * @param distribution Distribution for sampling game.action once this list is created.
     * @return An exhaustive {@link ActionList}.
     */
    @SuppressWarnings("unused")
    public static <C extends Command<?>> ActionList<C> makeExhaustiveActionList(int minDuration,
                                                                             int maxDuration,
                                                                             @NotNull List<C> commands,
                                                                             @NotNull Distribution<Action<C>> distribution) {
        assert minDuration > 0;
        assert maxDuration > minDuration;

        ActionList<C> set = new ActionList<>(distribution);
        for (C command : commands) {
            for (int i = minDuration; i < maxDuration; i++) {
                set.add(new Action<>(i, command));
            }
        }
        return set;
    }

    public static ActionList<CommandQWOP> makeExhaustiveQWOPActionList(int minDuration, int maxDuration,
                                                                       Distribution<Action<CommandQWOP>> distribution) {
        List<CommandQWOP> commands = new ArrayList<>();
        commands.add(CommandQWOP.NONE);
        commands.add(CommandQWOP.Q);
        commands.add(CommandQWOP.W);
        commands.add(CommandQWOP.O);
        commands.add(CommandQWOP.P);
        commands.add(CommandQWOP.QO);
        commands.add(CommandQWOP.QP);
        commands.add(CommandQWOP.WO);
        commands.add(CommandQWOP.WP);
        return makeExhaustiveActionList(minDuration, maxDuration, commands, distribution);
    }

    /**
     * Get an ActionList containing no elements.
     * @return An empty list of game.action.
     */
    public static <C extends Command<?>> ActionList<C> getEmptyList() {
        return new ActionList<>(new Distribution_Equal<>());
    }

    @Override
    public boolean add(@NotNull Action<C> action) {
        if (contains(action)) {
            return false;
        } else {
            return super.add(action);
        }
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Action<C>> actions) {
        for (Action<C> action : actions) {
            if (!contains(action)) {
                super.add(action);
            }
        }
        return true;
    }

    @Override
    public void add(int index, @NotNull Action<C> action) {
        if (!contains(action)) {
            super.add(index, action);
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends Action<C>> actions) {
        List<Action<C>> alist = new ArrayList<>(actions);
        for (Action<C> action : actions) {
            if (!contains(action)) {
                alist.remove(action);
            }
        }
        super.addAll(index, alist);
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionList other = (ActionList) o;
        EqualsBuilder eb = new EqualsBuilder();
        eb.append(this.samplingDistribution, other.samplingDistribution);
        eb.append(this.size(), other.size());
        eb.appendSuper(this.containsAll(other));
        return eb.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder();
        hash.append(samplingDistribution);
        for (Action<C> a : this) {
            hash.append(a); // FIXME Technically this isn't quite right because order is going to matter.
        }
        return hash.toHashCode();
    }

    public Distribution<Action<C>> getSamplingDistribution() {
        return samplingDistribution;
    }
//
//    public static class ActionListDeserializer<C extends Command<?>> extends StdDeserializer<ActionList<C>> {
//
//        public ActionListDeserializer() {
//            this(null);
//        }
//
//        public ActionListDeserializer(Class<ActionList<C>> t) {
//            super(t);
//        }
//
//        @Override
//        public ActionList<C> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
//            ObjectMapper mapper = new ObjectMapper();
//
//            JsonNode alistNode = p.getCodec().readTree(p);//node.get("actionList");
//
//            Distribution<Action<C>> dist = mapper.convertValue(alistNode.get("distribution"),
//                    new TypeReference<Distribution<Action>>() {});
//
//            ActionList<C> alist = new ActionList<>(dist);
//
//            Iterator<String> fieldIterator = alistNode.fieldNames();
//            while(fieldIterator.hasNext()) {
//                String actionField = fieldIterator.next();
//                String durationList = alistNode.get(actionField).asText();
//                try {
//                    C command = CommandQWOP.Keys.valueOf(actionField); // TODO
//                    String[] durations = durationList.split(" ");
//                    for (String duration : durations) {
//                        alist.add(new Action<>(Integer.parseInt(duration), command));
//                    }
//                } catch (IllegalArgumentException ignored) {} // For any fields which are not keys.
//            }
//            return alist;
//        }
//    }
//
//    public static class ActionListSerializer<C extends Command<?>> extends StdSerializer<ActionList<C>> {
//
//        public ActionListSerializer() {
//            this(null);
//        }
//
//        public ActionListSerializer(Class<ActionList<C>> t) {
//            super(t);
//        }
//
//        @Override
//        public void serialize(ActionList<C> value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
//
//            Map<String, String> actions = convert(value);
//            jgen.writeStartObject();
//            jgen.writeObjectField("distribution", value.getSamplingDistribution());
//            for (Map.Entry<String, String> a : actions.entrySet()) {
//                jgen.writeStringField(a.getKey(), a.getValue());
//            }
//            jgen.writeEndObject();
//        }
//        public Map<String, String> convert(ActionList<C> alist) {
//            Map<String, String> actionMap = new HashMap<>();
//            Set<C> presentCommands = new HashSet<>();
//            for (Action<C> a : alist) {
//                presentCommands.add(a.getCommand());
//            }
//
//            for (C command : presentCommands) {
//                StringBuilder sb = new StringBuilder();
//                for (Action<C> a : alist) {
//                    if (a.peek().equals(command)) {
//                        sb.append(a.getTimestepsTotal()).append(" ");
//                    }
//                }
//                actionMap.put(command.toString(), sb.toString());
//            }
//            return actionMap;
//        }
//    }
}
