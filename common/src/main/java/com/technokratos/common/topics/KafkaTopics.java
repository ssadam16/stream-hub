package com.technokratos.common.topics;

import lombok.experimental.UtilityClass;

@UtilityClass
public class KafkaTopics {
    public static final String TOPIC_CODE_EXECUTION        = "code.execution.requests";
    public static final String TOPIC_TASK_STATS            = "task.statistics.events";
    public static final String TOPIC_SOLUTION_COMPLETED    = "solution.completed.events";
    public static final String TOPIC_EXECUTION_RESULTS     = "code.execution.results";
    public static final String TOPIC_EXECUTION_DLQ     = "code.execution.dlq";
}
