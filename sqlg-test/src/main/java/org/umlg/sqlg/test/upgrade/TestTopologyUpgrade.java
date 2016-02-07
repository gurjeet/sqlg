package org.umlg.sqlg.test.upgrade;

import org.apache.tinkerpop.gremlin.AbstractGremlinTest;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.GraphReader;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoIo;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoReader;
import org.junit.Assert;
import org.junit.Test;
import org.umlg.sqlg.structure.SqlgGraph;
import org.umlg.sqlg.test.BaseTest;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Date: 2016/02/06
 * Time: 6:17 PM
 */
public class TestTopologyUpgrade extends BaseTest {

    @Test
    public void testUpgrade() throws Exception {
        //with topology
        Vertex a1 = this.sqlgGraph.addVertex(T.label, "A", "name", "john");
        Object idA1 = a1.id();
        Vertex b1 = this.sqlgGraph.addVertex(T.label, "B", "name", "joe");
        Object idB1 = b1.id();
        a1.addEdge("knows", b1, "name", "hithere");
        this.sqlgGraph.tx().commit();

        //Delete the topology
        Connection conn = this.sqlgGraph.tx().getConnection();
        Statement statement = conn.createStatement();
        statement.execute("DROP SCHEMA sqlg_schema CASCADE");
        statement.close();
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.close();

        //topology will be recreated
        SqlgGraph sqlgGraph1 = SqlgGraph.open(configuration);
        Assert.assertEquals(2, sqlgGraph1.traversal().V().count().next().intValue());
        Assert.assertEquals(1, sqlgGraph1.traversal().E().count().next().intValue());
        Assert.assertTrue(sqlgGraph1.traversal().V().hasLabel("A").hasNext());
        Assert.assertTrue(sqlgGraph1.traversal().V().hasLabel("B").hasNext());
        Assert.assertEquals(1, sqlgGraph1.traversal().V().hasLabel("A").count().next().intValue());
        Assert.assertEquals(1, sqlgGraph1.traversal().V().hasLabel("B").count().next().intValue());
        Vertex a = sqlgGraph1.traversal().V().hasLabel("A").next();
        Assert.assertEquals(idA1, a.id());
        Vertex b = sqlgGraph1.traversal().V().hasLabel("B").next();
        Assert.assertEquals(idB1, b.id());
        Assert.assertEquals(1, sqlgGraph1.traversal().V(a).out("knows").count().next().intValue());
        Assert.assertEquals(b, sqlgGraph1.traversal().V(a).out("knows").next());
        Assert.assertEquals(1, sqlgGraph1.traversal().V(b).in("knows").count().next().intValue());
        Assert.assertEquals(a, sqlgGraph1.traversal().V(b).in("knows").next());
        Assert.assertEquals(1, sqlgGraph1.traversal().V(a).properties("name").count().next().intValue());
        Assert.assertTrue(sqlgGraph1.traversal().V(a).properties("name").next().isPresent());
        Assert.assertEquals("john", sqlgGraph1.traversal().V(a).properties("name").next().value());
        Assert.assertEquals(1, sqlgGraph1.traversal().V(a).outE("knows").properties("name").count().next().intValue());
        Assert.assertTrue(sqlgGraph1.traversal().V(a).outE("knows").properties("name").next().isPresent());
        Assert.assertEquals("hithere", sqlgGraph1.traversal().V(a).outE("knows").properties("name").next().value());
        sqlgGraph1.close();

        //from topology
        sqlgGraph1 = SqlgGraph.open(configuration);
        Assert.assertEquals(2, sqlgGraph1.traversal().V().count().next().intValue());
        Assert.assertEquals(1, sqlgGraph1.traversal().E().count().next().intValue());
        Assert.assertTrue(sqlgGraph1.traversal().V().hasLabel("A").hasNext());
        Assert.assertTrue(sqlgGraph1.traversal().V().hasLabel("B").hasNext());
        Assert.assertEquals(1, sqlgGraph1.traversal().V().hasLabel("A").count().next().intValue());
        Assert.assertEquals(1, sqlgGraph1.traversal().V().hasLabel("B").count().next().intValue());
        a = sqlgGraph1.traversal().V().hasLabel("A").next();
        Assert.assertEquals(idA1, a.id());
        b = sqlgGraph1.traversal().V().hasLabel("B").next();
        Assert.assertEquals(idB1, b.id());
        Assert.assertEquals(1, sqlgGraph1.traversal().V(a).out("knows").count().next().intValue());
        Assert.assertEquals(b, sqlgGraph1.traversal().V(a).out("knows").next());
        Assert.assertEquals(1, sqlgGraph1.traversal().V(b).in("knows").count().next().intValue());
        Assert.assertEquals(a, sqlgGraph1.traversal().V(b).in("knows").next());
        Assert.assertEquals(1, sqlgGraph1.traversal().V(a).properties("name").count().next().intValue());
        Assert.assertTrue(sqlgGraph1.traversal().V(a).properties("name").next().isPresent());
        Assert.assertEquals("john", sqlgGraph1.traversal().V(a).properties("name").next().value());
        Assert.assertEquals(1, sqlgGraph1.traversal().V(a).outE("knows").properties("name").count().next().intValue());
        Assert.assertTrue(sqlgGraph1.traversal().V(a).outE("knows").properties("name").next().isPresent());
        Assert.assertEquals("hithere", sqlgGraph1.traversal().V(a).outE("knows").properties("name").next().value());
        sqlgGraph1.close();
    }

