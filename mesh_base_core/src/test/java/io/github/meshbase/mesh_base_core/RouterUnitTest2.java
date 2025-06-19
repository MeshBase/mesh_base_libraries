package io.github.meshbase.mesh_base_core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.github.meshbase.mesh_base_core.router.MeshRouter;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Optional;
import java.util.UUID;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class RouterUnitTest2 {
    private MeshRouter.RoutingTable routingTable;
    private UUID dest1;
    private UUID hop1;

    @BeforeEach
    public void setup() {
        routingTable = new MeshRouter.RoutingTable();
        dest1 = UUID.randomUUID();
        hop1 = UUID.randomUUID();
    }

    @Test
    public void testAddAndGetRoute() {
        setup();

        long futureTime = System.currentTimeMillis() + 60_000;
        routingTable.addRoute(dest1, hop1, 2, futureTime);

        Optional<MeshRouter.RouteEntry> route = routingTable.getRoute(dest1);
        assertTrue(route.isPresent());
        assertEquals(hop1, route.get().nextHop);
        assertEquals(2, route.get().cost);
        assertEquals(futureTime, route.get().expiresAt);
    }

    //
    @Test
    public void testRemoveRoute() {
        setup();
        long futureTime = System.currentTimeMillis() + 60_000;
        routingTable.addRoute(dest1, hop1, 2, futureTime);
        routingTable.removeRoute(dest1);

        assertFalse(routingTable.getRoute(dest1).isPresent());
    }

    @Test
    public void testCleanExpiredRoutes() {
        setup();
        long now = System.currentTimeMillis();
        UUID expiredDest = UUID.randomUUID();
        UUID validDest = UUID.randomUUID();

        routingTable.addRoute(expiredDest, hop1, 3, now - 10_000);
        routingTable.addRoute(validDest, hop1, 3, now + 10_000);


        assertFalse(routingTable.getRoute(expiredDest).isPresent());
        assertTrue(routingTable.getRoute(validDest).isPresent());
    }
}
