package org.example.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordGeneratorTest {
    @Test
    void generatesRequestedLengthAndCharacterTypes() {
        for (int i = 0; i < 50; i++) {
            String password = PasswordGenerator.generate(20, true, true, true, true);

            assertEquals(20, password.length());
            assertTrue(password.chars().anyMatch(c -> Character.isLowerCase(c)));
            assertTrue(password.chars().anyMatch(c -> Character.isUpperCase(c)));
            assertTrue(password.chars().anyMatch(Character::isDigit));
            assertTrue(password.chars().anyMatch(c -> PasswordGenerator.SYMBOLS.indexOf(c) >= 0));
        }
    }

    @Test
    void onlyUsesSelectedCharacterTypes() {
        String password = PasswordGenerator.generate(12, true, false, true, false);

        assertEquals(12, password.length());
        assertTrue(password.chars().allMatch(c ->
                Character.isLowerCase(c) || Character.isDigit(c)));
    }

    @Test
    void rejectsEmptyCharacterSelection() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGenerator.generate(12, false, false, false, false));
    }

    @Test
    void rejectsInvalidLength() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGenerator.generate(2, true, true, true, true));
    }
}