    @Test
    public void testGratefulDeadDBUpgrade() throws Exception {
        Graph g = this.sqlgGraph;
        GraphReader reader = GryoReader.build()
                .mapper(g.io(GryoIo.build()).mapper().create())
                .create();
        try (final InputStream stream = AbstractGremlinTest.class.getResourceAsStream("/grateful-dead.kryo")) {
            reader.readGraph(stream, g);
        }
        Traversal<Vertex, Long> traversal = get_g_V_both_both_count(g.traversal());
        Assert.assertEquals(new Long(1406914), traversal.next());
        Assert.assertFalse(traversal.hasNext());

        //Delete the topology
        Connection conn = this.sqlgGraph.tx().getConnection();
        Statement statement = conn.createStatement();
        statement.execute("DROP SCHEMA sqlg_schema CASCADE");
        statement.close();
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.close();

        SqlgGraph sqlgGraph1 = SqlgGraph.open(configuration);
        sqlgGraph1.tx().normalBatchModeOn();
        sqlgGraph1.traversal().V().forEachRemaining(Element::remove);
        sqlgGraph1.tx().commit();
        g = sqlgGraph1;
        reader = GryoReader.build()
                .mapper(g.io(GryoIo.build()).mapper().create())
                .create();
        try (final InputStream stream = AbstractGremlinTest.class.getResourceAsStream("/grateful-dead.kryo")) {
            reader.readGraph(stream, g);
        }
        traversal = get_g_V_both_both_count(g.traversal());
        Assert.assertEquals(new Long(1406914), traversal.next());
        Assert.assertFalse(traversal.hasNext());
        sqlgGraph1.close();
        sqlgGraph1 = SqlgGraph.open(configuration);
        sqlgGraph1.tx().normalBatchModeOn();
        sqlgGraph1.traversal().V().forEachRemaining(Element::remove);
        sqlgGraph1.tx().commit();
        g = sqlgGraph1;
        reader = GryoReader.build()
                .mapper(g.io(GryoIo.build()).mapper().create())
                .create();
        try (final InputStream stream = AbstractGremlinTest.class.getResourceAsStream("/grateful-dead.kryo")) {
            reader.readGraph(stream, g);
        }
        traversal = get_g_V_both_both_count(g.traversal());
        Assert.assertEquals(new Long(1406914), traversal.next());
        Assert.assertFalse(traversal.hasNext());
        sqlgGraph1.close();
    }

    @Test
    public void testModernGraph() throws Exception {
        loadModern();
        Traversal<Vertex, Long> traversal = this.sqlgGraph.traversal().V().both().both().count();
        Assert.assertEquals(new Long(30), traversal.next());
        Assert.assertFalse(traversal.hasNext());
        this.sqlgGraph.traversal().V().forEachRemaining(Element::remove);
        this.sqlgGraph.tx().commit();
        //Delete the topology
        Connection conn = this.sqlgGraph.tx().getConnection();
        Statement statement = conn.createStatement();
        statement.execute("DROP SCHEMA sqlg_schema CASCADE");
        statement.close();
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.close();
        SqlgGraph sqlgGraph1 = SqlgGraph.open(configuration);
        loadModern(sqlgGraph1);
        traversal = sqlgGraph1.traversal().V().both().both().count();
        Assert.assertEquals(new Long(30), traversal.next());
        Assert.assertFalse(traversal.hasNext());
        sqlgGraph1.close();
    }

    public Traversal<Vertex, Long> get_g_V_both_both_count(GraphTraversalSource g) {
        return g.V().both().both().count();
    }
}
