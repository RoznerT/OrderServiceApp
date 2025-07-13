dependencies {
    implementation(project(":common"))
    
    // Swagger Core for OpenAPI 3.0 annotations (for code documentation)
    implementation("io.swagger.core.v3:swagger-annotations:2.2.16")
    implementation("io.swagger.core.v3:swagger-core:2.2.16")
    implementation("io.swagger.core.v3:swagger-models:2.2.16")
    
    // Jackson for JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    
    // YAML support for OpenAPI generation
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
} 