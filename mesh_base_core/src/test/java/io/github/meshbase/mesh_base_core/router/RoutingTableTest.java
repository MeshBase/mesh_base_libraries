package io.github.meshbase.mesh_base_core.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandler;
import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandlerListener;
import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandlersEnum;
import io.github.meshbase.mesh_base_core.global_interfaces.Device;
import io.github.meshbase.mesh_base_core.global_interfaces.SendError;
import io.github.meshbase.mesh_base_core.router.AckMessageBody;
import io.github.meshbase.mesh_base_core.router.ConcreteMeshProtocol;
import io.github.meshbase.mesh_base_core.router.MeshProtocol;
import io.github.meshbase.mesh_base_core.router.MeshRouter;
import io.github.meshbase.mesh_base_core.router.ProtocolType;
import io.github.meshbase.mesh_base_core.router.Router;
import io.github.meshbase.mesh_base_core.router.SendListener;
import io.github.meshbase.mesh_base_core.router.SendMessageBody;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class RoutingTableTest {
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
        long futureTime = System.currentTimeMillis() + 60_000;
        routingTable.addRoute(dest1, hop1, 2, futureTime);

        Optional<MeshRouter.RouteEntry> route = routingTable.getRoute(dest1);
        assertTrue(route.isPresent());
        assertEquals(hop1, route.get().nextHop);
        assertEquals(2, route.get().cost);
        assertEquals(futureTime, route.get().expiresAt);
    }
//
/*    @Test
    public void testRemoveRoute() {
        long futureTime = System.currentTimeMillis() + 60_000;
        routingTable.addRoute(dest1, hop1, 2, futureTime);
        routingTable.removeRoute(dest1);

        assertFalse(routingTable.getRoute(dest1).isPresent());
    }

    @Test
    public void testCleanExpiredRoutes() {
        long now = System.currentTimeMillis();
        UUID expiredDest = UUID.randomUUID();
        UUID validDest = UUID.randomUUID();

        routingTable.addRoute(expiredDest, hop1, 3, now - 10_000);
        routingTable.addRoute(validDest, hop1, 3, now + 10_000);


        assertFalse(routingTable.getRoute(expiredDest).isPresent());
        assertTrue(routingTable.getRoute(validDest).isPresent());
    }*/
}
