package com.zl.multiple.datasource.config;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.DES;
import com.zl.multiple.datasource.entity.MultiDatasourceProperties;
import com.zl.multiple.datasource.util.DynamicDataSource;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: springboot-multiple-dataSources
 * @description: 自动配置多数据源问题的配置类
 * @author: 张乐
 * @create: 2019-01-11 17:33
 **/
@Configuration
@EnableConfigurationProperties(MultiDatasourceProperties.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@ComponentScan("xf.zl.springbootstatermultipledatasource")
public class MultiDatasourceConfiguration {

    public static final int INITIAL_CAPACITY = 10;
    static final String key = "xsdgffdekeyqweas";

    @Bean("dynamicDataSource")
    public DynamicDataSource dynamicDataSource(MultiDatasourceProperties multiDatasourceProperties, DataSourceProperties defaultDataSourceProperties) {
        defaultDataSourceProperties = desCript(defaultDataSourceProperties);

        //创建动态数据源
        DynamicDataSource dynamicDataSource = new DynamicDataSource();

        Map<Object, Object> dataSources = new HashMap(INITIAL_CAPACITY);

        Map<String, DataSourceProperties> properties = multiDatasourceProperties.getMulti();
//        String poolClassName = multiDatasourceProperties.getPoolClassName();
        String poolClassName = "com.alibaba.druid.pool.DruidDataSource";
//        if (poolClassName == null) {
//            //默认使用c3p0数据库连接池
//            poolClassName = ComboPooledDataSource.class.getName();
//        }
        Class<DataSource> poolClass = null;
        try {
            poolClass = (Class<DataSource>) Class.forName(poolClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }

        DataSource defaultDb = getDataSource(poolClass, defaultDataSourceProperties);
        dynamicDataSource.setDefaultTargetDataSource(defaultDb);

        for (Map.Entry<String, DataSourceProperties> entry : properties.entrySet()) {
            entry.setValue(desCript(entry.getValue()));
            DataSource db = getDataSource(poolClass, entry.getValue());
            dataSources.put(entry.getKey(), db);
        }
        dynamicDataSource.setTargetDataSources(dataSources);

        return dynamicDataSource;
    }

    private DataSourceProperties desCript(DataSourceProperties properties){
        properties.setPassword(decryptStr(properties.getPassword()));
        return properties;
    }



    public static String decryptStr(String passod) {
        DES des = SecureUtil.des(key.getBytes());
        try {
            passod = des.decryptStr(passod);
        }catch (Exception e){}
        return passod;
    }
    public static String encrypt(String passod) {
        try {
            DES des = SecureUtil.des(key.getBytes());
            return HexUtil.encodeHexStr( des.encrypt(passod));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Bean
    public MultipleDatasourceDaoProcessor multipleDatasourceDaoProcessor() {
        return new MultipleDatasourceDaoProcessor();
    }

    private DataSource getDataSource(Class<DataSource> poolClass, DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(poolClass).build();
    }
}
