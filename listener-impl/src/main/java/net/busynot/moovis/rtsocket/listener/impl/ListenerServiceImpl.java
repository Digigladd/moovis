package net.busynot.moovis.rtsocket.listener.impl;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.pubsub.PubSubRegistry;
import com.lightbend.lagom.javadsl.pubsub.TopicId;
import net.busynot.moovis.rtsocket.listener.api.ListenerService;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.CompletableFuture;

public class ListenerServiceImpl implements ListenerService {

    private final PubSubRegistry pubSub;
    private final ActorRef accumulator;

    @Inject
    public ListenerServiceImpl(PubSubRegistry pubSub,
                               @Named("accumulator") ActorRef accumulator) {
        this.pubSub = pubSub;
        this.accumulator = accumulator;
    }

    @Override
    public ServiceCall<NotUsed, Source<String, ?>> sessionStream() {
        return request -> CompletableFuture.completedFuture(pubSub.refFor(TopicId.of(String.class,"rtsocket")).subscriber());
    }
}