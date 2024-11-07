package controllers;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.example.DemoApplication;

import config.TestConfig;

@SpringBootTest(classes = DemoApplication.class)
@Import(TestConfig.class)
public class SimpleTest {

    @Test
    public void contextLoads() {
        assert(true);
    }
}