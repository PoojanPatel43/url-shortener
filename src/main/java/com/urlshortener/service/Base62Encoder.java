package com.urlshortener.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Base62 encoder for generating short URL codes.
 * Uses characters [0-9][a-z][A-Z] for URL-safe encoding.
 */
@Component
public class Base62Encoder {

    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;
    private final SecureRandom random = new SecureRandom();

    /**
     * Encodes a long number to Base62 string.
     *
     * @param num the number to encode
     * @return Base62 encoded string
     */
    public String encode(long num) {
        if (num == 0) {
            return String.valueOf(BASE62_CHARS.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62_CHARS.charAt((int) (num % BASE)));
            num /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back to a long number.
     *
     * @param str the Base62 string to decode
     * @return decoded long value
     */
    public long decode(String str) {
        long num = 0;
        for (char c : str.toCharArray()) {
            num = num * BASE + BASE62_CHARS.indexOf(c);
        }
        return num;
    }

    /**
     * Generates a random Base62 string of specified length.
     *
     * @param length desired length of the output string
     * @return random Base62 string
     */
    public String generateRandom(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_CHARS.charAt(random.nextInt(BASE)));
        }
        return sb.toString();
    }

    /**
     * Validates if a string contains only Base62 characters.
     *
     * @param str the string to validate
     * @return true if valid Base62 string
     */
    public boolean isValid(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (BASE62_CHARS.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }
}
