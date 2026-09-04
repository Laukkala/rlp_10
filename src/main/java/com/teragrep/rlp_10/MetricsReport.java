package com.teragrep.rlp_10;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
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

import java.util.concurrent.TimeUnit;

public class MetricsReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsReport.class);
    private final MetricRegistry metricRegistry;
    private final MetricsConfiguration metricsConfiguration;
    private final PrometheusConfiguration prometheusConfiguration;
    private final Slf4jReporter slf4jReport;
    private final Server prometheusReport;

    public MetricsReport(final MetricRegistry metricRegistry){
        this(metricRegistry, new MetricsConfiguration(), new PrometheusConfiguration());
    }

    public MetricsReport(final MetricRegistry metricRegistry, final MetricsConfiguration metricsConfiguration, final PrometheusConfiguration prometheusConfiguration){
        this.metricRegistry = metricRegistry;
        this.metricsConfiguration = metricsConfiguration;
        this.prometheusConfiguration = prometheusConfiguration;
        this.slf4jReport = Slf4jReporter.forRegistry(metricRegistry).outputTo(LOGGER).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
        this.prometheusReport = new Server(prometheusConfiguration.port());
    }

    public void start(){
        slf4jReport.start(metricsConfiguration.interval(), TimeUnit.SECONDS);
        CollectorRegistry.defaultRegistry.register(new DropwizardExports(metricRegistry));

        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        prometheusReport.setHandler(context);

        final MetricsServlet metricsServlet = new MetricsServlet();
        final ServletHolder servletHolder = new ServletHolder(metricsServlet);
        context.addServlet(servletHolder, "/metrics");
        try{
            prometheusReport.start();
        }catch (final Exception e){
            LOGGER.error("Failed to start Prometheus reporting server!");
            slf4jReport.close();
        }
    }

    public void stop(){
        slf4jReport.stop();
        try{
            prometheusReport.stop();
        }
        catch (final Exception e){
            LOGGER.error("Failed to stop Prometheus reporting server!");
        }
    }
}
