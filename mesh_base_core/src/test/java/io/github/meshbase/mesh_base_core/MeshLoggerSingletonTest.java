package io.github.meshbase.mesh_base_core;

import org.junit.Before;
import org.junit.Test;

import io.github.meshbase.mesh_base_core.mesh_logger.MeshLogger;
import io.github.meshbase.mesh_base_core.mesh_logger.MeshLoggerSingleton;

import okhttp3.OkHttpClient;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class MeshLoggerSingletonTest {

    @Before
    public void resetSingletonBeforeEach() throws Exception {
        Field instanceField = MeshLoggerSingleton.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    public void testInstanceInitiallyNull() throws Exception {
        Field instanceField = MeshLoggerSingleton.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        assertNull(instanceField.get(null));
    }

    @Test
    public void testGetCreatesInstance() throws Exception {
        assertNotNull(MeshLoggerSingleton.get());
    }

    @Test
    public void testInstanceIsSetAfterGet() throws Exception {
        MeshLoggerSingleton.get();
        Field instanceField = MeshLoggerSingleton.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        assertNotNull(instanceField.get(null));
    }

    @Test
    public void testGetReturnsSameInstance() {
        MeshLogger first = MeshLoggerSingleton.get();
        MeshLogger second = MeshLoggerSingleton.get();
        assertSame(first, second);
    }

    @Test
    public void testConfigureCreatesInstanceWhenNull() throws Exception {
        MeshLoggerSingleton.configure("http://test.url", false);
        Field instanceField = MeshLoggerSingleton.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        assertNotNull(instanceField.get(null));
    }

    @Test
    public void testConfigureModifiesExistingInstance() throws Exception {
        MeshLogger original = new MeshLogger("url1", Executors.newSingleThreadExecutor(), new OkHttpClient(), true);
        Field instanceField = MeshLoggerSingleton.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, original);

        MeshLoggerSingleton.configure("ignored-url", false);
        assertFalse(original.isDebugMode());
    }
}
