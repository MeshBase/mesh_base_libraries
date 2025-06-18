package io.github.meshbase.mesh_base_core;

import io.github.meshbase.mesh_base_core.mesh_logger.MeshLogger;
import io.github.meshbase.mesh_base_core.mesh_logger.MeshLoggerSingleton;
import io.github.meshbase.mesh_base_core.mesh_logger.TurnedOffInput;
import io.github.meshbase.mesh_base_core.mesh_logger.TurnedOnInput;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import okhttp3.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;

public class MeshLoggerTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final String BaseURI = "http://localhost:8000/event";

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testSendEvent_debugMode_sendsRequest() throws Exception {
        OkHttpClient mockClient = mock(OkHttpClient.class);
        Call mockCall = mock(Call.class);
        when(mockClient.newCall(any())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(new Response.Builder()
                .request(new Request.Builder().url(BaseURI).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("", MediaType.get("application/json")))
                .build());

        MeshLogger logger = new MeshLogger(BaseURI,
                Executors.newSingleThreadExecutor(), mockClient, true);

        logger.sendEvent(new TurnedOnInput("dev-1"));
        Thread.sleep(300); // give async time
        verify(mockClient, times(1)).newCall(any());
        verify(mockCall, times(1)).execute();

        logger.sendEvent(new TurnedOnInput("dev-1"));
        Thread.sleep(300); // give async time
        verify(mockClient, times(2)).newCall(any());
        verify(mockCall, times(2)).execute();
    }

    @Test
    public void testSendEvent_nonDebugMode_printsToConsole() {
        OkHttpClient mockClient = mock(OkHttpClient.class);

        MeshLogger logger = new MeshLogger(BaseURI,
                Executors.newSingleThreadExecutor(), mockClient, false);

        TurnedOffInput event = new TurnedOffInput("dev-2");
        logger.sendEvent(event);

        assertTrue(outContent.toString().contains("MeshLogger Debug Event"));
        assertTrue(outContent.toString().contains("dev-2"));
        assertTrue(outContent.toString().contains("TURNED_OFF"));
        verify(mockClient, times(0)).newCall(any());
    }

    @Test
    public void testSingletonInitAndReconfig() {
        MeshLoggerSingleton.configure("http://mocked.com/event", false);
        MeshLogger logger = MeshLoggerSingleton.get();

        assertNotNull(logger);

        logger.setDebug(true);
        assertTrue(logger.isDebugMode());

        logger.setDebug(false);
        assertFalse(logger.isDebugMode());
    }
}
