/*
 * Copyright 2017 jmrozanec
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cronutils.model.definition;

import com.cronutils.model.Cron;
import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.expression.QuestionMark;

public class CronConstraintsFactory {

    private CronConstraintsFactory() {/*HIDE*/}

    /**
     * Creates CronConstraint to ensure that either day-of-year or month is assigned a specific value.
     *
     * @return newly created CronConstraint instance, never {@code null};
     */
    public static CronConstraint ensureEitherDayOfYearOrMonth() {
        return new CronConstraint("Both, a day-of-year AND a day-of-month or day-of-week, are not supported.") {
            private static final long serialVersionUID = 520379111876897579L;

            @Override
            public boolean validate(Cron cron) {
                CronField dayOfYearField = cron.retrieve(CronFieldName.DAY_OF_YEAR);
                if (dayOfYearField != null && !(dayOfYearField.getExpression() instanceof QuestionMark)) {
                    return cron.retrieve(CronFieldName.DAY_OF_WEEK).getExpression() instanceof QuestionMark
                            && cron.retrieve(CronFieldName.DAY_OF_MONTH).getExpression() instanceof QuestionMark;
                }

                return true;
            }
        };
    }

    public static CronConstraint ensureEitherDayOfWeekOrDayOfMonth() {
        //Solves issue #63: https://github.com/jmrozanec/cron-utils/issues/63
        //both a day-of-week AND a day-of-month parameter should fail for QUARTZ
        return new CronConstraint("Both, a day-of-week AND a day-of-month parameter, are not supported.") {
            private static final long serialVersionUID = -4423693913868081656L;

            @Override
            public boolean validate(Cron cron) {
                CronField dayOfYearField = cron.retrieve(CronFieldName.DAY_OF_YEAR);
                CronField dayOfMonthField = cron.retrieve(CronFieldName.DAY_OF_MONTH);
                CronField dayOfWeekField = cron.retrieve(CronFieldName.DAY_OF_WEEK);
                if (dayOfYearField == null || dayOfYearField.getExpression() instanceof QuestionMark) {
                    if (dayOfMonthField != null && !(dayOfMonthField.getExpression() instanceof QuestionMark)) {
                        return dayOfWeekField != null && dayOfWeekField.getExpression() instanceof QuestionMark;
                    } else {
                        return dayOfWeekField != null && !(dayOfWeekField.getExpression() instanceof QuestionMark);
                    }
                }

                return true;
            }
        };
    }

    /**
     * Creates CronConstraint to ensure that for Quartz cron expressions, either day-of-month or day-of-week must be a question mark.
     * This follows the Quartz specification which requires one of these fields to be a question mark.
     *
     * @return newly created CronConstraint instance, never {@code null};
     */
    public static CronConstraint ensureQuartzDayOfMonthAndDayOfWeekValidation() {
        return new CronConstraint("Invalid cron expression: Both, a day-of-week AND a day-of-month parameter, must have at least one with a '?' character.") {
            private static final long serialVersionUID = 520379111876897580L;

            @Override
            public boolean validate(Cron cron) {
                final CronField dayOfMonthField = cron.retrieve(CronFieldName.DAY_OF_MONTH);
                final CronField dayOfWeekField = cron.retrieve(CronFieldName.DAY_OF_WEEK);

                // At least one of them must be a question mark
                return (dayOfMonthField != null && dayOfMonthField.getExpression() instanceof QuestionMark) ||
                       (dayOfWeekField != null && dayOfWeekField.getExpression() instanceof QuestionMark);
            }
        };
    }
}
