package com.agrorental;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Requires live MySQL database connection on localhost:3306")
@SpringBootTest
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
