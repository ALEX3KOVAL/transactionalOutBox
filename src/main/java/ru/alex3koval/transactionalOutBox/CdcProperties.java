package ru.alex3koval.transactionalOutBox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.cdc")
public record CdcProperties(
    String publicationName,
    String slotName,
    String name,
    String pluginName,
    String offsetStorageClass,
    String offsetStorageFileName,
    String offsetStorageFileFolderPath,
    Long offsetFlushIntervalMillis,
    String topicPrefix,
    String dbHost,
    Integer dbPort,
    String dbUser,
    String dbPassword,
    String dbName,
    String schemaIncludeList,
    String tableIncludeList,
    String columnIncludeList
) {
}
