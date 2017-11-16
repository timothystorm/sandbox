package org.storm.periscope.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class UserTest {
    @Test(expected = IllegalArgumentException.class)
    public void test_null_username() throws Exception {
        new User(null, "secret", true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_null_password() throws Exception {
        new User("user1", null, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_empty_username() throws Exception {
        new User("", "secret", true);
    }

    @Test
    public void test_equals() throws Exception {
        User u1 = new User("user1", "secret", true);
        assertEquals("same usernames should be equal", u1, u1);
        assertEquals("same username different password should be equal", u1, u1.setPassword("supersecret"));
        assertNotEquals("different usernames should NOT be equal", u1, u1.setUsername("user2"));
    }
}