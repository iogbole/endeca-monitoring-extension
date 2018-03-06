/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.appdynamics.extensions;
/**
 *
 * @author israel.ogbole
 * 04/03/2018
 */

import com.appdynamics.extensions.conf.*;
import com.appdynamics.extensions.PathResolver;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.apache.log4j.Logger;
import org.apache.commons.lang3.StringUtils;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.appdynamics.extensions.yml.YmlReader;

import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.xml.sax.SAXException;

public class EndecaMonitor extends AManagedMonitor {

    public static final Logger logger = Logger.getLogger(EndecaMonitor.class);
    private String hostName;
    private String metricPath;
    private String username;
    private String password;
    private String cache_stats_metrics;
    private String server_stats_metrics;
    private String result_page_stats_metrics;
    private static final String DEFAULT_CONFIG_FILE = "monitors/EndecaMonitor/config.yml";
    private static final String CONFIG_FILE_PARAM = "config-file";
    private static final String DEFAULT_METRIC_PATH = "Custom Metrics|EndecaMDEXStats|";
    protected Configuration config;

    @Override
    public TaskOutput execute(Map<String, String> taskParams,
            TaskExecutionContext context) throws TaskExecutionException {

        try {

            if (taskParams != null) {
                String configFilename = DEFAULT_CONFIG_FILE;
                if (taskParams.containsKey(CONFIG_FILE_PARAM)) {
                    configFilename = taskParams.get(CONFIG_FILE_PARAM);
                }

                File file = PathResolver.getFile(configFilename, Configuration.class);
                config = YmlReader.readFromFile(file, Configuration.class);
                Server server = config.getServer();
                MdexMetrics metricNodes = config.getMdexMetrics();

                if (config == null) {
                    logger.error("Config.yml is null, returning without executing the monitor.");
                    return null;
                }

                if (!Strings.isNullOrEmpty(config.getMetricPrefix())) {
                    metricPath = StringUtils.stripEnd(config.getMetricPrefix(), "|");
                    logger.debug("Metric Path from config:" + metricPath);
                } else {
                    metricPath = DEFAULT_METRIC_PATH;
                }

                if (!Strings.isNullOrEmpty(server.getHost())) {
                    hostName = StringUtils.stripEnd(server.getHost(), "/");
                    logger.debug("HostName  from config :" + hostName);
                } else {
                    logger.error("Endeca Host Name cannot be null in the config.yml file");
                    return null;
                }
                
                if (!Strings.isNullOrEmpty(String.valueOf(server.getPort()))) {
                    logger.debug("port number  from config :" + server.getPort());
                } else {
                    logger.error("Endeca Host Port Number cannot be empty in the config.yml file");
                    return null;
                }
                 
                if (!Strings.isNullOrEmpty(server.getUri())) {
                    logger.debug("Endeca metric URI :" + server.getUri());
                } else {
                    logger.error("/admin?op=stats is expected in uri in config.yml file");
                    return null;
                }

                cache_stats_metrics = taskParams.containsKey("cache_stats_metrics") ? taskParams.get("cache_stats_metrics") : "entry_count,hit_pct,miss_pct";
                server_stats_metrics = taskParams.containsKey("server_stats_metrics") ? taskParams.get("server_stats_metrics") : "avg,max,min,total";
                result_page_stats_metrics = taskParams.containsKey("server_stats_metrics") ? taskParams.get("result_page_stats") : "avg,max,min,total";

                String opsStatUrl = server.getHost() + ":" + server.getPort() + server.getUri();
                logger.debug("OpsStatUrl " + opsStatUrl);

                URL mdexHost = new URL(opsStatUrl);

                logger.trace("Read the opsStatUrl " + mdexHost.toString());
                logger.trace("metricpath " + metricPath);
                logger.trace("cache_stats_metrics " + cache_stats_metrics);
                logger.trace("server_stats_metrics " + server_stats_metrics);
                logger.trace("result_page_stats_metrics " + result_page_stats_metrics);

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = docFactory.newDocumentBuilder();
                Document xmldoc = builder.parse(mdexHost.openStream());

                XPath xPath = XPathFactory.newInstance().newXPath();

                if (!Strings.isNullOrEmpty(metricNodes.getCacheMetrics())) {
                    logger.debug("Collecting cache metrics " + metricNodes.getCacheMetrics());

                    metricsCollector((NodeList) xPath.evaluate("//cache_stats", xmldoc.getDocumentElement(), XPathConstants.NODESET),
                            metricNodes.getCacheMetrics(), "Cache");
                }

                if (!Strings.isNullOrEmpty(metricNodes.getServerMetrics())) {
                    logger.debug("Collectoing server metrics" + metricNodes.getServerMetrics());

                    metricsCollector((NodeList) xPath.evaluate("//server_stats/stat", xmldoc.getDocumentElement(), XPathConstants.NODESET),
                            metricNodes.getServerMetrics(), "Server");
                }

                if (!Strings.isNullOrEmpty(metricNodes.getResultPageMetrics())) {
                    logger.debug("collecting result page metrics " + metricNodes.getResultPageMetrics());

                    metricsCollector((NodeList) xPath.evaluate("//result_page_stats/stat", xmldoc.getDocumentElement(), XPathConstants.NODESET),
                            metricNodes.getResultPageMetrics(), "Result Page");
                }

            } else {
                logger.error("At lease one task param MUST exist in the the monitor.xml file");
                return null;
            }

        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException e) {

            logger.error("Error occured: " + e.toString());
            logger.error(Arrays.toString(e.getStackTrace()));

        }
        return null;
    }

