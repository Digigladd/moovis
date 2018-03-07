package net.busynot.moovis.uccx.puller.impl;

import akka.actor.Cancellable;
import akka.actor.UntypedActor;
import com.lightbend.lagom.javadsl.pubsub.PubSubRef;
import com.lightbend.lagom.javadsl.pubsub.PubSubRegistry;
import com.lightbend.lagom.javadsl.pubsub.TopicId;
import net.busynot.moovis.uccx.puller.api.AgentState;
import net.busynot.moovis.uccx.puller.api.CSQSummary;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import scala.concurrent.duration.Duration;
import sun.management.Agent;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AccumulatorActor extends UntypedActor {

    private static final Logger log = LoggerFactory.getLogger(AccumulatorActor.class);
    private Connection crsDb = null;
    private ConcurrentHashMap<Integer,String> csqs = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String,CSQSummary> summary = new ConcurrentHashMap<>();
    private final String crsversion = "select * from crsproperties WHERE propertyName = 'PRODUCT_VERSION'";
    private final String sqlCsq = "select contactservicequeueid,csqname from contactservicequeue WHERE active";
    private final String agentState = "select resourcename,eventtype, reasoncode, skillname,eventdatetime from Resource r inner join (select agentid,MAX(eventdatetime) as me from agentstatedetail where eventdatetime >= TODAY GROUP BY 1) g on r.resourceid = g.agentid inner join agentstatedetail asd on asd.agentid = g.agentid and asd.eventdatetime = g.me inner join ResourceSkillMapping rsm on rsm.resourceskillmapid = r.resourceSkillMapID and rsm.profileid = r.profileid inner join Skill s on s.skillid = rsm.skillid and rsm.profileid = s.profileid where r.active = 't'";
    private final String sqlCsqSummary = "select * from RtCSQsSummary";
    private final String url;
    private Cancellable tick;
    private Cancellable tickAgent;
    final private InfluxDB influxDB;
    private final static String db = "MOOVIS";
    private final static String policy = "MOOVIS_POLICY";
    private final PubSubRef<CSQSummary> csqTopic;
    private final PubSubRef<AgentState> agentTopic;


    @Inject
    public AccumulatorActor(Configuration configuration,
                            PubSubRegistry pubSub) {

        this.csqTopic = pubSub.refFor(TopicId.of(CSQSummary.class,"csqs"));
        this.agentTopic = pubSub.refFor(TopicId.of(AgentState.class,"agents"));

        String hostName = configuration.getString("uccx.host");
        String alias = hostName.replace("-","_").toLowerCase().substring(0,hostName.indexOf("."))+"_uccx";
        url = "jdbc:informix-sqli://"
                + hostName + ":" + "1504" + "/" + "db_cra"
                + ":informixserver=" + alias;
        try {
            java.lang.Class.forName("com.informix.jdbc.IfxDriver");

            crsDb = DriverManager.getConnection(url,"uccxhruser",configuration.getString("uccx.uccxhruserpwd"));

            Statement stmt = crsDb.createStatement();

            ResultSet rs = stmt.executeQuery(crsversion);

            while (rs.next()) {
                log.info("UCCX version {}", rs.getString("propertyvalue"));
            }
            rs.close();
            stmt.close();


            Statement stmtCsq = crsDb.createStatement();
            ResultSet rsCsq = stmtCsq.executeQuery(sqlCsq);

            while (rsCsq.next()) {

                csqs.put(rsCsq.getInt("contactservicequeueid"),rsCsq.getString("csqname"));
            }

            log.info("UCCX Csqs {}", csqs.size());

            rsCsq.close();
            stmtCsq.close();


        } catch (Exception e) {
            log.error("{}",e);
        }

        log.info("Connect to InfluxDB {}",configuration.getString("influxdb.url"));
        influxDB = InfluxDBFactory.connect(configuration.getString("influxdb.url"));

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

    }

    @Override
    public void preStart() {
        tick = getContext().system().scheduler().schedule(
                Duration.create(1, TimeUnit.SECONDS),
                Duration.create(5, TimeUnit.SECONDS),
                getSelf(),
                AccumulatorCommand.GetCsq.INSTANCE,
                getContext().system().dispatcher(),
                getSelf());

        tickAgent = getContext().system().scheduler().schedule(
                Duration.create(1, TimeUnit.SECONDS),
                Duration.create(15, TimeUnit.SECONDS),
                getSelf(),
                AccumulatorCommand.GetAgent.INSTANCE,
                getContext().system().dispatcher(),
                getSelf());
    }

    @Override
    public void postStop() {
        try {
            if (crsDb != null && !crsDb.isClosed()) {
                crsDb.close();
            }
            tick.cancel();
            tickAgent.cancel();
        } catch (Exception e) {
            log.error("Closing db connections failed {}",e);
        }
    }

    @Override
    public void onReceive(Object msg) throws Throwable {

        if (msg instanceof AccumulatorCommand.GetAgent) {
            getAgentState();
        } else if (msg instanceof AccumulatorCommand.GetCsq) {
            getSummary();
        } else {
            unhandled(msg);
        }

    }

    private void getSummary() {
        try {
            log.info("Retreive csqs summary.");
            Statement summaryStmt = crsDb.createStatement();
            long time = System.currentTimeMillis();
            ResultSet rsSummary = summaryStmt.executeQuery(sqlCsqSummary);

            while (rsSummary.next()) {
                CSQSummary csqsummary = new CSQSummary(
                        rsSummary.getString("csqname"),
                        rsSummary.getInt("loggedinagents"),
                        rsSummary.getInt("availableagents"),
                        rsSummary.getInt("unavailableagents"),
                        rsSummary.getInt("totalcalls"),
                        rsSummary.getInt("oldestcontact"),
                        rsSummary.getInt("callshandled"),
                        rsSummary.getInt("callsabandoned"),
                        rsSummary.getInt("callsdequeued"),
                        rsSummary.getInt("avgtalkduration"),
                        rsSummary.getInt("avgwaitduration"),
                        rsSummary.getInt("longesttalkduration"),
                        rsSummary.getInt("longestwaitduration"),
                        rsSummary.getInt("callswaiting"),
                        rsSummary.getInt("workingagents"),
                        rsSummary.getInt("talkingagents"),
                        rsSummary.getInt("reservedagents"),
                        rsSummary.getString("convavgtalkduration"),
                        rsSummary.getString("convavgwaitduration"),
                        rsSummary.getString("convlongesttalkduration"),
                        rsSummary.getString("convlongestwaitduration"),
                        rsSummary.getString("convoldestcontact")
                );
                summary.put(rsSummary.getString("csqname"), csqsummary);
                sendMetrics(csqsummary,time);
                csqTopic.publish(csqsummary);
            }
            summaryStmt.close();
            rsSummary.close();
        } catch (Exception e) {
            log.error("Can't get csqs summary {}", e);
        }
    }

    private void getAgentState() {
        try {
            log.info("Retreive agent states.");
            Statement summaryStmt = crsDb.createStatement();
            long time = System.currentTimeMillis();
            ResultSet rsSummary = summaryStmt.executeQuery(agentState);

            while (rsSummary.next()) {
                AgentState state= new AgentState(
                        rsSummary.getString("resourcename"),
                        rsSummary.getInt("eventtype"),
                        rsSummary.getInt("reasoncode"),
                        rsSummary.getString("skillname"),
                        rsSummary.getTimestamp("eventdatetime").toInstant().toEpochMilli()
                );

                sendAgentState(state,time);
                agentTopic.publish(state);
            }
            summaryStmt.close();
            rsSummary.close();
        } catch (Exception e) {
            log.error("Can't get agent state {}", e);
        }
    }

    private void sendMetrics(CSQSummary summary, long time) {

        influxDB.write(Point.measurement("csqsummary")
                .time(time, TimeUnit.MILLISECONDS)
                .tag("csq", summary.getCsqName())

                .addField("loggedinagents", summary.getLoggedInAgents())
                .addField("availableagents", summary.getAvailableAgents())
                .addField("unavailableagents", summary.getUnavailableAgents())
                .addField("totalcalls", summary.getTotalCalls())
                .addField("oldestcontact", summary.getOldestContact())
                .addField("callshandled", summary.getCallsHandled())
                .addField("callsabandoned", summary.getCallsAbandoned())
                .addField("callsdequeued", summary.getCallsDequeued())
                .addField("avgtalkduration", summary.getAvgTalkDuration())
                .addField("avgwaitduration", summary.getAvgWaitDuration())
                .addField("longesttalkduration", summary.getLongestTalkDuration())
                .addField("longestwaitduration", summary.getLongestWaitDuration())
                .addField("callswaiting", summary.getCallsWaiting())
                .addField("workingagents", summary.getWorkingAgents())
                .addField("talkingagents", summary.getTalkingAgents())
                .addField("reservedagents", summary.getReservedAgents())
                .addField("convavgtalkduration", summary.getConvAvgTalkDuration())
                .addField("convavgwaitduration", summary.getConvAvgWaitDuration())
                .addField("convlongesttalkduration", summary.getConvLongestTalkDuration())
                .addField("convlongestwaitduration", summary.getConvLongestWaitDuration())
                .addField("convoldestcontact", summary.getConvOldestContact())
                .build()
        );
    }

    private void sendAgentState(AgentState state, long time) {

        Map<String,String> tags = new HashMap<>();
        tags.put("skill", state.getSkill());
        tags.put("name", state.getAgentName());

        influxDB.write(Point.measurement("agentstate")
                .time(time, TimeUnit.MILLISECONDS)
                .tag(tags)
                .addField("resourcename", state.getAgentName())
                .addField("eventtype", state.getEventType())
                .addField("reasoncode", state.getReasonCode())
                .addField("eventtime", state.getEventtime())
                .build()
        );
    }
}
