package net.busynot.moovis.eptica.puller.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.serialization.Jsonable;
import com.typesafe.config.Config;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;

@JsonDeserialize
@Wither
@Value
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public final class EpticaSupervision implements Jsonable {

    String group;
    EpticaRequests requests;
    EpticaMail mail;
    EpticaAccount accounts;
    EpticaAccount administrators;
    EpticaAccount supervisors;
    EpticaAgent agents;

    public static EpticaSupervision fromConfig(String group,Config sup) {
        EpticaSupervision es = new EpticaSupervision(group,null,null,null,null,null,null);
        try {
            es = es.withRequests(
                    new EpticaRequests(
                            sup.getInt("requests.received.all"),
                            sup.getInt("requests.waiting.all"),
                            sup.getInt("requests.postponed.all"),
                            sup.getInt("requests.incall.all"),
                            sup.getInt("requests.review.all"),
                            sup.getInt("requests.invalid.all"),
                            sup.getInt("requests.pool"),
                            sup.getInt("requests.reserve"))
            )
                    .withMail(
                            new EpticaMail(
                                    sup.getInt("mail.waiting"),
                                    sup.getInt("mail.parsed"),
                                    sup.getInt("mail.outgoing")
                            )
                    )
                    .withAccounts(
                            new EpticaAccount(
                                    sup.getInt("accounts.all"),
                                    sup.getInt("accounts.enabled"),
                                    sup.getInt("accounts.connected")
                            )
                    )
                    .withAdministrators(
                            new EpticaAccount(
                                    sup.getInt("administrators.all"),
                                    sup.getInt("administrators.enabled"),
                                    sup.getInt("administrators.connected")
                            )
                    )
                    .withSupervisors(
                            new EpticaAccount(
                                    sup.getInt("supervisors.all"),
                                    sup.getInt("supervisors.enabled"),
                                    sup.getInt("supervisors.connected")
                            )
                    )
                    .withAgents(
                            new EpticaAgent(
                                    sup.getInt("agents.all"),
                                    sup.getInt("agents.enabled"),
                                    sup.getInt("agents.email.all"),
                                    sup.getInt("agents.email.enabled"),
                                    sup.getInt("agents.auto.all"),
                                    sup.getInt("agents.auto.enabled"),
                                    sup.getInt("agents.network.all"),
                                    sup.getInt("agents.network.enabled"),
                                    sup.getInt("agents.gui.all"),
                                    sup.getInt("agents.gui.enabled"),
                                    sup.getInt("agents.gui.online"),
                                    sup.getInt("agents.gui.offline"),
                                    sup.getInt("agents.gui.waiting"),
                                    sup.getInt("agents.gui.manual"),
                                    sup.getInt("agents.gui.paused"),
                                    sup.getInt("agents.gui.busy"),
                                    sup.getInt("agents.gui.incall")
                            )
                    );
        } catch (Exception e) {

        }
        return es;

    }
}
