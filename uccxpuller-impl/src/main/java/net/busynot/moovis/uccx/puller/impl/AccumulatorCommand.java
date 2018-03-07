package net.busynot.moovis.uccx.puller.impl;


public interface AccumulatorCommand {

    enum GetCsq implements AccumulatorCommand {
        INSTANCE;
    }

    enum GetAgent implements AccumulatorCommand {
        INSTANCE;
    }


}
