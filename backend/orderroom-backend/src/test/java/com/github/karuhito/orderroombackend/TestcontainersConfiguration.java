package com.github.karuhito.orderroombackend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * テスト用の PostgreSQL を Docker で起動する設定。
 * コンテナは Bean なので Spring のコンテキストと同じ期間だけ動き、
 * 全テストクラスが同じ設定を @Import すればコンテキストもコンテナも1つで共有される。
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        // Neon と同じバージョンに合わせる
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18.6"));
    }
}
