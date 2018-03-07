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
public class EpticaAgent implements Jsonable {

    Integer all;
    Integer enabled;
    Integer emailAll;
    Integer emailEnabled;
    Integer autoAll;
    Integer autoEnabled;
    Integer networkAll;
    Integer networkEnabled;
    Integer guiAll;
    Integer guiEnabled;
    Integer guiOnline;
    Integer guiOffline;
    Integer guiWaiting;
    Integer guiManual;
    Integer guiPaused;
    Integer guiBusy;
    Integer guiIncall;
}
