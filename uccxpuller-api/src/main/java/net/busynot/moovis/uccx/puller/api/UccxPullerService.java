package net.busynot.moovis.uccx.puller.api;


import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import static com.lightbend.lagom.javadsl.api.Service.pathCall;
import static com.lightbend.lagom.javadsl.api.Service.named;

/*
 * Uccx Puller Service
 */
public interface UccxPullerService extends Service {

    ServiceCall<NotUsed,Source<CSQSummary,?>> csqSummaryStream();
    ServiceCall<NotUsed,Source<AgentState,?>> agentStateStream();

    @Override
    default Descriptor descriptor() {
        return named("uccxpuller").withCalls(
                pathCall("/rt/csqs",this::csqSummaryStream),
                pathCall("rt/agents", this::agentStateStream)
        );
    }

}