package net.busynot.moovis.eptica.supervision.api;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;

import static com.lightbend.lagom.javadsl.api.Service.pathCall;
import static com.lightbend.lagom.javadsl.api.Service.named;


/*
 * Eptica Supervision servlet interface
 * Default url is http://eptica/supervision
 */
public interface EpticaSupervisionService extends Service {
    ServiceCall<NotUsed,String> monitor(String action,String group);

    @Override
    default Descriptor descriptor() {
        return named("epticasupervision").withCalls(
                pathCall("/eptica/supervision?action&group",this::monitor)
                        .withResponseSerializer(new PlainTextSerializer())
        ).withAutoAcl(true);
    }
}
