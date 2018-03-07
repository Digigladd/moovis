package net.busynot.moovis.rtsocket.listener.impl;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import com.lightbend.lagom.javadsl.pubsub.PubSubRegistry;
import com.lightbend.lagom.javadsl.pubsub.TopicId;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.inject.ApplicationLifecycle;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Singleton
public class RtSocketListener {

    final private ActorSystem system;
    final Source<Tcp.IncomingConnection, CompletionStage<Tcp.ServerBinding>> connections;
    final static Logger log = LoggerFactory.getLogger(RtSocketListener.class);
    final private PubSubRegistry pubSub;
    final private InfluxDB influxDB;
    private final static String db = "MOOVIS";
    private final static String policy = "MOOVIS_POLICY";
    private final ActorRef accumulator;


    @Inject
    public RtSocketListener(ApplicationLifecycle applicationLifecycle,
                            ActorSystem system,
                            Materializer mat,
                            PubSubRegistry pubSub,
                            Configuration config,
                            @Named("accumulator")ActorRef accumulator) {
        this.system = system;
        this.pubSub = pubSub;
        this.accumulator = accumulator;

        log.info("Connect to InfluxDB {}",config.getString("influxdb.url"));
        influxDB = InfluxDBFactory.connect(config.getString("influxdb.url"));

        log.info("Creating database {}",db);
        influxDB.createDatabase(db);
        log.info("Set database {}",db);
        influxDB.setDatabase(db);
        log.info("Creating retention policy {}",db);
        influxDB.createRetentionPolicy(policy, db, "3d", "1d", 2, true);
        influxDB.setRetentionPolicy(policy);

        influxDB.enableBatch(2000,
                1000,
                TimeUnit.MILLISECONDS
        );
        influxDB.enableGzip();

        log.info("Binding {} port {}", "0.0.0.0", 7002);
        connections = Tcp.get(system).bind("0.0.0.0", 7002);

        connections.runForeach(connection -> {

            log.info("New connection from {}", connection.remoteAddress());

            final Flow<ByteString, ByteString, NotUsed> rtlistener = Flow.of(ByteString.class)
                    .via(Framing.delimiter(ByteString.fromString("\n"), 256, FramingTruncation.DISALLOW))
                    .map(ByteString::utf8String)
                    .via(Flow.fromGraph(new ListenerStage(pubSub.refFor(TopicId.of(String.class,"rtsocket")),influxDB,accumulator)));

            connection.handleWith(rtlistener, mat);
        }, mat);

        applicationLifecycle.addStopHook(() -> {
            if (influxDB != null) {
                influxDB.close();
            }
            return CompletableFuture.completedFuture(Done.getInstance());
        });
    }

}
