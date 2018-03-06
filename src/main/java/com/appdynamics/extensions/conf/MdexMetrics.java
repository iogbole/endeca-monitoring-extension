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
public class MdexMetrics {

    public String getCacheMetrics() {
        return cacheMetrics;
    }

    public void setCacheMetrics(String cacheMetrics) {
        this.cacheMetrics = cacheMetrics;
    }

    public String getServerMetrics() {
        return serverMetrics;
    }

    public void setServerMetrics(String serverMetrics) {
        this.serverMetrics = serverMetrics;
    }

    public String getResultPageMetrics() {
        return resultPageMetrics;
    }

    public void setResultPageMetrics(String resultPageMetrics) {
        this.resultPageMetrics = resultPageMetrics;
    }
    
    private String cacheMetrics;
    private String serverMetrics;
    private String resultPageMetrics;
    
}
