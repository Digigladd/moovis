package net.busynot.moovis.rtsocket.listener.api;


import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import static com.lightbend.lagom.javadsl.api.Service.pathCall;
import static com.lightbend.lagom.javadsl.api.Service.named;


/*
 * RT_Socket listener service
 * This endpoint is only for debugging purpose.
 * Use A simple websocket client & you will receive everything the listener get from the RT_Socket session
 */
public interface ListenerService extends Service {

    ServiceCall<NotUsed,Source<String,?>> sessionStream();

    @Override
    default Descriptor descriptor() {
        return named("rtsocket-listener").withCalls(
                pathCall("/rtsocket/session",this::sessionStream)
        );
    }

}