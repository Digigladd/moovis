package net.busynot.moovis.eptica.puller.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;

@JsonDeserialize
@Wither
@Value
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class EpticaRequests implements Jsonable {
    Integer received;
    Integer waiting;
    Integer postponed;
    Integer incall;
    Integer review;
    Integer invalid;
    Integer pool;
    Integer reserve;
}
