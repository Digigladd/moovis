package net.busynot.moovis.rtsocket.listener.impl;

import akka.persistence.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class AccumulatorActor extends UntypedPersistentActor  {

    private static final Logger log = LoggerFactory.getLogger(AccumulatorActor.class);

    LoadingCache<String, TVIData> state = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<String, TVIData>() {
                        public TVIData load(String key) throws Exception {
                            return TVIData.create(key);
                        }
                    });

    @Override
    public String persistenceId() { return "tviaccumulator"; }

    @Override
    public Recovery recovery() {
        return Recovery.create(SnapshotSelectionCriteria.latest());
    }


    @Override
    public void onReceiveRecover(Object msg) throws Throwable {
        if (msg instanceof RecoveryCompleted) {
            log.info("Accumulator actor recovery completed!");
            if (state == null) {
                log.info("No eligible snapshot for recover, creating new state!");
                state.invalidateAll();
                deleteSnapshots(SnapshotSelectionCriteria.latest());
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextMidnight = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT);

            log.info("Scheduling reset, next {}, every 24h", nextMidnight);

            getContext().system().scheduler().schedule(scala.concurrent.duration.Duration.create(Duration.between(now,nextMidnight).getSeconds(), TimeUnit.SECONDS),
                    scala.concurrent.duration.Duration.create(1,TimeUnit.DAYS),
                    getSelf(),
                    AccumulatorCommand.ResetAccumulator.INSTANCE,
                    getContext().system().dispatcher(),
                    null);
        } else if (msg instanceof SnapshotOffer) {
            SnapshotOffer so = (SnapshotOffer)msg;
            log.info("Accumulator actor received snapshot offer: {}", so.snapshot());
            //refuse this snapshot if it's too old
            if (isFresh(so.metadata().timestamp())) {
                List<TVIData> entries = (List)so.snapshot();
                entries.forEach(
                        entry -> state.put(entry.getSplit(),entry)
                );
            }
        }


    }

    @Override
    public void onReceiveCommand(Object msg) throws Throwable {
        if(msg instanceof AccumulatorCommand.UpdateAcdCalls) {

            AccumulatorCommand.UpdateAcdCalls uAcdCalls = (AccumulatorCommand.UpdateAcdCalls)msg;
            TVIData split = state.get(uAcdCalls.getSplit()).updateAcdCalls(uAcdCalls.getValue());
            state.put(uAcdCalls.getSplit(),split);
            getSender().tell(split.callReceived(),getSelf());

        } else if (msg instanceof AccumulatorCommand.UpdateAbnCalls) {
            AccumulatorCommand.UpdateAbnCalls uAbnCalls = (AccumulatorCommand.UpdateAbnCalls)msg;

            TVIData split = state.get(uAbnCalls.getSplit()).updateAbnCalls(uAbnCalls.getValue());
            state.put(uAbnCalls.getSplit(),split);
            getSender().tell(split.callAbandoned(),getSelf());

        } else if (msg instanceof AccumulatorCommand.UpdateIntervalData) {
            AccumulatorCommand.UpdateIntervalData datas = (AccumulatorCommand.UpdateIntervalData)msg;

            TVIData split = state.get(datas.getSplit()).updateAbnCalls(datas.getAbnCalls()).updateAcdCalls(datas.getAcdCalls()).updateOfferedCalls(datas.getOfferedCalls());
            state.put(datas.getSplit(),split);

            getSender().tell(split,getSelf());
        } else if (msg instanceof SaveSnapshotSuccess) {
            SaveSnapshotSuccess success = (SaveSnapshotSuccess)msg;
            log.debug("Accumulator state saved {}", success.metadata());
            deleteSnapshots(SnapshotSelectionCriteria.create(success.metadata().sequenceNr()-1,success.metadata().timestamp()-1));
        } else if (msg instanceof DeleteSnapshotsSuccess) {
            log.debug("Accumulator snapshots successfully cleanup {}",msg);
        } else if(msg instanceof AccumulatorCommand.ResetAccumulator) {
            log.debug("Time to reset state!");
            state.invalidateAll();
            snapshot();
        } else if (msg instanceof AccumulatorCommand.Snapshot) {

            snapshot();

        } else {
            log.debug("Accumulator actor received {}",msg);
            unhandled(msg);
        }


    }

    private void snapshot() {
        List<TVIData> datas = new ArrayList<>();
        state.asMap().entrySet().forEach(entry -> datas.add(entry.getValue()));
        saveSnapshot(datas);
    }

    private boolean isFresh(long timestamp) {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime snapshotDay = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
        return today.getYear() == snapshotDay.getYear() && today.getMonth() == snapshotDay.getMonth() && today.getDayOfMonth() == snapshotDay.getDayOfMonth();
    }
}
