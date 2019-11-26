package value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameSerializable;
import game.qwop.CommandQWOP;
import game.qwop.IStateQWOP;
import game.state.transform.ITransform;

import java.io.File;
import java.io.IOException;

public class ValueFunction_TensorFlow_StateOnlyLoaded<S extends IStateQWOP> extends ValueFunction_TensorFlow_StateOnly<S> {

    @JsonCreator
    public ValueFunction_TensorFlow_StateOnlyLoaded(@JsonProperty("modelFile") File modelFile,
                                                    @JsonProperty("gameTemplate") IGameSerializable<CommandQWOP, S> gameTemplate,
                                                    @JsonProperty("stateNormalizer") ITransform<S> stateNormalizer,
                                                    @JsonProperty("activeCheckpoint") String checkpointFile,
                                                    @JsonProperty("keepProbability") float keepProbability,
                                                    @JsonProperty("tensorboardLogging") boolean tensorboardLogging) throws IOException {
        super(modelFile, gameTemplate, stateNormalizer, checkpointFile, keepProbability, tensorboardLogging);
    }

    @JsonIgnore
    public String getFileName() { // Exists to keep this field from being written by Jackson.
        return fileName;
    }
}
