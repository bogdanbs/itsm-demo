package de.bintintan.itsm.common.interfaces.rest.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public JacksonConfig() {
        this.mapper = new ObjectMapper().findAndRegisterModules() // Registers JavaTimeModule automatically
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // Forces ISO-8601 strings
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}