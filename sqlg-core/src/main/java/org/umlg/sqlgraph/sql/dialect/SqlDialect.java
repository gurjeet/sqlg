package org.umlg.sqlgraph.sql.dialect;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.umlg.sqlgraph.structure.PropertyType;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Created by pieter on 2014/07/16.
 */
public interface SqlDialect {

    PropertyType sqlTypeToPropertyType(int sqlType, String typeName);

    String getJdbcDriver();

    void validateProperty(Object key, Object value);

    public default boolean needsSemicolon() {
        return true;
    }

    public default boolean supportsCascade() {
        return true;
    }

    String getColumnEscapeKey();

    String getPrimaryKeyType();

    String getAutoIncrementPrimaryKeyConstruct();

    String propertyTypeToSqlDefinition(PropertyType propertyType);

    int propertyTypeToJavaSqlType(PropertyType propertyType);

    String getForeignKeyTypeDefinition();

    public default String maybeWrapInQoutes(String field) {
        StringBuilder sb = new StringBuilder(getColumnEscapeKey());
        sb.append(field);
        sb.append(getColumnEscapeKey());
        return sb.toString();
    }

    public default boolean supportsFloatValues() {
        return true;
    }

    public default boolean supportsTransactionalSchema() {
        return true;
    }

    public default boolean supportsBooleanArrayValues() {
        return true;
    }

    public default boolean supportsByteArrayValues() {
        return true;
    }

    public default boolean supportsDoubleArrayValues() {
        return true;
    }

    public default boolean supportsFloatArrayValues() {
        return true;
    }

    public default boolean supportsIntegerArrayValues() {
        return true;
    }

    public default boolean supportsLongArrayValues() {
        return true;
    }

    public default boolean supportsStringArrayValues() {
        return true;
    }

    public default void assertTableName(String tableName) {
    }

    public default void putJsonObject(ObjectNode obj, String columnName, int sqlType, Object o) {
        try {
            switch (sqlType) {
                case Types.BIT:
                    obj.put(columnName, (Boolean) o);
                    break;
                case Types.SMALLINT:
                    obj.put(columnName, ((Integer) o).shortValue());
                    break;
                case Types.INTEGER:
                    obj.put(columnName, (Integer) o);
                    break;
                case Types.BIGINT:
                    obj.put(columnName, (Long) o);
                    break;
                case Types.REAL:
                    obj.put(columnName, (Float) o);
                    break;
                case Types.DOUBLE:
                    obj.put(columnName, (Double) o);
                    break;
                case Types.VARCHAR:
                    obj.put(columnName, (String) o);
                    break;
                case Types.ARRAY:
                    ArrayNode arrayNode = obj.putArray(columnName);
                    Array array = (Array) o;
                    int baseType = array.getBaseType();
                    Object[] objectArray = (Object[]) array.getArray();
                    switch (baseType) {
                        case Types.BIT:
                            for (Object arrayElement : objectArray) {
                                arrayNode.add((Boolean) arrayElement);
                            }
                            break;
                        case Types.SMALLINT:
                            for (Object arrayElement : objectArray) {
                                arrayNode.add((Short) arrayElement);
                            }
                            break;
                        case Types.INTEGER:
                            for (Object arrayElement : objectArray) {
                                arrayNode.add((Integer) arrayElement);
                            }
                            break;
                        case Types.BIGINT:
                            for (Object arrayElement : objectArray) {
                                arrayNode.add((Long) arrayElement);
                            }
                            break;
                        case Types.REAL:
                            for (Object arrayElement : objectArray) {
                                arrayNode.add((Float) arrayElement);
                            }
                            break;
                        case Types.DOUBLE:
                            for (Object arrayElement : objectArray) {
                                arrayNode.add((Double) arrayElement);
                            }
                            break;
                        case Types.VARCHAR:
                            for (Object arrayElement : objectArray) {
                                arrayNode.add((String) arrayElement);
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unknown array sqlType " + sqlType);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown sqlType " + sqlType);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public default void putJsonMetaObject(ObjectNode obj, String columnName, int sqlType, Object o) {
        try {
            switch (sqlType) {
                case Types.BIT:
                    obj.put(columnName, PropertyType.BOOLEAN.name());
                    break;
                case Types.SMALLINT:
                    obj.put(columnName, PropertyType.SHORT.name());
                    break;
                case Types.INTEGER:
                    obj.put(columnName, PropertyType.INTEGER.name());
                    break;
                case Types.BIGINT:
                    obj.put(columnName, PropertyType.LONG.name());
                    break;
                case Types.REAL:
                    obj.put(columnName, PropertyType.FLOAT.name());
                    break;
                case Types.DOUBLE:
                    obj.put(columnName, PropertyType.DOUBLE.name());
                    break;
                case Types.VARCHAR:
                    obj.put(columnName, PropertyType.STRING.name());
                    break;
                case Types.ARRAY:
                    Array array = (Array) o;
                    int baseType = array.getBaseType();
                    switch (baseType) {
                        case Types.BIT:
                            obj.put(columnName, PropertyType.BOOLEAN_ARRAY.name());
                            break;
                        case Types.SMALLINT:
                            obj.put(columnName, PropertyType.SHORT_ARRAY.name());
                            break;
                        case Types.INTEGER:
                            obj.put(columnName, PropertyType.INTEGER_ARRAY.name());
                            break;
                        case Types.BIGINT:
                            obj.put(columnName, PropertyType.LONG_ARRAY.name());
                            break;
                        case Types.REAL:
                            obj.put(columnName, PropertyType.FLOAT_ARRAY.name());
                            break;
                        case Types.DOUBLE:
                            obj.put(columnName, PropertyType.DOUBLE_ARRAY.name());
                            break;
                        case Types.VARCHAR:
                            obj.put(columnName, PropertyType.STRING_ARRAY.name());
                            break;
                        default:
                            throw new IllegalStateException("Unknown array sqlType " + sqlType);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown sqlType " + sqlType);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

}
