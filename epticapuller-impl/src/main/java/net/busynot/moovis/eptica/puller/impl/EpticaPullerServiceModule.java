package net.busynot.moovis.eptica.puller.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import net.busynot.moovis.eptica.puller.api.EpticaPullerService;
import net.busynot.moovis.eptica.supervision.api.EpticaSupervisionService;
import play.libs.akka.AkkaGuiceSupport;

public class EpticaPullerServiceModule extends AbstractModule implements ServiceGuiceSupport, AkkaGuiceSupport {

    @Override
    protected void configure() {
        bindService(EpticaPullerService.class,EpticaPullerServiceImpl.class);
        bind(ZooKeeperLocator.class).asEagerSingleton();
        bindClient(EpticaSupervisionService.class);
        bindActor(EpticaActor.class, "epticaPuller");
    }
}
