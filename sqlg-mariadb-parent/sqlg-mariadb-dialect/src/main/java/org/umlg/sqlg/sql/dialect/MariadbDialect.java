package org.umlg.sqlg.sql.dialect;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.umlg.sqlg.structure.PropertyType;
import org.umlg.sqlg.structure.SchemaTable;
import org.umlg.sqlg.structure.SqlgGraph;
import org.umlg.sqlg.util.SqlgUtil;

import java.sql.*;
import java.time.*;
import java.util.*;

/**
 * @author Pieter Martin (https://github.com/pietermartin)
 *         Date: 2017/07/07
 */
@SuppressWarnings("unused")
public class MariadbDialect extends BaseSqlDialect {

    @Override
    public String sqlInsertEmptyValues() {
        return " VALUES ()";
    }

    @Override
    public List<Triple<String, String, String>> getVertexTables(DatabaseMetaData metaData) {
        List<Triple<String, String, String>> vertexTables = new ArrayList<>();
        String[] types = new String[]{"TABLE"};
        try {
            //load the vertices
            try (ResultSet vertexRs = metaData.getTables(null, null, "V_%", types)) {
                while (vertexRs.next()) {
                    //MariaDb does not support schemas.
                    String tblCat = null;
                    String schema = vertexRs.getString(1);
                    String table = vertexRs.getString(3);

                    //verify the table name matches our pattern
                    if (!table.startsWith("V_")) {
                        continue;
                    }
                    vertexTables.add(Triple.of(tblCat, schema, table));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return vertexTables;
    }

    @Override
    public List<Triple<String, String, String>> getEdgeTables(DatabaseMetaData metaData) {
        List<Triple<String, String, String>> edgeTables = new ArrayList<>();
        String[] types = new String[]{"TABLE"};
        try {
            //load the edges without their properties
            try (ResultSet edgeRs = metaData.getTables(null, null, "E_%", types)) {
                while (edgeRs.next()) {
                    String edgCat = null;
                    String schema = edgeRs.getString(1);
                    String table = edgeRs.getString(3);
                    edgeTables.add(Triple.of(edgCat, schema, table));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return edgeTables;
    }

    @Override
    public List<String> getSchemaNames(DatabaseMetaData metaData) {
        List<String> schemaNames = new ArrayList<>();
        try {
            try (ResultSet schemaRs = metaData.getCatalogs()) {
                while (schemaRs.next()) {
                    String schema = schemaRs.getString(1);
                    if (!this.getInternalSchemas().contains(schema)) {
                        schemaNames.add(schema);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return schemaNames;
    }

    @Override
    public boolean schemaExists(DatabaseMetaData metadata, String catalog, String schema) throws SQLException {
        ResultSet schemaRs = metadata.getCatalogs();
        while (schemaRs.next()) {
            String db = schemaRs.getString(1);
            if (db.equals(schema)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String maybeWrapInQoutes(String field) {
        return getColumnEscapeKey() + field.replace(getColumnEscapeKey(), "\"" + getColumnEscapeKey()) + getColumnEscapeKey();
//        return field.replace(getColumnEscapeKey(), "\"" + getColumnEscapeKey());
    }

    @Override
    public boolean isPrimaryKeyForeignKey(String lastIndexName) {
        return lastIndexName.startsWith("SYS_IDX") || lastIndexName.startsWith("SYS_FK");
    }

    @Override
    public String dialectName() {
        return "MariadbDialect";
    }

    @Override
    public Set<String> getInternalSchemas() {
        return new HashSet<>(Arrays.asList("information_schema", "performance_schema", "mysql", "test"));
    }

    @Override
    public boolean supportsDropSchemas() {
        return true;
    }

    @Override
    public boolean supportsCascade() {
        return false;
    }

    @Override
    public String getPublicSchema() {
        return "PUBLIC";
    }

    @Override
    public String existIndexQuery(SchemaTable schemaTable, String prefix, String indexName) {
        StringBuilder sb = new StringBuilder("SELECT * FROM INFORMATION_SCHEMA.SYSTEM_INDEXINFO WHERE TABLE_SCHEM = '");
        sb.append(schemaTable.getSchema());
        sb.append("' AND  TABLE_NAME = '");
        sb.append(prefix);
        sb.append(schemaTable.getTable());
        sb.append("' AND INDEX_NAME = '");
        sb.append(indexName);
        sb.append("'");
        return sb.toString();
    }

    @Override
    public boolean supportsTransactionalSchema() {
        return false;
    }

    @Override
    public void validateProperty(Object key, Object value) {
        if (value instanceof String) {
            return;
        }
        if (value instanceof Character) {
            return;
        }
        if (value instanceof Boolean) {
            return;
        }
        if (value instanceof Byte) {
            return;
        }
        if (value instanceof Short) {
            return;
        }
        if (value instanceof Integer) {
            return;
        }
        if (value instanceof Long) {
            return;
        }
        if (value instanceof Double) {
            return;
        }
        if (value instanceof LocalDate) {
            return;
        }
        if (value instanceof LocalDateTime) {
            return;
        }
        if (value instanceof ZonedDateTime) {
            return;
        }
        if (value instanceof LocalTime) {
            return;
        }
        if (value instanceof Period) {
            return;
        }
        if (value instanceof Duration) {
            return;
        }
        if (value instanceof byte[]) {
            return;
        }
        if (value instanceof boolean[]) {
            return;
        }
        if (value instanceof char[]) {
            return;
        }
        if (value instanceof short[]) {
            return;
        }
        if (value instanceof int[]) {
            return;
        }
        if (value instanceof long[]) {
            return;
        }
        if (value instanceof double[]) {
            return;
        }
        if (value instanceof String[]) {
            return;
        }
        if (value instanceof Character[]) {
            return;
        }
        if (value instanceof Boolean[]) {
            return;
        }
        if (value instanceof Byte[]) {
            return;
        }
        if (value instanceof Short[]) {
            return;
        }
        if (value instanceof Integer[]) {
            return;
        }
        if (value instanceof Long[]) {
            return;
        }
        if (value instanceof Double[]) {
            return;
        }
        if (value instanceof LocalDateTime[]) {
            return;
        }
        if (value instanceof LocalDate[]) {
            return;
        }
        if (value instanceof LocalTime[]) {
            return;
        }
        if (value instanceof ZonedDateTime[]) {
            return;
        }
        if (value instanceof Duration[]) {
            return;
        }
        if (value instanceof Period[]) {
            return;
        }
        throw Property.Exceptions.dataTypeOfPropertyValueNotSupported(value);
    }

    @Override
    public String getColumnEscapeKey() {
        return "`";
    }

    @Override
    public String getPrimaryKeyType() {
        return "BIGINT NOT NULL PRIMARY KEY";
    }

    @Override
    public String getAutoIncrementPrimaryKeyConstruct() {
        return "SERIAL PRIMARY KEY";
    }

    @Override
    public String[] propertyTypeToSqlDefinition(PropertyType propertyType) {
        switch (propertyType) {
            case BOOLEAN:
                return new String[]{"BOOLEAN"};
            case BYTE:
                return new String[]{"TINYINT"};
            case SHORT:
                return new String[]{"SMALLINT"};
            case INTEGER:
                return new String[]{"INTEGER"};
            case LONG:
                return new String[]{"BIGINT"};
            case DOUBLE:
                return new String[]{"DOUBLE"};
            case LOCALDATE:
                return new String[]{"DATE"};
            case LOCALDATETIME:
                return new String[]{"DATETIME"};
            case ZONEDDATETIME:
                return new String[]{"TIMESTAMP WITH TIME ZONE", "LONGVARCHAR"};
            case LOCALTIME:
                return new String[]{"TIME WITH TIME ZONE"};
            case PERIOD:
                return new String[]{"INTEGER", "INTEGER", "INTEGER"};
            case DURATION:
                return new String[]{"BIGINT", "INTEGER"};
            case STRING:
                return new String[]{"LONGTEXT"};
            case JSON:
                return new String[]{"LONGTEXT"};
            case POINT:
                throw new IllegalStateException("HSQLDB does not support gis types!");
            case POLYGON:
                throw new IllegalStateException("HSQLDB does not support gis types!");
            case GEOGRAPHY_POINT:
                throw new IllegalStateException("HSQLDB does not support gis types!");
            case GEOGRAPHY_POLYGON:
                throw new IllegalStateException("HSQLDB does not support gis types!");
            case BYTE_ARRAY:
                return new String[]{"LONGVARBINARY"};
            case byte_ARRAY:
                return new String[]{"LONGVARBINARY"};
            case boolean_ARRAY:
                return new String[]{"BOOLEAN ARRAY DEFAULT ARRAY[]"};
            case BOOLEAN_ARRAY:
                return new String[]{"BOOLEAN ARRAY DEFAULT ARRAY[]"};
            case SHORT_ARRAY:
                return new String[]{"SMALLINT ARRAY DEFAULT ARRAY[]"};
            case short_ARRAY:
                return new String[]{"SMALLINT ARRAY DEFAULT ARRAY[]"};
            case int_ARRAY:
                return new String[]{"INTEGER ARRAY DEFAULT ARRAY[]"};
            case INTEGER_ARRAY:
                return new String[]{"INTEGER ARRAY DEFAULT ARRAY[]"};
            case LONG_ARRAY:
                return new String[]{"BIGINT ARRAY DEFAULT ARRAY[]"};
            case long_ARRAY:
                return new String[]{"BIGINT ARRAY DEFAULT ARRAY[]"};
            case float_ARRAY:
                return new String[]{"REAL ARRAY DEFAULT ARRAY[]"};
            case DOUBLE_ARRAY:
                return new String[]{"DOUBLE ARRAY DEFAULT ARRAY[]"};
            case double_ARRAY:
                return new String[]{"DOUBLE ARRAY DEFAULT ARRAY[]"};
            case STRING_ARRAY:
                return new String[]{"LONGVARCHAR ARRAY DEFAULT ARRAY[]"};
            case LOCALDATETIME_ARRAY:
                return new String[]{"TIMESTAMP WITH TIME ZONE ARRAY DEFAULT ARRAY[]"};
            case LOCALDATE_ARRAY:
                return new String[]{"DATE ARRAY DEFAULT ARRAY[]"};
            case LOCALTIME_ARRAY:
                return new String[]{"TIME WITH TIME ZONE ARRAY DEFAULT ARRAY[]"};
            case ZONEDDATETIME_ARRAY:
                return new String[]{"TIMESTAMP WITH TIME ZONE ARRAY DEFAULT ARRAY[]", "LONGVARCHAR ARRAY DEFAULT ARRAY[]"};
            case DURATION_ARRAY:
                return new String[]{"BIGINT ARRAY DEFAULT ARRAY[]", "INTEGER ARRAY DEFAULT ARRAY[]"};
            case PERIOD_ARRAY:
                return new String[]{"INTEGER ARRAY DEFAULT ARRAY[]", "INTEGER ARRAY DEFAULT ARRAY[]", "INTEGER ARRAY DEFAULT ARRAY[]"};
            default:
                throw new IllegalStateException("Unknown propertyType " + propertyType.name());
        }
    }

    @Override
    public int propertyTypeToJavaSqlType(PropertyType propertyType) {
        switch (propertyType) {
            case BOOLEAN:
                return Types.BOOLEAN;
            case BYTE:
                return Types.TINYINT;
            case SHORT:
                return Types.SMALLINT;
            case INTEGER:
                return Types.INTEGER;
            case LONG:
                return Types.BIGINT;
            case DOUBLE:
                return Types.DOUBLE;
            case STRING:
                return Types.CLOB;
            case LOCALDATETIME:
                return Types.TIMESTAMP;
            case LOCALDATE:
                return Types.DATE;
            case LOCALTIME:
                return Types.TIME;
            case JSON:
                //TODO support other others like Geometry...
                return Types.OTHER;
            case byte_ARRAY:
                return Types.ARRAY;
            case boolean_ARRAY:
                return Types.ARRAY;
            case short_ARRAY:
                return Types.ARRAY;
            case int_ARRAY:
                return Types.ARRAY;
            case long_ARRAY:
                return Types.ARRAY;
            case float_ARRAY:
                return Types.ARRAY;
            case double_ARRAY:
                return Types.ARRAY;
            case STRING_ARRAY:
                return Types.ARRAY;
            default:
                throw new IllegalStateException("Unknown propertyType " + propertyType.name());
        }
    }

    @Override
    public PropertyType sqlTypeToPropertyType(SqlgGraph sqlgGraph, String schema, String table, String column, int sqlType, String typeName, ListIterator<Triple<String, Integer, String>> metaDataIter) {
        switch (sqlType) {
            case Types.BOOLEAN:
                return PropertyType.BOOLEAN;
            case Types.SMALLINT:
                return PropertyType.SHORT;
            case Types.INTEGER:
                return PropertyType.INTEGER;
            case Types.BIGINT:
                return PropertyType.LONG;
            case Types.REAL:
                return PropertyType.FLOAT;
            case Types.DOUBLE:
                return PropertyType.DOUBLE;
            case Types.VARCHAR:
                return PropertyType.STRING;
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return PropertyType.LOCALDATETIME;
            case Types.DATE:
                return PropertyType.LOCALDATE;
            case Types.TIME:
                return PropertyType.LOCALTIME;
            case Types.VARBINARY:
                return PropertyType.BYTE_ARRAY;
            case Types.ARRAY:
                return sqlArrayTypeNameToPropertyType(typeName, sqlgGraph, schema, table, column, metaDataIter);
            default:
                throw new IllegalStateException("Unknown sqlType " + sqlType);
        }
    }

    @Override
    public PropertyType sqlArrayTypeNameToPropertyType(String typeName, SqlgGraph sqlgGraph, String schema, String table, String columnName, ListIterator<Triple<String, Integer, String>> metaDataIter) {
        switch (typeName) {
            case "BOOLEAN ARRAY":
                return PropertyType.BOOLEAN_ARRAY;
            case "SMALLINT ARRAY":
                return PropertyType.SHORT_ARRAY;
            case "INTEGER ARRAY":
                return PropertyType.INTEGER_ARRAY;
            case "BIGINT ARRAY":
                return PropertyType.LONG_ARRAY;
            case "DOUBLE ARRAY":
                return PropertyType.DOUBLE_ARRAY;
            case "DATE ARRAY":
                return PropertyType.LOCALDATE_ARRAY;
            case "TIME WITH TIME ZONE ARRAY":
                return PropertyType.LOCALTIME_ARRAY;
            case "TIMESTAMP WITH TIME ZONE ARRAY":
                //need to check the next column to know if its a LocalDateTime or ZonedDateTime array
                Triple<String, Integer, String> metaData = metaDataIter.next();
                metaDataIter.previous();
                if (metaData.getLeft().startsWith(columnName + "~~~")) {
                    return PropertyType.ZONEDDATETIME_ARRAY;
                } else {
                    return PropertyType.LOCALDATETIME_ARRAY;
                }
            default:
                if (typeName.contains("VARCHAR") && typeName.contains("ARRAY")) {
                    return PropertyType.STRING_ARRAY;
                } else {
                    throw new RuntimeException(String.format("Array type not supported typeName = %s", typeName));
                }
        }
    }

    @Override
    public String getForeignKeyTypeDefinition() {
        return "BIGINT UNSIGNED";
    }

    @Override
    public boolean supportsFloatValues() {
        return false;
    }

    @Override
    public boolean supportsByteValues() {
        return true;
    }

    @Override
    public boolean supportsFloatArrayValues() {
        return false;
    }

    @Override
    public String getArrayDriverType(PropertyType propertyType) {
        switch (propertyType) {
            case boolean_ARRAY:
                return "BOOLEAN";
            case BOOLEAN_ARRAY:
                return "BOOLEAN";
            case SHORT_ARRAY:
                return "SMALLINT";
            case short_ARRAY:
                return "SMALLINT";
            case INTEGER_ARRAY:
                return "INTEGER";
            case int_ARRAY:
                return "INTEGER";
            case LONG_ARRAY:
                return "BIGINT";
            case long_ARRAY:
                return "BIGINT";
            case DOUBLE_ARRAY:
                return "DOUBLE";
            case double_ARRAY:
                return "DOUBLE";
            case STRING_ARRAY:
                return "VARCHAR";
            case LOCALDATETIME_ARRAY:
                return "TIMESTAMP";
            case LOCALDATE_ARRAY:
                return "DATE";
            case LOCALTIME_ARRAY:
                return "TIME";
            default:
                throw new IllegalStateException("propertyType " + propertyType.name() + " unknown!");
        }
    }

    @Override
    public String createTableStatement() {
        return "CREATE TABLE ";
    }

    @Override
    public void validateColumnName(String column) {
        super.validateColumnName(column);
    }

    @Override
    public Set<String> getSpacialRefTable() {
        return Collections.emptySet();
    }

    @Override
    public List<String> getGisSchemas() {
        return Collections.emptyList();
    }

    @Override
    public void setJson(PreparedStatement preparedStatement, int parameterStartIndex, JsonNode right) {
        throw new IllegalStateException("Hsqldb does not support json types, this should not have happened!");
    }

    @Override
    public void setPoint(PreparedStatement preparedStatement, int parameterStartIndex, Object point) {
        throw new IllegalStateException("Hsqldb does not support gis types, this should not have happened!");
    }

    @Override
    public void setLineString(PreparedStatement preparedStatement, int parameterStartIndex, Object lineString) {
        throw new IllegalStateException("Hsqldb does not support gis types, this should not have happened!");
    }

    @Override
    public void setPolygon(PreparedStatement preparedStatement, int parameterStartIndex, Object point) {
        throw new IllegalStateException("Hsqldb does not support gis types, this should not have happened!");
    }

    @Override
    public void setGeographyPoint(PreparedStatement preparedStatement, int parameterStartIndex, Object point) {
        throw new IllegalStateException("Hsqldb does not support gis types, this should not have happened!");
    }

    @Override
    public void handleOther(Map<String, Object> properties, String columnName, Object o, PropertyType propertyType) {
        throw new IllegalStateException("Hsqldb does not support other types, this should not have happened!");
    }

    @Override
    public <T> T getGis(SqlgGraph sqlgGraph) {
        throw new IllegalStateException("Hsqldb does not support other types, this should not have happened!");
    }

    @Override
    public void lockTable(SqlgGraph sqlgGraph, SchemaTable schemaTable, String prefix) {
        throw new UnsupportedOperationException("Hsqldb does not support table locking!");
    }

    @Override
    public void alterSequenceCacheSize(SqlgGraph sqlgGraph, SchemaTable schemaTable, String sequence, int batchSize) {
        throw new UnsupportedOperationException("Hsqldb does not support alterSequenceCacheSize!");
    }

    @Override
    public long nextSequenceVal(SqlgGraph sqlgGraph, SchemaTable schemaTable, String prefix) {
        throw new UnsupportedOperationException("Hsqldb does not support nextSequenceVal!");
    }

    @Override
    public long currSequenceVal(SqlgGraph sqlgGraph, SchemaTable schemaTable, String prefix) {
        throw new UnsupportedOperationException("Hsqldb does not support currSequenceVal!");
    }

    @Override
    public String sequenceName(SqlgGraph sqlgGraph, SchemaTable outSchemaTable, String prefix) {
        throw new UnsupportedOperationException("Hsqldb does not support sequenceName!");
    }

    @Override
    public boolean supportsBulkWithinOut() {
        return false;
    }

    @Override
    public String createTemporaryTableStatement() {
        return "DECLARE LOCAL TEMPORARY TABLE ";
    }

    @Override
    public String afterCreateTemporaryTableStatement() {
        return "";
    }

    @Override
    public List<String> sqlgTopologyCreationScripts() {
        List<String> result = new ArrayList<>();

        //SERIAL is an alias for BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE
        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`V_schema` (`ID` SERIAL PRIMARY KEY, `createdOn` DATETIME, `name` TEXT);");
        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`V_vertex` (`ID` SERIAL PRIMARY KEY, `createdOn` DATETIME, `name` TEXT, `schemaVertex` TEXT);");
        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`V_edge` (`ID` SERIAL PRIMARY KEY, `createdOn` DATETIME, `name` TEXT);");
        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`V_property` (`ID` SERIAL PRIMARY KEY, `createdOn` DATETIME, `name` TEXT, `type` TEXT);");
        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`V_index` (`ID` SERIAL PRIMARY KEY, `createdOn` DATETIME, `name` TEXT, `index_type` TEXT);");
        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`V_globalUniqueIndex` (" +
                "`ID` SERIAL PRIMARY KEY, " +
                "`createdOn` DATETIME, " +
                "`name` TEXT);");

        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`E_schema_vertex`(" +
                "`ID` SERIAL PRIMARY KEY, " +
                "`sqlg_schema.vertex__I` BIGINT UNSIGNED, " +
                "`sqlg_schema.schema__O` BIGINT UNSIGNED, " +
                "FOREIGN KEY (`sqlg_schema.vertex__I`) REFERENCES `sqlg_schema`.`V_vertex` (`ID`), " +
                "FOREIGN KEY (`sqlg_schema.schema__O`) REFERENCES `sqlg_schema`.`V_schema` (`ID`)" +
                ");");

        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`E_in_edges`(" +
                "`ID` SERIAL PRIMARY KEY, " +
                "`sqlg_schema.edge__I` BIGINT UNSIGNED, " +
                "`sqlg_schema.vertex__O` BIGINT UNSIGNED, " +
                "FOREIGN KEY (`sqlg_schema.edge__I`) REFERENCES `sqlg_schema`.`V_edge` (`ID`), " +
                "FOREIGN KEY (`sqlg_schema.vertex__O`) REFERENCES `sqlg_schema`.`V_vertex` (`ID`)" +
                ");");

        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`E_out_edges`(" +
                "`ID` SERIAL PRIMARY KEY, " +
                "`sqlg_schema.edge__I` BIGINT UNSIGNED, " +
                "`sqlg_schema.vertex__O` BIGINT UNSIGNED, " +
                "FOREIGN KEY (`sqlg_schema.edge__I`) REFERENCES `sqlg_schema`.`V_edge` (`ID`), " +
                "FOREIGN KEY (`sqlg_schema.vertex__O`) REFERENCES `sqlg_schema`.`V_vertex` (`ID`)" +
                ");");

        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`E_vertex_property`(" +
                "`ID` SERIAL PRIMARY KEY, " +
                "`sqlg_schema.property__I` BIGINT UNSIGNED, " +
                "`sqlg_schema.vertex__O` BIGINT UNSIGNED, " +
                "FOREIGN KEY (`sqlg_schema.property__I`) REFERENCES `sqlg_schema`.`V_property` (`ID`), " +
                "FOREIGN KEY (`sqlg_schema.vertex__O`) REFERENCES `sqlg_schema`.`V_vertex` (`ID`)" +
                ");");

        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`E_edge_property`(" +
                "`ID` SERIAL PRIMARY KEY, " +
                "`sqlg_schema.property__I` BIGINT UNSIGNED, " +
                "`sqlg_schema.edge__O` BIGINT UNSIGNED, " +
                "FOREIGN KEY (`sqlg_schema.property__I`) REFERENCES `sqlg_schema`.`V_property` (`ID`), " +
                "FOREIGN KEY (`sqlg_schema.edge__O`) REFERENCES `sqlg_schema`.`V_edge` (`ID`)" +
                ");");

        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`E_vertex_index`(" +
                "`ID` SERIAL PRIMARY KEY, " +
                "`sqlg_schema.index__I` BIGINT UNSIGNED, " +
                "`sqlg_schema.vertex__O` BIGINT UNSIGNED, " +
                "FOREIGN KEY (`sqlg_schema.index__I`) REFERENCES `sqlg_schema`.`V_index` (`ID`), " +
                "FOREIGN KEY (`sqlg_schema.vertex__O`) REFERENCES `sqlg_schema`.`V_vertex` (`ID`)" +
                ");");

        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`E_edge_index`(" +
                "`ID` SERIAL PRIMARY KEY, " +
                "`sqlg_schema.index__I` BIGINT UNSIGNED, " +
                "`sqlg_schema.edge__O` BIGINT UNSIGNED, " +
                "FOREIGN KEY (`sqlg_schema.index__I`) REFERENCES `sqlg_schema`.`V_index` (`ID`), " +
                "FOREIGN KEY (`sqlg_schema.edge__O`) REFERENCES `sqlg_schema`.`V_edge` (`ID`)" +
                ");");

        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`E_index_property`(" +
                "`ID` SERIAL PRIMARY KEY, " +
                "`sqlg_schema.property__I` BIGINT UNSIGNED, " +
                "`sqlg_schema.index__O` BIGINT UNSIGNED, " +
                "FOREIGN KEY (`sqlg_schema.property__I`) REFERENCES `sqlg_schema`.`V_property` (`ID`), " +
                "FOREIGN KEY (`sqlg_schema.index__O`) REFERENCES `sqlg_schema`.`V_index` (`ID`)" +
                ");");

        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`E_globalUniqueIndex_property`(" +
                "`ID` SERIAL PRIMARY KEY, " +
                "`sqlg_schema.property__I` BIGINT UNSIGNED, " +
                "`sqlg_schema.globalUniqueIndex__O` BIGINT UNSIGNED, " +
                "FOREIGN KEY (`sqlg_schema.property__I`) REFERENCES `sqlg_schema`.`V_property` (`ID`), " +
                "FOREIGN KEY (`sqlg_schema.globalUniqueIndex__O`) REFERENCES `sqlg_schema`.`V_globalUniqueIndex` (`ID`)" +
                ");");

        result.add("CREATE TABLE IF NOT EXISTS `sqlg_schema`.`V_log`(`ID` SERIAL PRIMARY KEY, `timestamp` DATETIME, `pid` INTEGER, `log` TEXT);");

        return result;
    }

    @Override
    public String sqlgAddPropertyIndexTypeColumn() {
        return "ALTER TABLE \"sqlg_schema\".\"V_property\" ADD COLUMN \"index_type\" LONGVARCHAR DEFAULT 'NONE';";
    }

    @Override
    public Long getPrimaryKeyStartValue() {
        return 0L;
    }

    private Array createArrayOf(Connection conn, PropertyType propertyType, Object[] data) {
        try {
            switch (propertyType) {
                case LOCALTIME_ARRAY:
//                    // shit DST for local time
//                    if (data != null) {
//                        int a = 0;
//                        for (Object o : data) {
//                            data[a++] = shiftDST(((Time) o).toLocalTime());
//                        }
//                    }
                    // fall through
                case STRING_ARRAY:
                case long_ARRAY:
                case LONG_ARRAY:
                case int_ARRAY:
                case INTEGER_ARRAY:
                case short_ARRAY:
                case SHORT_ARRAY:
                case float_ARRAY:
                case FLOAT_ARRAY:
                case double_ARRAY:
                case DOUBLE_ARRAY:
                case boolean_ARRAY:
                case BOOLEAN_ARRAY:
                case LOCALDATETIME_ARRAY:
                case LOCALDATE_ARRAY:
                case ZONEDDATETIME_ARRAY:
                case JSON_ARRAY:
                    return conn.createArrayOf(getArrayDriverType(propertyType), data);
                default:
                    throw new IllegalStateException("Unhandled array type " + propertyType.name());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object convertArray(PropertyType propertyType, java.sql.Array array) throws SQLException {
        switch (propertyType) {
            case BOOLEAN_ARRAY:
                return SqlgUtil.convertObjectArrayToBooleanArray((Object[]) array.getArray());
            case boolean_ARRAY:
                return SqlgUtil.convertObjectArrayToBooleanPrimitiveArray((Object[]) array.getArray());
            case SHORT_ARRAY:
                return SqlgUtil.convertObjectOfIntegersArrayToShortArray((Object[]) array.getArray());
            case short_ARRAY:
                return SqlgUtil.convertObjectOfIntegersArrayToShortPrimitiveArray((Object[]) array.getArray());
            case INTEGER_ARRAY:
                return SqlgUtil.convertObjectOfIntegersArrayToIntegerArray((Object[]) array.getArray());
            case int_ARRAY:
                return SqlgUtil.convertObjectOfIntegersArrayToIntegerPrimitiveArray((Object[]) array.getArray());
            case LONG_ARRAY:
                return SqlgUtil.convertObjectOfLongsArrayToLongArray((Object[]) array.getArray());
            case long_ARRAY:
                return SqlgUtil.convertObjectOfLongsArrayToLongPrimitiveArray((Object[]) array.getArray());
            case DOUBLE_ARRAY:
                return SqlgUtil.convertObjectOfDoublesArrayToDoubleArray((Object[]) array.getArray());
            case double_ARRAY:
                return SqlgUtil.convertObjectOfDoublesArrayToDoublePrimitiveArray((Object[]) array.getArray());
            case FLOAT_ARRAY:
                return SqlgUtil.convertObjectOfFloatsArrayToFloatArray((Object[]) array.getArray());
            case float_ARRAY:
                return SqlgUtil.convertObjectOfFloatsArrayToFloatPrimitiveArray((Object[]) array.getArray());
            case STRING_ARRAY:
                return SqlgUtil.convertObjectOfStringsArrayToStringArray((Object[]) array.getArray());
            case LOCALDATETIME_ARRAY:
                Object[] timestamps = (Object[]) array.getArray();
                return SqlgUtil.copyObjectArrayOfTimestampToLocalDateTime(timestamps, new LocalDateTime[(timestamps).length]);
            case LOCALDATE_ARRAY:
                Object[] dates = (Object[]) array.getArray();
                return SqlgUtil.copyObjectArrayOfDateToLocalDate(dates, new LocalDate[dates.length]);
            case LOCALTIME_ARRAY:
                Object[] times = (Object[]) array.getArray();
                return SqlgUtil.copyObjectArrayOfTimeToLocalTime(times, new LocalTime[times.length]);
            default:
                throw new IllegalStateException("Unhandled property type " + propertyType.name());
        }
    }

    @Override
    public void setArray(PreparedStatement statement, int index, PropertyType type,
                         Object[] values) throws SQLException {
        statement.setArray(index, createArrayOf(statement.getConnection(), type, values));
    }

    @Override
    public boolean isSystemIndex(String indexName) {
        return indexName.startsWith("SYS_IDX_") || indexName.startsWith("SYS_PK") || indexName.endsWith("SYS_FK");
    }

    @Override
    public String valueToString(PropertyType propertyType, Object value) {
        throw new RuntimeException("Hsqldb.valueToString should not be called.");
    }
}
