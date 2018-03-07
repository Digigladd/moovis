package net.busynot.moovis.rtsocket.listener.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonCreator;

import java.time.LocalDateTime;

@JsonDeserialize
@Wither
@Value
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class IntervalData implements Jsonable {

    LocalDateTime dateTime;
    Integer value;

    public static IntervalData create() {
        return new IntervalData(LocalDateTime.now(),0);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).
                append(dateTime.getHour()).
                append(dateTime.getMinute() / 30).
                toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) {
            return false;
        }
        IntervalData rhs = (IntervalData) obj;
        return new EqualsBuilder()
                .append(dateTime.getHour(), rhs.dateTime.getHour())
                .append(dateTime.getMinute() / 30, rhs.dateTime.getMinute() / 30)
                .isEquals();
    }
}
