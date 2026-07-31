package yzh.nas.business.config;

import org.apache.catalina.Context;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebDavConfig implements WebMvcConfigurer {

    @Value("${photo.storage.base-path:D:\\test\\photos}")
    private String storageBasePath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/webdav/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "PUT", "DELETE", "POST", "HEAD", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("DAV", "Allow", "Location", "Content-Length");
    }

    /**
     * 配置 Tomcat 支持 WebDAV 自定义 HTTP 方法
     */
    @Bean
    public TomcatContextCustomizer tomcatContextCustomizer() {
        return (Context context) -> {
            // 允许所有 HTTP 方法
            context.setAllowCasualMultipartParsing(true);
        };
    }
}
