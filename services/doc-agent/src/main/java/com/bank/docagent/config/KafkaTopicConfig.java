package com.bank.docagent.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean public NewTopic submissionReceived() { return TopicBuilder.name("doc-agent.submission.received").partitions(3).replicas(1).build(); }
    @Bean public NewTopic classified()         { return TopicBuilder.name("doc-agent.classified").partitions(3).replicas(1).build(); }
    @Bean public NewTopic extracted()          { return TopicBuilder.name("doc-agent.extracted").partitions(3).replicas(1).build(); }
    @Bean public NewTopic verified()           { return TopicBuilder.name("doc-agent.verified").partitions(3).replicas(1).build(); }
    @Bean public NewTopic routed()             { return TopicBuilder.name("doc-agent.routed").partitions(3).replicas(1).build(); }
}
