FROM digigladd/grafana:master

RUN mkdir /dashboards

COPY provisioning/datasources/moovis.yaml /etc/grafana/provisioning/datasources/moovis.yaml
COPY dashboards/*.json /dashboards/
COPY provisioning/dashboards/moovis.yaml /etc/grafana/provisioning/dashboards/moovis.yaml

VOLUME ["/var/lib/grafana", "/var/log/grafana", "/etc/grafana"]

EXPOSE 3000

ENV GF_AUTH_ANONYMOUS_ENABLED true
ENV GF_USERS_DEFAULT_THEME light
ENV GF_INSTALL_PLUGINS grafana-clock-panel

COPY ./run.sh /run.sh

ENTRYPOINT ["/run.sh"]
