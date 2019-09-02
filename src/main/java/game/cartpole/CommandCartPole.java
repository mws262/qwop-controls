package game.cartpole;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.base.Preconditions;
import game.action.Command;

import java.io.IOException;

@JsonSerialize(using = CommandCartPole.Serializer.class)
@JsonDeserialize(using = CommandCartPole.Deserialize.class)
public class CommandCartPole extends Command<boolean[]> {

    public static final CommandCartPole
        LEFT = new CommandCartPole(new boolean[] {true, false}),
        RIGHT = new CommandCartPole(new boolean[] {false, true});

    private final float[] oneHotEncoding;

    private CommandCartPole(boolean[] commandData) {
        super(commandData);
        oneHotEncoding = new float[] {
                commandData[0] ? 1f : 0f,
                commandData[1] ? 1f : 0f
        };
    }

    public float[] toOneHot() {
        return oneHotEncoding;
    }

    public static CommandCartPole getByIndex(int index) {
        Preconditions.checkArgument(index >= 0 && index < 2, "Index out of bounds.", index);
        return (index == 0) ? LEFT : RIGHT;
    }

    public static class Serializer extends StdSerializer<CommandCartPole> {

        public Serializer() {
            this(null);
        }
        public Serializer(Class<CommandCartPole> t) {
            super(t);
        }

        @Override
        public void serialize(CommandCartPole value, JsonGenerator gen, SerializerProvider provider) throws IOException, JsonProcessingException {
//            gen.writeStartObject();
            gen.writeStringField("direction", value == CommandCartPole.LEFT ? "left" : "right");
        }

        @Override
        public void serializeWithType(CommandCartPole value, JsonGenerator gen,
                                      SerializerProvider provider, TypeSerializer typeSer)
                throws IOException, JsonProcessingException {

            typeSer.writeTypePrefixForObject(value, gen);
            serialize(value, gen, provider); // call your customized serialize method
            typeSer.writeTypeSuffixForObject(value, gen);
        }
    }

    public static class Deserialize extends StdDeserializer<CommandCartPole> {

        public Deserialize() {
            this(null);
        }
        public Deserialize(Class<?> t) {
            super(t);
        }

        @Override
        public CommandCartPole deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            String dir = node.get("direction").asText();

            return dir.equals("left") ? CommandCartPole.LEFT : CommandCartPole.RIGHT;
        }
    }
}
