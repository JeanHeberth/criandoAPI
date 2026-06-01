package br.com.criandoapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@Configuration
public class OpenApiConfig {

	private static final String SECURITY_SCHEME_NAME = "bearerAuth";

	private static final Logger log = LoggerFactory.getLogger(OpenApiConfig.class);

	private final Environment environment;

	public OpenApiConfig(Environment environment) {
		this.environment = environment;
	}

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Criando API")
						.version("v1")
						.description("Documentacao Swagger da API com autenticacao JWT"))
				.components(new Components()
						.addSecuritySchemes(SECURITY_SCHEME_NAME,
								new SecurityScheme()
										.name(SECURITY_SCHEME_NAME)
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
	}

	@EventListener(ApplicationReadyEvent.class)
	public void logSwaggerUrl() {
		String port = environment.getProperty("local.server.port",
				environment.getProperty("server.port", "8080"));
		String contextPath = normalizePath(environment.getProperty("server.servlet.context-path", ""));
		String swaggerPath = normalizePath(environment.getProperty("springdoc.swagger-ui.path", "/swagger-ui.html"));

		log.info("Swagger disponível em: http://localhost:{}{}{}", port, contextPath, swaggerPath);
	}

	private String normalizePath(String path) {
		if (path == null || path.isBlank() || "/".equals(path)) {
			return "";
		}

		return path.startsWith("/") ? path : "/" + path;
	}
}

