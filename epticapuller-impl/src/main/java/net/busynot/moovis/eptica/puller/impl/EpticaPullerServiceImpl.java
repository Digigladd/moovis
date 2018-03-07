package net.busynot.moovis.eptica.puller.impl;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.util.Timeout;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import net.busynot.moovis.eptica.puller.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.compat.java8.FutureConverters;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class EpticaPullerServiceImpl implements EpticaPullerService {

    private final ActorRef epticaPuller;
    private final static Logger log = LoggerFactory.getLogger(EpticaPullerServiceImpl.class);

    @Inject
    public EpticaPullerServiceImpl(@Named("epticaPuller") ActorRef epticaPuller) {
        this.epticaPuller = epticaPuller;
    }

    @Override
    public ServiceCall<NotUsed, EpticaSupervision> monitor(String group) {
        return request -> {

                return pull(new EpticaCommand.SchedulePull(group));
        };
    }

    @Override
    public ServiceCall<NotUsed, EpticaSupervision> check(String group) {
        return request -> {

            return pull(new EpticaCommand.Pull(group));
        };
    }

    @Override
    public ServiceCall<NotUsed, EpticaSupervision> cancel(String group) {
        return request -> {

            return pull(new EpticaCommand.CancelPull(group));
        };
    }

    private CompletionStage<EpticaSupervision> pull(EpticaCommand pull) {

        return FutureConverters.toJava(ask(epticaPuller,
                pull,
                Timeout.apply(25, TimeUnit.SECONDS)
        )).thenApply(response -> (EpticaSupervision)response);
    }
}
