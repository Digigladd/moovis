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
public class CSQSummary implements Jsonable {


    String csqName;
    Integer loggedInAgents;
    Integer availableAgents;
    Integer unavailableAgents;
    Integer totalCalls;
    Integer oldestContact;
    Integer callsHandled;
    Integer callsAbandoned;
    Integer callsDequeued;
    Integer avgTalkDuration;
    Integer avgWaitDuration;
    Integer longestTalkDuration;
    Integer longestWaitDuration;
    Integer callsWaiting;
    Integer workingAgents;
    Integer talkingAgents;
    Integer reservedAgents;
    String convAvgTalkDuration;
    String convAvgWaitDuration;
    String convLongestTalkDuration;
    String convLongestWaitDuration;
    String convOldestContact;


}