    //Loop through the list of metrics and send the ones that matched the list
    private void metricsCollector(NodeList metrics, String list, String type) {
        try {
            for (int i = 0; i < metrics.getLength(); i++) {
                Attr metric = (Attr) metrics.item(i).getAttributes().getNamedItem("name");
                if (list.toLowerCase().equals("*")
                        || (list.toLowerCase().contains(metric.getValue().toLowerCase()))) {
                    //Got a match, get all the attributes 
                    for (int x = 0; x < metrics.item(i).getAttributes().getLength(); x++) {
                        Attr attribute = (Attr) metrics.item(i).getAttributes().item(x);
                        if (server_stats_metrics.contains(attribute.getName()) || cache_stats_metrics.contains(attribute.getName()) || result_page_stats_metrics.contains(attribute.getName())) {
                            printMetric(type + "|" + metric.getValue().split("\\.")[0] + "|" + attribute.getName(),
                                    appDmetrifier(attribute.getValue()),
                                    MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error occured in getMetrics: " + e.toString());
            logger.error(Arrays.toString(e.getStackTrace()));

        }
    }

    protected int appDmetrifier(String metric) {
        if (metric == null || "".equals(metric)) {
            metric = "0";
        }
        //Endeca returns nan and n/a in metric values, check if IsNumber using nfe.  
        double metricDoubleValue;
        try {
            metricDoubleValue = Double.parseDouble(metric);
        } catch (NumberFormatException nfe) {
            return 0;
        }
        int metricIntValue = (int) Math.round(metricDoubleValue);
        return metricIntValue;
    }

    protected void printMetric(String name, int value, String aggType, String timeRollup, String clusterRollup) {
        String metricName = metricPath + "|" + name;
        MetricWriter metricWriter = getMetricWriter(metricName, aggType, timeRollup, clusterRollup);
        metricWriter.printMetric(String.valueOf(value));
        logger.debug("METRIC:  NAME:" + metricName + " VALUE:" + value + " :" + aggType + ":" + timeRollup + ":" + clusterRollup);
    }

    public static void main(String[] argv)
            throws Exception {
        Map<String, String> taskParams = new HashMap<>();
        TaskExecutionContext context = null;
        taskParams.put("url", "http://localhost:4104/endeca-calls.xml");
        //Not in use as RS's Endeca servers does not require authentication to view stats
        taskParams.put("username", "root");
        taskParams.put("password", "PASSWORD");
        //taskParams.put("cache_stats", "Totals,CacheKey_RangePredicateFilter");
        //taskParams.put("server_stats", "HTTP: Total request time,HTTP: Time reading request");
        taskParams.put("cache_stats", "*");
        taskParams.put("server_stats", "*");
        taskParams.put("result_page_stats", "*");
        taskParams.put("cache_stats_metrics", "entry_count,hit_pct,miss_pct");
        taskParams.put("server_stats_metrics", "avg,max,min,total,n");
        taskParams.put("result_page_stats_metrics", "avg,max,min,total,n");
        taskParams.put("metric_path", "Custom Metrics|MDEXStats|");
        taskParams.put("config-file", "/Users/israel.ogbole/appDynamics/extensions/endeca-monitoring-extension/src/main/resources/conf/config.yml");

        new EndecaMonitor().execute(taskParams, context);
    }

}
