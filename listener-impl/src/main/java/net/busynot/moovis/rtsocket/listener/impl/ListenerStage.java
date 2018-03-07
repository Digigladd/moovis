package net.busynot.moovis.rtsocket.listener.impl;

import akka.Done;
import akka.actor.ActorRef;
import akka.stream.Attributes;
import akka.stream.FlowShape;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import akka.util.ByteString;
import akka.util.Timeout;
import com.lightbend.lagom.javadsl.pubsub.PubSubRef;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ListenerStage extends GraphStage<FlowShape<String, ByteString>> {

    public Inlet<String> in = Inlet.<String>create("DigestCalculator.in");
    public Outlet<ByteString> out = Outlet.<ByteString>create("DigestCalculator.out");
    private FlowShape<String, ByteString> shape = FlowShape.of(in, out);
    private final static Logger log = LoggerFactory.getLogger(ListenerStage.class);
    private final PubSubRef<String> topic;
    private final InfluxDB influxDB;
    private Random rand = new Random();
    private final ActorRef accumulator;

    public ListenerStage(PubSubRef<String> topic,
                         InfluxDB influxDB,
                         ActorRef accumulator) {
        this.topic = topic;
        this.influxDB = influxDB;
        this.accumulator = accumulator;
    }

    @Override
    public FlowShape<String, ByteString> shape() {
        return shape;
    }

    @Override
    public GraphStageLogic createLogic(Attributes inheritedAttributes) {
        return new GraphStageLogic(shape) {

            {

                setHandler(out, new AbstractOutHandler() {
                    @Override
                    public void onPull() throws Exception {
                        pull(in);
                    }
                });
                setHandler(in, new AbstractInHandler() {
                    @Override
                    public void onPush() throws Exception {
                        String chunk = grab(in);
                        //log.info("Received {}", chunk);
                        topic.publish(chunk);
                        writePoint(chunk);
                        pull(in);
                    }

                    @Override
                    public void onUpstreamFinish() throws Exception {
                        completeStage();
                    }
                });
            }


        };
    }

    private void writePoint(String line) {
        if (line.startsWith("F1")) {
            try {

                String[] splitted = line.split("\\|");

                AccumulatorCommand.UpdateIntervalData updateCommand = new AccumulatorCommand.UpdateIntervalData(
                        splitted[1],
                        IntervalData.create().withValue(toInteger(splitted[8].trim())),
                        IntervalData.create().withValue(toInteger(splitted[5].trim())),
                        IntervalData.create().withValue(toInteger(splitted[23].trim()))
                );

                akka.pattern.PatternsCS.ask(this.accumulator, updateCommand, Timeout.apply(FiniteDuration.apply(1000, TimeUnit.SECONDS))).thenAccept(
                        tviData -> {
                            try {

                                influxDB.write(
                                        Point.measurement("tvi")
                                                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                                                .tag("SPLIT", splitted[1])
                                                .addField("INQUEUE", toInteger(splitted[2].trim()))
                                                .addField("AVAILABLE", toInteger(splitted[3].trim()))
                                                .addField("ANSTIME", toSecond(splitted[4]))
                                                .addField("ABNCALLS", ((TVIData)tviData).callAbandoned())
                                                .addField("ACD", toInteger(splitted[6].trim()))
                                                .addField("OLDESTCALL", toSecond(splitted[7]))
                                                .addField("ACDCALLS", ((TVIData)tviData).callReceived())
                                                .addField("ACDTIME", toSecond(splitted[9]))
                                                .addField("ABNTIME", toSecond(splitted[10]))
                                                .addField("AGINRING", toInteger(splitted[11].trim()))
                                                .addField("ONACD", toInteger(splitted[12].trim()))
                                                .addField("INACW", toInteger(splitted[13].trim()))
                                                .addField("OTHER", toInteger(splitted[14].trim()))
                                                .addField("INAUX", toInteger(splitted[15].trim()))
                                                .addField("STAFFED", toInteger(splitted[16].trim()))
                                                .addField("EWTHIGH", toSecond(splitted[17]))
                                                .addField("EWTMEDIUM", toSecond(splitted[18]))
                                                .addField("EWTLOW", toSecond(splitted[19]))
                                                .addField("DA_INQUEUE", toInteger(splitted[20].trim()))
                                                .addField("ACCEPTABLE", toInteger(splitted[21].trim()))
                                                .addField("SERVICELEVEL", toInteger(splitted[22].trim()))
                                                .addField("CALLSOFFERED", ((TVIData)tviData).callOffered())
                                                .build()
                                );
                            } catch (Exception e) {
                                log.error("Unable to write point {}: {}",line,e);
                            }
                        }
                );


            } catch (Exception e) {
                log.error("Unable to read input {}: {}",line,e);
            }
        } else if (line.startsWith("F3")) {
            this.accumulator.tell(AccumulatorCommand.Snapshot.INSTANCE,null);
        }
    }

    private Integer toInteger(String value) {
        Integer cpt = 0;
        if (!value.isEmpty()) {
            cpt = Integer.valueOf(value);
        }
        return cpt;
    }

    private Integer toSecond(String value) {
        Integer time = 0;
        if (value.contains(":")) {
            String[] splitted = value.trim().split(":");
            if (splitted.length > 0) {
                if (splitted.length > 1) {
                    time = toInteger(splitted[0]) * 60 + toInteger(splitted[1]);
                } else {
                    time = toInteger(splitted[0]);
                }
            }
        } else {
            time = value.equalsIgnoreCase("*****") ? 100 * 60 : 0;
        }
        return time;
    }
}
