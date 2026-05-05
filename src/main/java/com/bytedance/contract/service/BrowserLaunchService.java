package com.bytedance.contract.service;

import java.awt.Desktop;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BrowserLaunchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserLaunchService.class);

    private final ServletWebServerApplicationContext applicationContext;

    public BrowserLaunchService(ServletWebServerApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowserAfterStartup() {
        if (!Desktop.isDesktopSupported()) {
            LOGGER.info("当前环境不支持桌面浏览器自动打开，跳过。");
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            LOGGER.info("当前环境不支持浏览器打开动作，跳过。");
            return;
        }

        int port = applicationContext.getWebServer().getPort();
        String url = "http://localhost:" + port + "/";
        try {
            desktop.browse(URI.create(url));
            LOGGER.info("已自动打开浏览器：{}", url);
        } catch (Exception exception) {
            LOGGER.warn("自动打开浏览器失败，请手动访问：{}", url, exception);
        }
    }
}
