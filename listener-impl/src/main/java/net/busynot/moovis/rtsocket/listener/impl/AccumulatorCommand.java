package net.busynot.moovis.rtsocket.listener.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Value;

public interface AccumulatorCommand {

    enum ResetAccumulator implements AccumulatorCommand {
        INSTANCE;
    }

    enum Snapshot implements AccumulatorCommand {
        INSTANCE;
    }

    @JsonDeserialize
    @Value
    @AllArgsConstructor(onConstructor = @__(@JsonCreator))
    final class UpdateAbnCalls implements AccumulatorCommand {
        String split;
        IntervalData value;
    }

    @JsonDeserialize
    @Value
    @AllArgsConstructor(onConstructor = @__(@JsonCreator))
    final class UpdateAcdCalls implements AccumulatorCommand {
        String split;
        IntervalData value;
    }

    @JsonDeserialize
    @Value
    @AllArgsConstructor(onConstructor = @__(@JsonCreator))
    final class UpdateIntervalData implements AccumulatorCommand {
        String split;
        IntervalData acdCalls;
        IntervalData abnCalls;
        IntervalData offeredCalls;
    }
}
