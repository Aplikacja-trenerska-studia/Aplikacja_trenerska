package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import com.studia.aplikacja_trenerska.config.DevAccountInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AplikacjaTrenerskaApplicationTests {
	@Autowired ApplicationContext context;

	@Test
	void contextLoads() {
		assertThat(context.getBeansOfType(DevAccountInitializer.class)).isEmpty();
	}
}
