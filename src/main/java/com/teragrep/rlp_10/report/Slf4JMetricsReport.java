package com.teragrep.rlp_10.report;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.teragrep.rlp_10.config.MetricsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Slf4JMetricsReport implements MetricsReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(Slf4JMetricsReport.class);
    private final MetricsConfiguration metricsConfiguration;
    private final Slf4jReporter slf4jReport;

    public Slf4JMetricsReport(final MetricRegistry metricRegistry){
        this(metricRegistry, new MetricsConfiguration());
    }

    public Slf4JMetricsReport(final MetricRegistry metricRegistry, final MetricsConfiguration metricsConfiguration){
        this.metricsConfiguration = metricsConfiguration;
        this.slf4jReport = Slf4jReporter.forRegistry(metricRegistry).outputTo(LOGGER).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
    }

    @Override
    public void start(){
        slf4jReport.start(metricsConfiguration.interval(), TimeUnit.SECONDS);
    }

    @Override
    public void stop(){
        slf4jReport.stop();
    }
}
