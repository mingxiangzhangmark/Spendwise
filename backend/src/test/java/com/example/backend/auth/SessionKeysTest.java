package com.example.backend.auth;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class SessionKeysTest {

    @Test
    void constant_USER_DTO_isExpected() {
        assertEquals("USER", SessionKeys.USER_DTO);
    }

    @Test
    void class_isFinal_and_hasPrivateCtor() throws Exception {
        assertTrue(Modifier.isFinal(SessionKeys.class.getModifiers()),
                "SessionKeys should be final");

        Constructor<?>[] ctors = SessionKeys.class.getDeclaredConstructors();
        assertEquals(1, ctors.length, "Should have exactly one constructor");
        Constructor<?> ctor = ctors[0];
        assertTrue(Modifier.isPrivate(ctor.getModifiers()), "Constructor should be private");

        ctor.setAccessible(true);
        Object instance = ctor.newInstance();
        assertNotNull(instance);
    }
}
