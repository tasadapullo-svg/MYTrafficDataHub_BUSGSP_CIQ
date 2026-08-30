package com.mytransitgps;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mytransitgps.persistence.mapper.JbApiRequestLogMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 验证正式数据库开关启用时 MyBatis、Mapper 与 Repository 依赖链可以完成装配。 */
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:mybatis-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "mytransitgps.workspace-root=..",
                "mytransitgps.database.enabled=true",
                "mytransitgps.continuous.enabled=false",
                "archive.daily.enabled=false",
                "traffic.ciq.enabled=false"
        })
class MyBatisDatabaseEnabledContextTest {
    @Autowired
    SqlSessionFactory sqlSessionFactory;

    @Autowired
    JbApiRequestLogMapper apiRequestLogMapper;

    @Test
    void databaseEnabledCreatesSqlSessionFactoryAndMapper() {
        assertNotNull(sqlSessionFactory);
        assertNotNull(apiRequestLogMapper);
    }
}
