package net.busynot.moovis.eptica.puller.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.AllArgsConstructor;
import lombok.Value;

public interface EpticaCommand extends Jsonable {

    String getGroup();

    @JsonDeserialize
    @Value
    @AllArgsConstructor(onConstructor = @__(@JsonCreator))
    final class Pull implements EpticaCommand, Jsonable {
        String group;
    }

    @JsonDeserialize
    @Value
    @AllArgsConstructor(onConstructor = @__(@JsonCreator))
    final class SchedulePull implements EpticaCommand, Jsonable {
        String group;
    }

    @JsonDeserialize
    @Value
    @AllArgsConstructor(onConstructor = @__(@JsonCreator))
    final class CancelPull implements EpticaCommand, Jsonable {
        String group;
    }

    @JsonDeserialize
    @Value
    @AllArgsConstructor(onConstructor = @__(@JsonCreator))
    final class ScheduledPull implements EpticaCommand, Jsonable {
        String group;
    }
}
