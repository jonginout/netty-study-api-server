package com.jonginout.nettyapiserverjongin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.net.InetSocketAddress;

@Configuration
@ComponentScan("com.jonginout.nettyapiserverjongin")
@PropertySource("classpath:api-server.properties")
public class ApiServerConfig {
    @Value("${boss.thread.count}")
    private int bossThreadCount;

    @Value("${worker.thread.count}")
    private int workerThreadCount;

    @Value("${tcp.port}")
    private int tcpPort;

    // ApiServer 부트스트랩에서 사용된다.
    @Bean(name = "bossThreadCount")
    public int getBossThreadCount() {
        return bossThreadCount;
    }

    // ApiServer 부트스트랩에서 사용된다.
    @Bean(name = "workerThreadCount")
    public int getWorkerThreadCount() {
        return workerThreadCount;
    }

    @Bean(name = "tcpSocketAddress")
    public InetSocketAddress tcpPort() {
        return new InetSocketAddress(tcpPort);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
