package controllers;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.DemoApplication;

@SpringBootTest(classes = DemoApplication.class)
public class SimpleTest {

    @Test
    public void contextLoads() {
        assert(true);
    }
}