package com.example.aichat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 集成测试基线 - Spring 上下文加载冒烟测试。
 *
 * 本地化方案：共享 src/test/resources/application.yml 默认用空 H2（无 schema），
 * 而 ModelServiceImpl 启动期会查 ai_model 表，空 H2 无法建表。
 * 这里在本类内用 @SpringBootTest(properties=...) 把数据源覆盖回本机 MySQL
 * （localhost:3306/ai_chat，root/123456），Flyway 自动建表，让完整 ApplicationContext 真实加载。
 * Redis 仍走共享测试配置的降级端口（懒连接，不阻塞启动）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/ai_chat?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true",
        "spring.datasource.username=root",
        "spring.datasource.password=123456",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.flyway.enabled=false"
})
class AichatApplicationTests {

	@Test
	void contextLoads() {
	}

}
