package com.team01.freelance.common.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/** Type metadata belongs to Redis values, never to HTTP or event payloads. */
public final class TypedCacheMapper {
    private TypedCacheMapper() { }

    public static ObjectMapper copyOf(ObjectMapper mapper) {
        ObjectMapper cacheMapper = mapper.copy();
        // API read-only fields (notably User.id) must still round-trip through
        // trusted cache values. This copy is never used to parse HTTP input.
        cacheMapper.setAnnotationIntrospector(new com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector() {
            @Override
            public com.fasterxml.jackson.annotation.JsonProperty.Access findPropertyAccess(
                    com.fasterxml.jackson.databind.introspect.Annotated member) {
                var access = super.findPropertyAccess(member);
                return access == com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY
                        ? com.fasterxml.jackson.annotation.JsonProperty.Access.READ_WRITE : access;
            }
        });
        return cacheMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.team01.freelance.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.time.")
                        .allowIfSubType(Number.class)
                        .allowIfSubTypeIsArray()
                        .build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY);
    }
}
