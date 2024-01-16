package controller;

import model.User;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import db.Database;
import util.HttpRequest;

import static controller.UserController.createUser;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserControllerTest {

    @Test
    public void testCreateUser() {
        String request = "GET /path?userId=testUser&password=testPassword&name=TestName&email=test@example.com";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        createUser(new HttpRequest(request));

        User expectedUser = new User("testUser", "testPassword", "TestName", "test@example.com");
        assertEquals(expectedUser, Database.findUserById("testUser"));
    }
}
