package net.busynot.moovis.uccx.puller.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import net.busynot.moovis.uccx.puller.api.UccxPullerService;
import play.libs.akka.AkkaGuiceSupport;

public class UccxPullerServiceModule extends AbstractModule implements ServiceGuiceSupport,AkkaGuiceSupport {

    @Override
    protected void configure() {
        bindService(UccxPullerService.class,UccxPullerServiceImpl.class);
        bindActor(AccumulatorActor.class, "accumulator");

    }
}
