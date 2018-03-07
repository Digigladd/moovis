package net.busynot.moovis.eptica.puller.impl;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.UntypedActor;
import com.google.common.cache.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.busynot.moovis.eptica.puller.api.*;
import net.busynot.moovis.eptica.supervision.api.EpticaSupervisionService;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class EpticaActor extends UntypedActor {

    private final EpticaSupervisionService supervision;
    private static final Logger log = LoggerFactory.getLogger(EpticaActor.class);
    final private InfluxDB influxDB;
    private final static String db = "MOOVIS";
    private final static String policy = "MOOVIS_POLICY";


    RemovalListener<String, Cancellable> removalListener = new RemovalListener<String, Cancellable>() {
        public void onRemoval(RemovalNotification<String, Cancellable> removal) {
            log.info("Removing schedule for {}",removal.getKey());
            Cancellable conn = removal.getValue();
            conn.cancel(); // tear down properly
        }
    };

    LoadingCache<String, Cancellable> pullers = CacheBuilder.newBuilder()
            .removalListener(removalListener)
            .build(
                    new CacheLoader<String, Cancellable>() {
                        public Cancellable load(String key) throws Exception {
                            log.info("Scheduling group {}",key);
                            return getContext().system().scheduler().schedule(
                                    Duration.create(0, TimeUnit.SECONDS),
                                    Duration.create(5, TimeUnit.SECONDS),
                                    getSelf(),
                                    new EpticaCommand.ScheduledPull(key),
                                    getContext().system().dispatcher(),
                                    getSelf());
                        }
                    });

    @Inject
    public EpticaActor(EpticaSupervisionService supervision,
                       Configuration config) {
        this.supervision = supervision;
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

        log.info("Eptica groups configuration: {}",config.getString("eptica.groups"));

        Arrays.stream(config.getString("eptica.groups").split(",")).forEach(group -> {
            log.info("Scheduling supervision for group {}", group);
            getSelf().tell(new EpticaCommand.SchedulePull(group),getSelf());
        });
    }

    @Override
    public void postStop() {
        if (influxDB != null) {
            influxDB.close();
        }
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof EpticaCommand) {

            String group = ((EpticaCommand)message).getGroup();
            final ActorRef sender = getSender();

            CompletionStage<String> supervisionRequest = supervision.monitor("request", group).handleRequestHeader(
                    header -> header.withHeader("Authorization", "Basic bW9uaXRvcjphZG1pbg==")
            ).invoke(NotUsed.getInstance());

            CompletionStage<String> supervisionAccount = supervision.monitor("account", group).handleRequestHeader(
                    header -> header.withHeader("Authorization", "Basic bW9uaXRvcjphZG1pbg==")
            ).invoke(NotUsed.getInstance());

            supervisionRequest
                    .thenCombine(supervisionAccount, (requests, accounts) -> {
                        String combine = requests.concat(accounts);
                        return combine;
                    }).thenAccept(epticaSupervision -> {
                        Config sup = ConfigFactory.parseString(epticaSupervision);
                        sender.tell(EpticaSupervision.fromConfig(group,sup),getSelf());

                        if (message instanceof EpticaCommand.SchedulePull) {
                            if (!sup.isEmpty()) {
                                try {
                                    pullers.get(group);
                                } catch (Exception e) {

                                }
                            }
                        } else if (message instanceof EpticaCommand.CancelPull) {
                            pullers.invalidate(group);
                        }
                    });
        } else if (message instanceof EpticaSupervision) {
            sendMetrics((EpticaSupervision)message);
        } else {
            unhandled(message);
        }
    }

    private void sendRequestsMetrics(EpticaRequests sup, String group) {
        influxDB.write(
                Point.measurement("eptica-requests")
                        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .tag("GROUP", group)
                        .addField("received", sup.getReceived())
                        .addField("waiting", sup.getWaiting())
                        .addField("postponed", sup.getPostponed())
                        .addField("incall", sup.getIncall())
                        .addField("review", sup.getReview())
                        .addField("invalid", sup.getInvalid())
                        .addField("pool", sup.getPool())
                        .addField("reserve", sup.getReserve())
                        .build()
        );
    }

    private void sendAccountMetrics(EpticaAccount sup, String group) {
        influxDB.write(
                Point.measurement("eptica-accounts")
                        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .tag("GROUP", group)
                        .addField("all", sup.getAll())
                        .addField("enabled", sup.getEnabled())
                        .addField("connected", sup.getConnected())
                        .build()
        );
    }

    private void sendAdministratorsMetrics(EpticaAccount sup, String group) {
        influxDB.write(
                Point.measurement("eptica-administrators")
                        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .tag("GROUP", group)
                        .addField("all", sup.getAll())
                        .addField("enabled", sup.getEnabled())
                        .addField("connected", sup.getConnected())
                        .build()
        );
    }

    private void sendSupervisorsMetrics(EpticaAccount sup, String group) {
        influxDB.write(
                Point.measurement("eptica-supervisors")
                        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .tag("GROUP", group)
                        .addField("all", sup.getAll())
                        .addField("enabled", sup.getEnabled())
                        .addField("connected", sup.getConnected())
                        .build()
        );
    }

    private void sendMailMetrics(EpticaMail sup, String group) {
        influxDB.write(
                Point.measurement("eptica-mails")
                        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .tag("GROUP", group)
                        .addField("waiting", sup.getWaiting())
                        .addField("parsed", sup.getParsed())
                        .addField("outgoing", sup.getOutgoing())
                        .build()
        );
    }

    private void sendAgentMetrics(EpticaAgent sup, String group) {
        influxDB.write(
                Point.measurement("eptica-agents")
                        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .tag("GROUP", group)
                        .addField("all", sup.getAll())
                        .addField("enabled", sup.getEnabled())
                        .addField("emailAll", sup.getEmailAll())
                        .addField("emailEnabled", sup.getEmailEnabled())
                        .addField("autoAll", sup.getAutoAll())
                        .addField("autoEnabled", sup.getAutoEnabled())
                        .addField("networkAll", sup.getNetworkAll())
                        .addField("networkEnabled", sup.getNetworkEnabled())
                        .addField("guiAll", sup.getGuiAll())
                        .addField("guiEnabled", sup.getGuiEnabled())
                        .addField("guiOnline", sup.getGuiOnline())
                        .addField("guiOffline", sup.getGuiOffline())
                        .addField("guiWaiting", sup.getGuiWaiting())
                        .addField("guiManual", sup.getGuiManual())
                        .addField("guiPaused", sup.getGuiPaused())
                        .addField("guiBusy", sup.getGuiBusy())
                        .addField("guiIncall", sup.getGuiIncall())
                        .build()
        );
    }

    private void sendMetrics(EpticaSupervision sup) {
        sendRequestsMetrics(sup.getRequests(),sup.getGroup());
        sendAccountMetrics(sup.getAccounts(),sup.getGroup());
        sendSupervisorsMetrics(sup.getSupervisors(),sup.getGroup());
        sendAdministratorsMetrics(sup.getAdministrators(),sup.getGroup());
        sendMailMetrics(sup.getMail(),sup.getGroup());
        sendAgentMetrics(sup.getAgents(),sup.getGroup());
    }
}
