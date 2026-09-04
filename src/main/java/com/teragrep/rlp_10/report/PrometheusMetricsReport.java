package com.teragrep.rlp_10.report;

import com.codahale.metrics.MetricRegistry;
import com.teragrep.rlp_10.config.MetricsConfiguration;
import com.teragrep.rlp_10.config.PrometheusConfiguration;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrometheusMetricsReport implements MetricsReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusMetricsReport.class);
    private final MetricRegistry metricRegistry;
    private final Server prometheusMetricsServer;

    public PrometheusMetricsReport(final MetricRegistry metricRegistry){
        this(metricRegistry, new MetricsConfiguration(), new PrometheusConfiguration());
    }

    public PrometheusMetricsReport(final MetricRegistry metricRegistry, final MetricsConfiguration metricsConfiguration, final PrometheusConfiguration prometheusConfiguration){
        this.metricRegistry = metricRegistry;
        this.prometheusMetricsServer = new Server(prometheusConfiguration.port());
    }

    @Override
    public void start(){
        CollectorRegistry.defaultRegistry.register(new DropwizardExports(metricRegistry));

        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        prometheusMetricsServer.setHandler(context);

        final MetricsServlet metricsServlet = new MetricsServlet();
        final ServletHolder servletHolder = new ServletHolder(metricsServlet);
        context.addServlet(servletHolder, "/metrics");
        try{
            prometheusMetricsServer.start();
        }catch (final Exception e){
            LOGGER.error("Failed to start Prometheus reporting server!");
        }
    }

    @Override
    public void stop(){
        try{
            prometheusMetricsServer.stop();
        }
        catch (final Exception e){
            LOGGER.error("Failed to stop Prometheus reporting server!");
        }
    }
}
