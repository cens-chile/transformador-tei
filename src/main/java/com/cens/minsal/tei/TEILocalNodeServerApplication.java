package com.cens.minsal.tei;


import ca.uhn.fhir.rest.server.RestfulServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;


import java.io.IOException;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.context.annotation.Conditional;

//@SpringBootApplication
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,ElasticsearchRestClientAutoConfiguration.class, ThymeleafAutoConfiguration.class})
public class TEILocalNodeServerApplication {

    private final Logger log = LoggerFactory.getLogger(TEILocalNodeServerApplication.class);
    @Autowired
    AutowireCapableBeanFactory beanFactory;
    private static ConfigurableApplicationContext context;
    

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        context = SpringApplication.run(TEILocalNodeServerApplication.class, args);
    }
        

    @Bean
    public ServletRegistrationBean hapiServletRegistration(RestfulServer restfulServer) {
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
        beanFactory.autowireBean(restfulServer);
        servletRegistrationBean.setServlet(restfulServer);
        servletRegistrationBean.addUrlMappings("/fhir/*");
        servletRegistrationBean.setLoadOnStartup(1);

        return servletRegistrationBean;
    }

    
    public static void restart() {
        ApplicationArguments args = context.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            context.close();
            context = SpringApplication.run(TEILocalNodeServerApplication.class, args.getSourceArgs());
        });

        thread.setDaemon(false);
        thread.start();
    }
}
