package com.github.lyd.gateway.provider.configuration;

import com.github.lyd.gateway.provider.endpoint.GatewayRefreshBusEndpoint;
import com.github.lyd.gateway.provider.event.GatewayRefreshRemoteListener;
import com.github.lyd.gateway.provider.filter.ZuulErrorFilter;
import com.github.lyd.gateway.provider.filter.ZuulLogsFilter;
import com.github.lyd.gateway.provider.locator.GrantAccessLocator;
import com.github.lyd.gateway.provider.locator.RateLimitLocator;
import com.github.lyd.gateway.provider.locator.ZuulRouteLocator;
import com.github.lyd.gateway.provider.service.feign.SystemGrantAccessClient;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties;
import com.netflix.zuul.ZuulFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 自定义bus配置类
 * @author: liuyadu
 * @date: 2018/10/23 10:31
 * @description:
 */
@Slf4j
@Configuration
public class BusEndpointConfiguration {
    private RateLimitLocator rateLimitLocator;
    private ZuulRouteLocator zuulRoutesLocator;
    private GrantAccessLocator permissionLocator;
    @Bean
    public GrantAccessLocator permissionLocator(ZuulRouteLocator zuulRoutesLocator, SystemGrantAccessClient systemAccessApi) {
        permissionLocator = new GrantAccessLocator(systemAccessApi, zuulRoutesLocator);
        return permissionLocator;
    }
    @Bean
    public ZuulErrorFilter zuulErrorFilter() {
        return new ZuulErrorFilter();
    }

    @Bean
    public ZuulFilter preRequestLogFilter() {
        return new ZuulLogsFilter();
    }

    /**
     * 初始化限流加载器
     *
     * @return
     */
    @Bean
    public RateLimitLocator rateLimitLocator(RateLimitProperties rateLimitProperties, JdbcTemplate jdbcTemplate) {
        rateLimitLocator = new RateLimitLocator(jdbcTemplate, rateLimitProperties);
        log.debug("注入RateLimitLocator", rateLimitLocator);
        return rateLimitLocator;
    }

    /**
     * 初始化路由加载器
     *
     * @return
     */
    @Bean
    public ZuulRouteLocator zuulRouteLocator(ZuulProperties zuulProperties, ServerProperties serverProperties, JdbcTemplate jdbcTemplate) {
        zuulRoutesLocator = new ZuulRouteLocator(serverProperties.getServlet().getContextPath(), zuulProperties, jdbcTemplate);
        zuulRoutesLocator.setJdbcTemplate(jdbcTemplate);
        log.debug("注入ZuulRoutesLocator", zuulRoutesLocator);
        return zuulRoutesLocator;
    }

    /**
     * 配置网关刷新bus监听
     *
     * @return
     */
    @Bean
    public GatewayRefreshRemoteListener gatewayRefreshRemoteListener() {
        GatewayRefreshRemoteListener rateLimitRefreshRemoteListener = new GatewayRefreshRemoteListener(rateLimitLocator, zuulRoutesLocator,permissionLocator);
        log.debug("注入GatewayRefreshRemoteListener", rateLimitRefreshRemoteListener);
        return rateLimitRefreshRemoteListener;
    }

    /**
     * 配置网关刷新bus端点
     *
     * @param context
     * @param bus
     * @return
     */
    @Bean
    @ConditionalOnEnabledEndpoint
    @ConditionalOnClass({Endpoint.class})
    public GatewayRefreshBusEndpoint gatewayRefreshBusEndpoint(ApplicationContext context, BusProperties bus) {
        GatewayRefreshBusEndpoint endpoint = new GatewayRefreshBusEndpoint(context, bus.getId());
        log.debug("注入GatewayRefreshBusEndpoint", endpoint);
        return endpoint;
    }



}
