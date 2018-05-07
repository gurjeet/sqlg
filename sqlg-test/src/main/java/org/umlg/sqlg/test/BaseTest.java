package org.umlg.sqlg.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Level;
import org.apache.tinkerpop.gremlin.AbstractGremlinTest;
import org.apache.tinkerpop.gremlin.process.traversal.Step;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.MapHelper;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.GraphReader;
import org.apache.tinkerpop.gremlin.structure.io.Io;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONIo;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umlg.sqlg.TestAppender;
import org.umlg.sqlg.sql.parse.ReplacedStep;
import org.umlg.sqlg.step.SqlgGraphStep;
import org.umlg.sqlg.step.SqlgStep;
import org.umlg.sqlg.step.SqlgVertexStep;
import org.umlg.sqlg.strategy.SqlgSqlExecutor;
import org.umlg.sqlg.structure.SqlgGraph;
import org.umlg.sqlg.util.SqlgUtil;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Date: 2014/07/12
 * Time: 5:44 PM
 */
public abstract class BaseTest {

    private static Logger logger = LoggerFactory.getLogger(BaseTest.class.getName());
    protected SqlgGraph sqlgGraph;
    protected SqlgGraph sqlgGraph1;
    protected GraphTraversalSource gt;
    protected static Configuration configuration;
    private long start;
    protected static final int SLEEP_TIME = 1000;

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            BaseTest.this.start = System.currentTimeMillis();
            logger.info("Starting test: " + description.getClassName() + "." + description.getMethodName());
        }

        protected void finished(Description description) {
            long millis = System.currentTimeMillis() - BaseTest.this.start;
            String time = String.format("%02d min, %02d sec, %02d mil",
                    TimeUnit.MILLISECONDS.toMinutes(millis),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
                    TimeUnit.MILLISECONDS.toMillis(millis) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis))
            );
            logger.info(String.format("Finished test: %s.%s Time taken: %s", description.getClassName(), description.getMethodName(), time));
        }
    };

    @BeforeClass
    public static void beforeClass() throws ClassNotFoundException, IOException, PropertyVetoException {
        URL sqlProperties = Thread.currentThread().getContextClassLoader().getResource("sqlg.properties");
        try {
            configuration = new PropertiesConfiguration(sqlProperties);
            if (!configuration.containsKey("jdbc.url")) {
                throw new IllegalArgumentException(String.format("SqlGraph configuration requires that the %s be set", "jdbc.url"));
            }
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void before() throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        this.sqlgGraph = SqlgGraph.open(configuration);
        SqlgUtil.dropDb(this.sqlgGraph);
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.close();
        this.sqlgGraph = SqlgGraph.open(configuration);
        assertNotNull(this.sqlgGraph);
        assertNotNull(this.sqlgGraph.getBuildVersion());
        this.gt = this.sqlgGraph.traversal();
        if (configuration.getBoolean("distributed", false)) {
            this.sqlgGraph1 = SqlgGraph.open(configuration);
            assertNotNull(this.sqlgGraph1);
            assertEquals(this.sqlgGraph.getBuildVersion(),this.sqlgGraph1.getBuildVersion());
        }
        stopWatch.stop();
        logger.info("Startup time for test = " + stopWatch.toString());
    }

    @After
    public void after() throws Exception {
        try {
            this.sqlgGraph.tx().onClose(Transaction.CLOSE_BEHAVIOR.ROLLBACK);
            this.sqlgGraph.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        try {
            if (this.sqlgGraph1 != null) {
                this.sqlgGraph1.tx().onClose(Transaction.CLOSE_BEHAVIOR.ROLLBACK);
                this.sqlgGraph1.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static boolean isPostgres() {
        return configuration.getString("jdbc.url").contains("postgresql");
    }

    public static boolean isMsSqlServer() {
        return configuration.getString("jdbc.url").contains("sqlserver");
    }

    /**
     * return a clone of the configuration
     *
     * @return
     */
    protected static Configuration getConfigurationClone() {
        Configuration conf = new PropertiesConfiguration();
        Iterator<String> it = configuration.getKeys();
        while (it.hasNext()) {
            String s = it.next();
            conf.setProperty(s, configuration.getProperty(s));
        }
        return conf;
    }

    public void dropSqlgSchema(SqlgGraph sqlgGraph) {
        List<String> result = new ArrayList<>();
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_schema_vertex") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_in_edges") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_out_edges") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_vertex_property") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_vertex_partition") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_edge_partition") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_partition_partition") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_edge_property") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_vertex_identifier") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_edge_identifier") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_vertex_distribution") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_edge_distribution") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_vertex_colocate") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_edge_colocate") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_vertex_index") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_edge_index") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_index_property") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("E_globalUniqueIndex_property") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));

        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("V_graph") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("V_log") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("V_schema") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("V_vertex") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("V_edge") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("V_partition") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("V_property") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("V_index") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));
        result.add("DROP TABLE " + sqlgGraph.getSqlDialect().maybeWrapInQoutes("sqlg_schema") + "." + sqlgGraph.getSqlDialect().maybeWrapInQoutes("V_globalUniqueIndex") + (sqlgGraph.getSqlDialect().needsSemicolon() ? ";" : ""));

        result.add(sqlgGraph.getSqlDialect().dropSchemaStatement("sqlg_schema"));

        Connection connection = sqlgGraph.tx().getConnection();
        try (Statement statement = connection.createStatement()) {
            for (String s : result) {
                statement.execute(s);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    protected GraphTraversal<Vertex, Vertex> vertexTraversal(SqlgGraph sqlgGraph, Vertex v) {
        return sqlgGraph.traversal().V(v);
    }

    protected GraphTraversal<Edge, Edge> edgeTraversal(SqlgGraph sqlgGraph, Edge e) {
        return sqlgGraph.traversal().E(e);
    }

    protected void assertDb(String table, int numberOfRows) {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = this.sqlgGraph.getConnection();
            stmt = conn.createStatement();
            StringBuilder sql = new StringBuilder("SELECT * FROM ");
            sql.append(this.sqlgGraph.getSqlDialect().maybeWrapInQoutes(this.sqlgGraph.getSqlDialect().getPublicSchema()));
            sql.append(".");
            sql.append(this.sqlgGraph.getSqlDialect().maybeWrapInQoutes(table));
            if (this.sqlgGraph.getSqlDialect().needsSemicolon()) {
                sql.append(";");
            }
            if (logger.isDebugEnabled()) {
                logger.debug(sql.toString());
            }
            ResultSet rs = stmt.executeQuery(sql.toString());
            int countRows = 0;
            while (rs.next()) {
                countRows++;
            }
            assertEquals(numberOfRows, countRows);
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            //noinspection EmptyCatchBlock
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException se2) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                fail(se.getMessage());
            }
        }

    }

    /**
     * print the traversal before and after execution
     * @param traversal
     */
    protected void printTraversalForm(final Traversal<?, ?> traversal) {
        final boolean muted = Boolean.parseBoolean(System.getProperty("muteTestLogs", "false"));

        if (!muted) System.out.println("   pre-strategy:" + traversal);
        traversal.hasNext();
        if (!muted) System.out.println("  post-strategy:" + traversal);
    }

    /**
     * print the traversal before and after execution, and return the last SQL generated
     * @param traversal
     * @return the SQL or null
     */
    protected String getSQL(final Traversal<?, ?> traversal){
    	org.apache.log4j.Logger l=org.apache.log4j.Logger.getLogger(SqlgSqlExecutor.class);
    	Level old=l.getLevel();
    	try {
    		l.setLevel(Level.DEBUG);
	    	printTraversalForm(traversal);
	    	org.apache.log4j.spi.LoggingEvent evt=TestAppender.getLast(SqlgSqlExecutor.class.getName());
	    	if (evt!=null){
	    		return String.valueOf(evt.getMessage());
	    	}
    	return null;
    	} finally {
    		l.setLevel(old);
    	}

    }

    protected void loadModern(SqlgGraph sqlgGraph) {
        Io.Builder<GraphSONIo> builder = GraphSONIo.build(GraphSONVersion.V3_0);
        final GraphReader reader = sqlgGraph.io(builder).reader().create();
        try (final InputStream stream = AbstractGremlinTest.class.getResourceAsStream("/tinkerpop-modern-v3d0.json")) {
            reader.readGraph(stream, sqlgGraph);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    protected void loadModern() {
        loadModern(this.sqlgGraph);
    }

    protected void loadGratefulDead(SqlgGraph sqlgGraph) {
        Io.Builder<GraphSONIo> builder = GraphSONIo.build(GraphSONVersion.V3_0);
        final GraphReader reader = sqlgGraph.io(builder).reader().create();
        try (final InputStream stream = AbstractGremlinTest.class.getResourceAsStream("/grateful-dead-v3d0.json")) {
            reader.readGraph(stream, sqlgGraph);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    protected void loadGratefulDead() {
        loadGratefulDead(this.sqlgGraph);
    }

    /**
     * Looks up the identifier as generated by the current source graph being tested.
     *
     * @param vertexName a unique string that will identify a graph element within a graph
     * @return the id as generated by the graph
     */
    public Object convertToVertexId(final String vertexName) {
        return convertToVertexId(this.sqlgGraph, vertexName);
    }

    /**
     * Looks up the identifier as generated by the current source graph being tested.
     *
     * @param g          the graph to get the element id from
     * @param vertexName a unique string that will identify a graph element within a graph
     * @return the id as generated by the graph
     */
    public Object convertToVertexId(final Graph g, final String vertexName) {
        return convertToVertex(g, vertexName).id();
    }

    protected Vertex convertToVertex(final Graph graph, final String vertexName) {
        // all test graphs have "name" as a unique id which makes it easy to hardcode this...works for now
        return graph.traversal().V().has("name", vertexName).next();
    }

    public Object convertToEdgeId(final Graph graph, final String outVertexName, String edgeLabel, final String inVertexName) {
        return graph.traversal().V().has("name", outVertexName).outE(edgeLabel).as("e").inV().has("name", inVertexName).<Edge>select("e").next().id();
    }

    public static void assertModernGraph(final Graph g1, final boolean assertDouble, final boolean lossyForId) {
        assertToyGraph(g1, assertDouble, lossyForId, true);
    }

    private static void assertToyGraph(final Graph g1, final boolean assertDouble, final boolean lossyForId, final boolean assertSpecificLabel) {
        assertEquals(new Long(6), g1.traversal().V().count().next());
        assertEquals(new Long(6), g1.traversal().E().count().next());

        final Vertex v1 = g1.traversal().V().has("name", "marko").next();
        assertEquals(29, v1.<Integer>value("age").intValue());
        assertEquals(2, v1.keys().size());
        assertEquals(assertSpecificLabel ? "person" : Vertex.DEFAULT_LABEL, v1.label());
        assertId(g1, lossyForId, v1, 1);

        final List<Edge> v1Edges = g1.traversal().V(v1.id()).bothE().toList();
        assertEquals(3, v1Edges.size());
        v1Edges.forEach(e -> {
            if (g1.traversal().E(e.id()).inV().values("name").next().equals("vadas")) {
                assertEquals("knows", e.label());
                if (assertDouble)
                    assertEquals(0.5d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.5f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertId(g1, lossyForId, e, 7);
            } else if (g1.traversal().E(e.id()).inV().values("name").next().equals("josh")) {
                assertEquals("knows", e.label());
                if (assertDouble)
                    assertEquals(1.0, e.value("weight"), 0.0001d);
                else
                    assertEquals(1.0f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertId(g1, lossyForId, e, 8);
            } else if (g1.traversal().E(e.id()).inV().values("name").next().equals("lop")) {
                assertEquals("created", e.label());
                if (assertDouble)
                    assertEquals(0.4d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.4f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertId(g1, lossyForId, e, 9);
            } else {
                fail("Edge not expected");
            }
        });

        final Vertex v2 = g1.traversal().V().has("name", "vadas").next();
        assertEquals(27, v2.<Integer>value("age").intValue());
        assertEquals(2, v2.keys().size());
        assertEquals(assertSpecificLabel ? "person" : Vertex.DEFAULT_LABEL, v2.label());
        assertId(g1, lossyForId, v2, 2);

        final List<Edge> v2Edges = g1.traversal().V(v2.id()).bothE().toList();
        assertEquals(1, v2Edges.size());
        v2Edges.forEach(e -> {
            if (g1.traversal().E(e.id()).outV().values("name").next().equals("marko")) {
                assertEquals("knows", e.label());
                if (assertDouble)
                    assertEquals(0.5d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.5f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertId(g1, lossyForId, e, 7);
            } else {
                fail("Edge not expected");
            }
        });

        final Vertex v3 = g1.traversal().V().has("name", "lop").next();
        assertEquals("java", v3.<String>value("lang"));
        assertEquals(2, v2.keys().size());
        assertEquals(assertSpecificLabel ? "software" : Vertex.DEFAULT_LABEL, v3.label());
        assertId(g1, lossyForId, v3, 3);

        final List<Edge> v3Edges = g1.traversal().V(v3.id()).bothE().toList();
        assertEquals(3, v3Edges.size());
        v3Edges.forEach(e -> {
            if (g1.traversal().E(e.id()).outV().values("name").next().equals("peter")) {
                assertEquals("created", e.label());
                if (assertDouble)
                    assertEquals(0.2d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.2f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertId(g1, lossyForId, e, 12);
            } else if (g1.traversal().E(e.id()).outV().next().value("name").equals("josh")) {
                assertEquals("created", e.label());
                if (assertDouble)
                    assertEquals(0.4d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.4f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertId(g1, lossyForId, e, 11);
            } else if (g1.traversal().E(e.id()).outV().values("name").next().equals("marko")) {
                assertEquals("created", e.label());
                if (assertDouble)
                    assertEquals(0.4d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.4f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertId(g1, lossyForId, e, 9);
            } else {
                fail("Edge not expected");
            }
        });

        final Vertex v4 = g1.traversal().V().has("name", "josh").next();
        assertEquals(32, v4.<Integer>value("age").intValue());
        assertEquals(2, v4.keys().size());
        assertEquals(assertSpecificLabel ? "person" : Vertex.DEFAULT_LABEL, v4.label());
        assertId(g1, lossyForId, v4, 4);

        final List<Edge> v4Edges = g1.traversal().V(v4.id()).bothE().toList();
        assertEquals(3, v4Edges.size());
        v4Edges.forEach(e -> {
            if (e.inVertex().values("name").next().equals("ripple")) {
                assertEquals("created", e.label());
                if (assertDouble)
                    assertEquals(1.0d, e.value("weight"), 0.0001d);
                else
                    assertEquals(1.0f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertId(g1, lossyForId, e, 10);
            } else if (e.inVertex().values("name").next().equals("lop")) {
                assertEquals("created", e.label());
                if (assertDouble)
                    assertEquals(0.4d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.4f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertId(g1, lossyForId, e, 11);
            } else if (e.outVertex().values("name").next().equals("marko")) {
                assertEquals("knows", e.label());
                if (assertDouble)
                    assertEquals(1.0d, e.value("weight"), 0.0001d);
                else
                    assertEquals(1.0f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertId(g1, lossyForId, e, 8);
            } else {
                fail("Edge not expected");
            }
        });

        final Vertex v5 = g1.traversal().V().has("name", "ripple").next();
        assertEquals("java", v5.<String>value("lang"));
        assertEquals(2, v5.keys().size());
        assertEquals(assertSpecificLabel ? "software" : Vertex.DEFAULT_LABEL, v5.label());
        assertId(g1, lossyForId, v5, 5);

        final List<Edge> v5Edges = IteratorUtils.list(v5.edges(Direction.BOTH));
        assertEquals(1, v5Edges.size());
        v5Edges.forEach(e -> {
            if (e.outVertex().values("name").next().equals("josh")) {
                assertEquals("created", e.label());
                if (assertDouble)
                    assertEquals(1.0d, e.value("weight"), 0.0001d);
                else
                    assertEquals(1.0f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertId(g1, lossyForId, e, 10);
            } else {
                fail("Edge not expected");
            }
        });

        final Vertex v6 = g1.traversal().V().has("name", "peter").next();
        assertEquals(35, v6.<Integer>value("age").intValue());
        assertEquals(2, v6.keys().size());
        assertEquals(assertSpecificLabel ? "person" : Vertex.DEFAULT_LABEL, v6.label());
        assertId(g1, lossyForId, v6, 6);

        final List<Edge> v6Edges = IteratorUtils.list(v6.edges(Direction.BOTH));
        assertEquals(1, v6Edges.size());
        v6Edges.forEach(e -> {
            if (e.inVertex().values("name").next().equals("lop")) {
                assertEquals("created", e.label());
                if (assertDouble)
                    assertEquals(0.2d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.2f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertId(g1, lossyForId, e, 12);
            } else {
                fail("Edge not expected");
            }
        });
    }

    private static void assertId(final Graph g, final boolean lossyForId, final Element e, final Object expected) {
        if (g.features().edge().supportsUserSuppliedIds()) {
            if (lossyForId)
                assertEquals(expected.toString(), e.id().toString());
            else
                assertEquals(expected, e.id());
        }
    }

    //copied from TinkerPop
    protected static <T> void checkResults(final List<T> expectedResults, final Traversal<?, T> traversal) {
        final List<T> results = traversal.toList();
        Assert.assertFalse(traversal.hasNext());
        if (expectedResults.size() != results.size()) {
            logger.error("Expected results: " + expectedResults);
            logger.error("Actual results:   " + results);
            Assert.assertEquals("Checking result size", expectedResults.size(), results.size());
        }

        for (T t : results) {
            if (t instanceof Map) {
                Assert.assertThat("Checking map result existence: " + t, expectedResults.stream().filter(e -> e instanceof Map).filter(e -> internalCheckMap((Map) e, (Map) t)).findAny().isPresent(), CoreMatchers.is(true));
            } else {
                Assert.assertThat("Checking result existence: " + t, expectedResults.contains(t), CoreMatchers.is(true));
            }
        }
        final Map<T, Long> expectedResultsCount = new HashMap<>();
        final Map<T, Long> resultsCount = new HashMap<>();
        Assert.assertEquals("Checking indexing is equivalent", expectedResultsCount.size(), resultsCount.size());
        expectedResults.forEach(t -> MapHelper.incr(expectedResultsCount, t, 1l));
        results.forEach(t -> MapHelper.incr(resultsCount, t, 1l));
        expectedResultsCount.forEach((k, v) -> Assert.assertEquals("Checking result group counts", v, resultsCount.get(k)));
        Assert.assertThat(traversal.hasNext(), CoreMatchers.is(false));
    }

    private static <A, B> boolean internalCheckMap(final Map<A, B> expectedMap, final Map<A, B> actualMap) {
        final List<Map.Entry<A, B>> actualList = actualMap.entrySet().stream().sorted((a, b) -> a.getKey().toString().compareTo(b.getKey().toString())).collect(Collectors.toList());
        final List<Map.Entry<A, B>> expectedList = expectedMap.entrySet().stream().sorted((a, b) -> a.getKey().toString().compareTo(b.getKey().toString())).collect(Collectors.toList());

        if (expectedList.size() != actualList.size()) {
            return false;
        }

        for (int i = 0; i < actualList.size(); i++) {
            if (!actualList.get(i).getKey().equals(expectedList.get(i).getKey())) {
                return false;
            }
            if (!actualList.get(i).getValue().equals(expectedList.get(i).getValue())) {
                return false;
            }
        }
        return true;
    }

    protected void assertStep(Step<?, ?> step, boolean isGraph, boolean isEagerLoad, boolean isForMultipleQueries, boolean comparatorsNotOnDb, boolean rangeOnDb) {
        if (isGraph) {
            Assert.assertTrue("Expected SqlgGraphStep, found " + step.getClass().getName(), step instanceof SqlgGraphStep);
        } else {
            Assert.assertTrue("Expected SqlgVertexStep, found " + step.getClass().getName(), step instanceof SqlgVertexStep);
        }
        SqlgStep sqlgStep = (SqlgStep) step;
        Assert.assertEquals("isEagerLoad should be " + isEagerLoad, isEagerLoad, sqlgStep.isEargerLoad());
        Assert.assertEquals("isForMultipleQueries should be " + isForMultipleQueries, isForMultipleQueries, sqlgStep.isForMultipleQueries());
        Assert.assertEquals("comparatorsNotOnDb should be " + comparatorsNotOnDb, comparatorsNotOnDb, sqlgStep.getReplacedSteps().stream().allMatch(r -> r.getDbComparators().isEmpty()));
        if (!rangeOnDb) {
            Assert.assertEquals("rangeOnDb should be " + rangeOnDb, rangeOnDb, sqlgStep.getReplacedSteps().get(sqlgStep.getReplacedSteps().size() - 1).getSqlgRangeHolder().isApplyOnDb());
        } else {
            //rangeOnDb is true even if there is no range
            ReplacedStep<?, ?> replacedStep = sqlgStep.getReplacedSteps().get(sqlgStep.getReplacedSteps().size() - 1);
            if (replacedStep.getSqlgRangeHolder() != null) {
                Assert.assertEquals("rangeOnDb should be " + rangeOnDb, rangeOnDb, replacedStep.getSqlgRangeHolder().isApplyOnDb());
            }
        }
    }

    protected void assertStep(Step<?, ?> step, boolean isGraph, boolean isEagerLoad, boolean isForMultipleQueries, boolean comparatorsNotOnDb) {
        if (isGraph) {
            Assert.assertTrue("Expected SqlgGraphStep, found " + step.getClass().getName(), step instanceof SqlgGraphStep);
        } else {
            Assert.assertTrue("Expected SqlgVertexStep, found " + step.getClass().getName(), step instanceof SqlgVertexStep);
        }
        SqlgStep sqlgStep = (SqlgStep) step;
        Assert.assertEquals("isEagerLoad should be " + isEagerLoad, isEagerLoad, sqlgStep.isEargerLoad());
        Assert.assertEquals("isForMultipleQueries should be " + isForMultipleQueries, isForMultipleQueries, sqlgStep.isForMultipleQueries());
        Assert.assertEquals("comparatorsNotOnDb should be " + comparatorsNotOnDb, comparatorsNotOnDb, sqlgStep.getReplacedSteps().stream().allMatch(r -> r.getDbComparators().isEmpty()));
    }

    protected void assertStep(Step<?, ?> step, boolean isGraph, boolean isEagerLoad, boolean comparatorsNotOnDb) {
        if (isGraph) {
            Assert.assertTrue("Expected SqlgGraphStep, found " + step.getClass().getName(), step instanceof SqlgGraphStep);
        } else {
            Assert.assertTrue("Expected SqlgVertexStep, found " + step.getClass().getName(), step instanceof SqlgVertexStep);
        }
        SqlgStep sqlgStep = (SqlgStep) step;
        Assert.assertEquals("isEagerLoad should be " + isEagerLoad, isEagerLoad, sqlgStep.isEargerLoad());
        Assert.assertEquals("comparatorsNotOnDb should be " + comparatorsNotOnDb, comparatorsNotOnDb, sqlgStep.getReplacedSteps().stream().allMatch(r -> r.getDbComparators().isEmpty()));
    }

    public <A, B> List<Map<A, B>> makeMapList(final int size, final Object... keyValues) {
        final List<Map<A, B>> mapList = new ArrayList<>();
        for (int i = 0; i < keyValues.length; i = i + (2 * size)) {
            final Map<A, B> map = new HashMap<>();
            for (int j = 0; j < (2 * size); j = j + 2) {
                map.put((A) keyValues[i + j], (B) keyValues[i + j + 1]);
            }
            mapList.add(map);
        }
        return mapList;
    }
}
