package com.bank.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 모든 서비스 공통 OpenAPI(Swagger) 설정.
 * spring.application.name 을 읽어 서비스별 제목을 자동 구성한다.
 *
 * 의존성은 각 서비스 build.gradle 에서 implementation 으로 가져온다.
 * common 모듈 자체는 compileOnly 라 미사용 시 런타임 비용 없음.
 */
@Configuration
@ConditionalOnClass(OpenAPI.class)
public class SwaggerConfig {

    @Value("${spring.application.name:internet-banking}")
    private String applicationName;

    @Value("${app.version:0.0.1-SNAPSHOT}")
    private String applicationVersion;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Internet Banking - " + applicationName)
                        .description(applicationName + " REST API 문서. /swagger-ui.html 에서 직접 호출 테스트 가능.")
                        .version(applicationVersion)
                        .contact(new Contact().name("Internet Banking MVP"))
                        .license(new License().name("Internal Use Only")));
    }
}
