package com.cronutils;

import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.cronutils.model.CronType.QUARTZ;
import static com.cronutils.model.CronType.SPRING;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class Issue605Test {

    static Stream<Arguments> cronExpressions() {
        return Stream.of(
                Arguments.of(QUARTZ, CronMapper.fromQuartzToCron4j()),
                Arguments.of(QUARTZ, CronMapper.fromQuartzToSpring()),
                Arguments.of(QUARTZ, CronMapper.fromQuartzToUnix()),
                Arguments.of(SPRING, CronMapper.fromSpringToQuartz())
        );
    }

    @ParameterizedTest
    @MethodSource("cronExpressions")
    void testDayOfWeekMappingSpring(CronType cronType, CronMapper mapper) {
        Cron cron = getCron(cronType, "0 0 0 ? * 5#1");
        assertDoesNotThrow(() -> mapper.map(cron));
    }

    private Cron getCron(CronType cronType, @SuppressWarnings("SameParameterValue") final String quartzExpression) {
        final CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(cronType);
        final CronParser parser = new CronParser(cronDefinition);
        return parser.parse(quartzExpression);
    }

}
