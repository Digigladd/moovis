package net.busynot.moovis.eptica.puller.impl;

import akka.Done;
import com.google.inject.Inject;
import com.lightbend.lagom.discovery.zookeeper.ZooKeeperServiceLocator;
import com.lightbend.lagom.discovery.zookeeper.ZooKeeperServiceRegistry;
import com.lightbend.lagom.javadsl.api.ServiceInfo;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.Environment;
import play.inject.ApplicationLifecycle;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class ZooKeeperLocator {

    private final ApplicationLifecycle applicationLifecycle;
    private final Environment environment;
    private final Logger log = LoggerFactory.getLogger(ZooKeeperLocator.class);
    private ZooKeeperServiceRegistry registry;
    private ServiceInstance<String> serviceInstance;
    private ServiceInstance<String> epticaServiceInstance;

    @Inject
    public ZooKeeperLocator(ApplicationLifecycle applicationLifecycle,
                            Environment environment,
                            ZooKeeperServiceLocator.Config zconfig,
                            Configuration config,
                            ServiceInfo serviceInfo) throws Exception {
        this.applicationLifecycle = applicationLifecycle;
        this.environment = environment;
        Random rand = new Random();
        if (environment.isProd()) {

            registry = new ZooKeeperServiceRegistry(
                    ZooKeeperServiceLocator.zkUri(zconfig.serverHostname(),zconfig.serverPort()),
                    ZooKeeperServiceLocator.defaultZKServicesPath()
            );
            registry.start();

            serviceInstance = ServiceInstance.<String>builder()
                    .name(serviceInfo.serviceName())
                    .id(String.valueOf(rand.nextInt()))
                    .address(serviceInfo.serviceName())
                    .port(config.getInt("http.port"))
                    .uriSpec(new UriSpec("{scheme}://{serviceAddress}:{servicePort}"))
                    .build();

            String epticaHost = config.getString("eptica.host");

            epticaServiceInstance = ServiceInstance.<String>builder()
                    .name("epticasupervision")
                    .id(String.valueOf(rand.nextInt()))
                    .address(epticaHost)
                    .port(80)
                    .uriSpec(new UriSpec("http://"+epticaHost+"80"))
                    .build();


            log.info("Registering {} Service...",serviceInfo.serviceName());
            registry.register(serviceInstance);
            log.info("Registering {} Eptica Supervision...",epticaServiceInstance.getAddress());
            registry.register(epticaServiceInstance);

            applicationLifecycle.addStopHook(() -> {
                log.info("{} Service stopping", serviceInfo.serviceName());
                registry.unregister(serviceInstance);
                registry.unregister(epticaServiceInstance);
                return CompletableFuture.completedFuture(Done.getInstance());
            });
        }
    }
}