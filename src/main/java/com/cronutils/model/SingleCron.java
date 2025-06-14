package com.cronutils.model;

import com.cronutils.mapper.CronMapper;
import com.cronutils.model.definition.CronConstraint;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.expression.visitor.ValidationFieldExpressionVisitor;
import com.cronutils.utils.Preconditions;
import java.time.ZonedDateTime;
import com.cronutils.model.time.ExecutionTime;
import java.util.*;

public class SingleCron implements Cron {
    private static final long serialVersionUID = 7487370826825439098L;
    private final CronDefinition cronDefinition;
    private final Map<CronFieldName, CronField> fields;
    private String asString;

    /**
     * Creates a Cron with the given cron definition and the given fields.
     * @param cronDefinition the definition to use for this Cron
     * @param fields the fields that should be used
     */
    public SingleCron(final CronDefinition cronDefinition, final List<CronField> fields) {
        this.cronDefinition = Preconditions.checkNotNull(cronDefinition, "CronDefinition must not be null");
        Preconditions.checkNotNull(fields, "CronFields cannot be null");
        this.fields = new EnumMap<>(CronFieldName.class);
        for (final CronField field : fields) {
            this.fields.put(field.getField(), field);
        }
    }

    /**
     * Retrieve value for cron field.
     *
     * @param name - cron field name.
     *             If null, a NullPointerException will be raised.
     * @return CronField that corresponds to given CronFieldName
     */
    public CronField retrieve(final CronFieldName name) {
        return fields.get(Preconditions.checkNotNull(name, "CronFieldName must not be null"));
    }

    /**
     * Retrieve all cron field values as map.
     *
     * @return unmodifiable Map with key CronFieldName and values CronField, never null
     */
    public Map<CronFieldName, CronField> retrieveFieldsAsMap() {
        return Collections.unmodifiableMap(fields);
    }

    public String asString() {
        if (asString == null) {
            final ArrayList<CronField> temporaryFields = new ArrayList<>(fields.values());
            temporaryFields.sort(CronField.createFieldComparator());
            final StringBuilder builder = new StringBuilder();
            for (final CronField field : temporaryFields) {
                builder.append(String.format("%s ", field.getExpression().asString()));
            }
            asString = builder.toString().trim();
        }
        return asString;
    }

    public CronDefinition getCronDefinition() {
        return cronDefinition;
    }

    /**
     * Validates this Cron instance by validating its cron expression.
     *
     * @return this Cron instance
     * @throws IllegalArgumentException if the cron expression is invalid
     */
    public Cron validate() {
        for (final Map.Entry<CronFieldName, CronField> field : retrieveFieldsAsMap().entrySet()) {
            final CronFieldName fieldName = field.getKey();
            field.getValue().getExpression().accept(
                    new ValidationFieldExpressionVisitor(getCronDefinition().getFieldDefinition(fieldName).getConstraints())
            );
        }
        for (final CronConstraint constraint : getCronDefinition().getCronConstraints()) {
            if (!constraint.validate(this)) {
                throw new IllegalArgumentException(String.format("Invalid cron expression: %s. %s", asString(), constraint.getDescription()));
            }
        }
        return this;
    }

    /**
     * Provides means to compare if two cron expressions are equivalent.
     *
     * @param cronMapper - maps 'cron' parameter to this instance definition;
     * @param cron       - any cron instance, never null
     * @return boolean - true if equivalent; false otherwise.
     */
    public boolean equivalent(final CronMapper cronMapper, final Cron cron) {
        return asString().equals(cronMapper.map(cron).asString());
    }

    /**
     * Provides means to compare if two cron expressions are equivalent.
     * Assumes same cron definition.
     *
     * @param cron - any cron instance, never null
     * @return boolean - true if equivalent; false otherwise.
     */
    public boolean equivalent(final Cron cron) {
        return asString().equals(cron.asString());
    }

    @Override
    public boolean overlap(final Cron cron) {
        Preconditions.checkNotNull(cron, "Cron must not be null");

        ExecutionTime thisExecutionTime = ExecutionTime.forCron(this);
        ExecutionTime otherExecutionTime = ExecutionTime.forCron(cron);

        // Start checking from now
        ZonedDateTime now = ZonedDateTime.now();
        
        // Get next execution for both crons
        Optional<ZonedDateTime> nextThis = thisExecutionTime.nextExecution(now);
        Optional<ZonedDateTime> nextOther = otherExecutionTime.nextExecution(now);
        
        // If either cron has no next execution, they can't overlap
        if (!nextThis.isPresent() || !nextOther.isPresent()) {
            return false;
        }

        // Check next 10 executions of this cron against the other cron
        ZonedDateTime checkDate = nextThis.get();
        for (int i = 0; i < 10; i++) {
            if (otherExecutionTime.isMatch(checkDate)) {
                return true;
            }
            Optional<ZonedDateTime> next = thisExecutionTime.nextExecution(checkDate);
            if (!next.isPresent()) {
                break;
            }
            checkDate = next.get();
        }

        // Check next 10 executions of other cron against this cron
        checkDate = nextOther.get();
        for (int i = 0; i < 10; i++) {
            if (thisExecutionTime.isMatch(checkDate)) {
                return true;
            }
            Optional<ZonedDateTime> next = otherExecutionTime.nextExecution(checkDate);
            if (!next.isPresent()) {
                break;
            }
            checkDate = next.get();
        }

        return false;
    }
}
