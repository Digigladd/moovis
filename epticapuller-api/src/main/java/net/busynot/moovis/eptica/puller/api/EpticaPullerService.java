package net.busynot.moovis.eptica.puller.api;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.Method;


import static com.lightbend.lagom.javadsl.api.Service.pathCall;
import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;

/*
 * Eptica Puller Service
 */
public interface EpticaPullerService extends Service {

    /*
     * Schedule pulling of group
     */
    ServiceCall<NotUsed,EpticaSupervision> monitor(String group);
    /*
     * Get supervision data of group
     */
    ServiceCall<NotUsed,EpticaSupervision> check(String group);
    /*
     * Disable pulling of group
     */
    ServiceCall<NotUsed,EpticaSupervision> cancel(String group);

    @Override
    default Descriptor descriptor() {
        return named("epticapuller").withCalls(
                pathCall("/epticapuller/monitor/:group",this::monitor),
                pathCall("/epticapuller/check/:group",this::check),
                restCall(Method.DELETE, "/epticapuller/monitor/:group",this::cancel)
        ).withAutoAcl(true);
    }
}
