package net.busynot.moovis.uccx.puller.impl;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.pubsub.PubSubRegistry;
import com.lightbend.lagom.javadsl.pubsub.TopicId;
import net.busynot.moovis.uccx.puller.api.AgentState;
import net.busynot.moovis.uccx.puller.api.CSQSummary;
import net.busynot.moovis.uccx.puller.api.UccxPullerService;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.CompletableFuture;

public class UccxPullerServiceImpl implements UccxPullerService {

    private final PubSubRegistry pubSub;
    private final ActorRef accumulator;

    @Inject
    public UccxPullerServiceImpl(PubSubRegistry pubSub,
                               @Named("accumulator") ActorRef accumulator) {
        this.pubSub = pubSub;
        this.accumulator = accumulator;
    }

    @Override
    public ServiceCall<NotUsed, Source<CSQSummary, ?>> csqSummaryStream() {
        return request -> CompletableFuture.completedFuture(pubSub.refFor(TopicId.of(CSQSummary.class,"csqs")).subscriber());
    }

    @Override
    public ServiceCall<NotUsed, Source<AgentState, ?>> agentStateStream() {
        return request -> CompletableFuture.completedFuture(pubSub.refFor(TopicId.of(AgentState.class,"agents")).subscriber());
    }
}