package com.example.backend.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebConfigTest {

    @Test
    void addResourceHandlers_registersPictureMapping() {
        WebConfig config = new WebConfig();

        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);

        when(registry.addResourceHandler("/picture/**")).thenReturn(registration);
        when(registration.addResourceLocations((String) any())).thenReturn(registration);

        config.addResourceHandlers(registry);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(registration).addResourceLocations(captor.capture());
        String loc = captor.getValue();

        String expectedPrefix = "file:" + System.getProperty("user.dir") + "/uploads/picture/";
        assertTrue(loc.startsWith(expectedPrefix),
                () -> "resource location should start with " + expectedPrefix + " but was " + loc);
    }
}
