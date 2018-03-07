package net.busynot.moovis.uccx.puller.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;
import org.codehaus.jackson.annotate.JsonCreator;

@JsonDeserialize
@Wither
@Value
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class AgentState implements Jsonable {


    String agentName;
    Integer eventType;
    Integer reasonCode;
    String skill;
    Long eventtime;


}
