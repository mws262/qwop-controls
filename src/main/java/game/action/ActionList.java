package game.action;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import distributions.Distribution;
import distributions.Distribution_Equal;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.IOException;
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
@JsonSerialize(using = ActionList.ActionListSerializer.class)
@JsonDeserialize(using = ActionList.ActionListDeserializer.class)
@JacksonXmlRootElement(localName = "ActionList")
public class ActionList extends ArrayList<Action> {

    private final Distribution<Action> samplingDistribution;

    /**
     * Create a new ActionList which can sample according to the rules of a {@link Distribution}. It may otherwise be
     * treated as an {@link ArrayList}.
     *
     * @param samplingDistribution Distribution that samples of the action set will be pulled when calling
     * {@link ActionList#sampleDistribution}.
     */
    public ActionList(@JsonProperty("samplingDistribution") Distribution<Action> samplingDistribution) {
        this.samplingDistribution = samplingDistribution;
    }

    /**
     * Get an element from this ActionList at random.
     *
     * @return A random element of this ActionList.
     * @see Distribution#randSample(List)
     */
    @JsonIgnore
    public Action getRandom() {
        return samplingDistribution.randSample(this);
    }

    /**
     * Get a random sample from the defined distribution.
     *
     * @return An action sampled from this ActionList according to its defined {@link Distribution}.
     **/
    public Action sampleDistribution() {
        return samplingDistribution.randOnDistribution(this);
    }

    /**
     * Duplicate this ActionList, producing a copy with the same {@link Action} elements and same sampling
     * {@link Distribution}.
     *
     * @return A getCopy of this ActionList.
     */
    public ActionList getCopy() {
        ActionList duplicate = new ActionList(samplingDistribution);
        duplicate.addAll(this);
        return duplicate;
    }

    /**
     * Same but for one set of keys and multiple durations.
     *
     * @param durations Timestep duration of the game.action to be created.
     * @param keys Single set of keys to be pressed for the specified durations.
     * @param dist Selection distribution for sampling over the {@link ActionList}.
     * @return ActionList created with one {@link Action} per duration specified.
     */
    public static ActionList makeActionList(int[] durations, CommandQWOP keys, Distribution<Action> dist) {
        ActionList set = new ActionList(dist);
        for (int duration : durations) {
            set.add(new Action(duration, keys));
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
    public static ActionList makeExhaustiveActionList(int minDuration, int maxDuration,
                                                      Distribution<Action> distribution) {
        assert minDuration > 0;
        assert maxDuration > minDuration;

        ActionList set = new ActionList(distribution);
        for (CommandQWOP.Keys key : CommandQWOP.Keys.values()) {
            for (int i = minDuration; i < maxDuration; i++) {
                set.add(new Action(i, key));
            }
        }
        return set;
    }

    /**
     * Get an ActionList containing no elements.
     * @return An empty list of game.action.
     */
    public static ActionList getEmptyList() {
        return new ActionList(new Distribution_Equal());
    }

    @Override
    public boolean add(Action action) {
        if (contains(action)) {
            return false;
        } else {
            return super.add(action);
        }
    }

    @Override
    public boolean addAll(Collection<? extends Action> actions) {
        for (Action action : actions) {
            if (!contains(action)) {
                super.add(action);
            }
        }
        return true;
    }

    @Override
    public void add(int index, Action action) {
        if (!contains(action)) {
            super.add(index, action);
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends Action> actions) {
        List<Action> alist = new ArrayList<>(actions);
        for (Action action : actions) {
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
        for (Action a : this) {
            hash.append(a); // FIXME Technically this isn't quite right because order is going to matter.
        }
        return hash.toHashCode();
    }

    public Distribution<Action> getSamplingDistribution() {
        return samplingDistribution;
    }

    public static class ActionListDeserializer extends StdDeserializer<ActionList> {

        public ActionListDeserializer() {
            this(null);
        }

        public ActionListDeserializer(Class<ActionList> t) {
            super(t);
        }

        @Override
        public ActionList deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = p.getCodec().readTree(p);

            JsonNode alistNode = node;//node.get("actionList");

            Distribution<Action> dist = mapper.convertValue(alistNode.get("distribution"),
                    new TypeReference<Distribution<Action>>() {});

            ActionList alist = new ActionList(dist);

            Iterator<String> fieldIterator = alistNode.fieldNames();
            while(fieldIterator.hasNext()) {
                String actionField = fieldIterator.next();
                String durationList = alistNode.get(actionField).asText();
                try {
                    CommandQWOP.Keys keys = CommandQWOP.Keys.valueOf(actionField);
                    String[] durations = durationList.split(" ");
                    for (String duration : durations) {
                        alist.add(new Action(Integer.parseInt(duration), keys));
                    }
                } catch (IllegalArgumentException ignored) {} // For any fields which are not keys.
            }
            return alist;
        }
    }

    public static class ActionListSerializer extends StdSerializer<ActionList> {

        public ActionListSerializer() {
            this(null);
        }

        public ActionListSerializer(Class<ActionList> t) {
            super(t);
        }

        @Override
        public void serialize(ActionList value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

            Map<String, String> actions = convert(value);
            jgen.writeStartObject();
            jgen.writeObjectField("distribution", value.getSamplingDistribution());
            for (Map.Entry<String, String> a : actions.entrySet()) {
                jgen.writeStringField(a.getKey(), a.getValue());
            }
            jgen.writeEndObject();
        }
        public Map<String, String> convert(ActionList alist) {
            Map<String, String> actionMap = new HashMap<>();
            Set<CommandQWOP.Keys> presentKeys = new HashSet<>();
            for (Action a : alist) {
                presentKeys.add(a.peek().keys);
            }

            for (CommandQWOP.Keys keys : presentKeys) {
                StringBuilder sb = new StringBuilder();
                for (Action a : alist) {
                    if (a.peek().keys == keys) {
                        sb.append(a.getTimestepsTotal()).append(" ");
                    }
                }
                actionMap.put(keys.toString(), sb.toString());
            }
            return actionMap;
        }
    }
}
