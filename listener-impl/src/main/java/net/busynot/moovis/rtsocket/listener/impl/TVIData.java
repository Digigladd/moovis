package net.busynot.moovis.rtsocket.listener.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;
import org.codehaus.jackson.annotate.JsonCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonDeserialize
@Wither
@Value
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class TVIData implements Jsonable {

    IntervalData currentAcdCalls;
    Integer acdCalls;
    IntervalData currentAbnCalls;
    Integer abnCalls;
    IntervalData currentOfferedCalls;
    Integer offeredCalls;
    String split;

    private static Logger log = LoggerFactory.getLogger(TVIData.class);

    public static TVIData create(String split) {
        return new TVIData(IntervalData.create(), 0, IntervalData.create(), 0,IntervalData.create(), 0,split);
    }

    public TVIData updateAcdCalls(IntervalData data) {
        if (data.equals(this.currentAcdCalls)) {
            return this.withCurrentAcdCalls(data);
        } else {
            return this
                    .withAcdCalls(callReceived())
                    .withCurrentAcdCalls(data);
        }
    }

    public TVIData updateAbnCalls(IntervalData data) {
        if (data.equals(this.currentAbnCalls)) {
            return this.withCurrentAbnCalls(data);
        } else {
            return this
                    .withAbnCalls(callAbandoned())
                    .withCurrentAbnCalls(data);
        }
    }

    public TVIData updateOfferedCalls(IntervalData data) {
        if (data.equals(this.currentOfferedCalls)) {
            return this.withCurrentOfferedCalls(data);
        } else {
            return this
                    .withOfferedCalls(callOffered())
                    .withCurrentOfferedCalls(data);
        }
    }

    public Integer callReceived() {
        return this.getCurrentAcdCalls().getValue() + this.getAcdCalls();
    }

    public Integer callAbandoned() {
        return this.getCurrentAbnCalls().getValue() + this.getAbnCalls();
    }

    public Integer callOffered() {
        return this.getCurrentOfferedCalls().getValue() + this.getOfferedCalls();
    }

    public TVIData reset() {
        return TVIData.create(split);
    }
}
