package com.jobportal.v1.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:" + serverPort);
        devServer.setDescription("Development Server");

        Contact contact = new Contact();
        contact.setEmail("admin@jobportal.com");
        contact.setName("Job Portal Team");
        contact.setUrl("https://jobportal.com");

        License mitLicense = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Job Portal API v1")
                .version("1.0")
                .contact(contact)
                .description("""
                    This API exposes endpoints for Job Portal application with role-based access control.
                    
                    ## Authentication Flow
                    
                    1. **Register**: `POST /api/auth/signup` - Create new account
                    2. **Verify Email**: `POST /api/auth/verify-signup` - Verify OTP sent to email
                    3. **Login**: Use role-specific endpoints:
                       - `POST /api/auth/admin/login` - For admin users
                       - `POST /api/auth/agency/login` - For agency users
                    4. **Use Token**: After login, click the **Authorize** button above and enter your JWT token
                    5. **Refresh Token**: `POST /api/auth/refresh-token` - Get new access token
                    6. **Logout**: `POST /api/auth/logout` - Invalidate refresh token
                    
                    ## Roles & Permissions
                    
                    | Role | Access |
                    |------|--------|
                    | **ADMIN** | Full system access, can access `/api/admin/**` endpoints |
                    | **AGENCY** | Agency-specific access, can access `/api/agency/**` endpoints |
                    
                    ## Important Notes
                    
                    - All protected endpoints require Bearer token authentication
                    - Tokens expire after configured time (default: 1 day)
                    - Refresh tokens can be used to get new access tokens
                    - Email verification is required before login
                    """)
                .license(mitLicense);

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("Enter your JWT token: Bearer <token>");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer))
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }
}