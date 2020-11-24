package cn.ninanina.wushan.service.driver;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * 管理selenium的WebDriver，负责各个浏览器之间的负载平衡，并且支持动态增删浏览器，以维持服务可用
 * 负载平衡策略为：采用队列进行负载平衡。取用webdriver的时候出队，用完了入队。如果用的时候发现失效，则不入队。
 */
@Component("driverManager")
@Slf4j
public class DriverManager {
    private BlockingDeque<Pair<String, WebDriver>> webDriverQueue;

    @PostConstruct
    public void init() {
        webDriverQueue = new LinkedBlockingDeque<>();
        System.setProperty("webdriver.chrome.driver", "/opt/WebDriver/bin/chromedriver");
    }

    /**
     * 增加指定机器的浏览器驱动
     *
     * @param ip   机器ip
     * @param port 浏览器接口
     */
    public boolean register(String ip, int port) {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", ip + ":" + port);
        ChromeDriver chromeDriver;
        try {
            chromeDriver = new ChromeDriver(options);
        } catch (Exception e) {
            log.error("register driver failed, error: {}", e.getCause().toString());
            return false;
        }
        webDriverQueue.offer(Pair.of(ip, chromeDriver));
        log.info("new driver registered, ip: {}, port:{}", ip, port);
        return true;
    }

    /**
     * 获取可用的driver，设置5秒超时。
     * 注意！调用之后一定要归还driver，调用restore(driver)
     */
    public Pair<String, WebDriver> access() {
        Pair<String, WebDriver> pair = null;
        try {
            pair = webDriverQueue.poll(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //TODO:发邮件/短信提醒机器资源不够用
            e.printStackTrace();
        }
        return pair;
    }

    public void restore(Pair<String, WebDriver> pair) {
        webDriverQueue.offer(pair);
    }

}
