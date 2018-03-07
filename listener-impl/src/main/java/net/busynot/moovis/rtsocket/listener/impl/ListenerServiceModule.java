package net.busynot.moovis.rtsocket.listener.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import net.busynot.moovis.rtsocket.listener.api.ListenerService;
import play.libs.akka.AkkaGuiceSupport;

public class ListenerServiceModule extends AbstractModule implements ServiceGuiceSupport,AkkaGuiceSupport {

    @Override
    protected void configure() {
        bindService(ListenerService.class,ListenerServiceImpl.class);
        bind(RtSocketListener.class).asEagerSingleton();
        bindActor(AccumulatorActor.class, "accumulator");

    }
}
