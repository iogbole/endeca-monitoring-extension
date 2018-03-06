/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.appdynamics.extensions.conf;

/**
 *
 * @author israel.ogbole
 * 06/03/2018
 */
public class Configuration {
    
    private Server server;
    private MdexMetrics mdexMetrics;

    public MdexMetrics getMdexMetrics() {
        return mdexMetrics;
    }

    public void setMdexMetrics(MdexMetrics mdexMetrics) {
        this.mdexMetrics = mdexMetrics;
    }

    
    private String metricPrefix;
   

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    
    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }
   
}
