package nl.mnsw.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Web MVC configuratie voor MNSW.
 * CORS: Angular dev server op localhost:4200 mag alle API-endpoints benaderen.
 * In productie worden allowedOrigins ingesteld via omgevingsvariabelen.
 */
@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:4200") // Angular dev server
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(false)
    }
}
