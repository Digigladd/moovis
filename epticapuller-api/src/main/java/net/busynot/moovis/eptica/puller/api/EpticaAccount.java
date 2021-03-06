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
public class EpticaAccount implements Jsonable {

    Integer all;
    Integer enabled;
    Integer connected;
}
