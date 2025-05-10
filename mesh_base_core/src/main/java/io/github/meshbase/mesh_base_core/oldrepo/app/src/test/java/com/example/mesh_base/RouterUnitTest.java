package com.example.mesh_base;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mesh_base.global_interfaces.ConnectionHandler;
import com.example.mesh_base.global_interfaces.ConnectionHandlerListener;
import com.example.mesh_base.global_interfaces.ConnectionHandlersEnum;
import com.example.mesh_base.global_interfaces.Device;
import com.example.mesh_base.global_interfaces.SendError;
import com.example.mesh_base.router.AckMessageBody;
import com.example.mesh_base.router.ConcreteMeshProtocol;
import com.example.mesh_base.router.MeshProtocol;
import com.example.mesh_base.router.ProtocolType;
import com.example.mesh_base.router.Router;
import com.example.mesh_base.router.SendListener;
import com.example.mesh_base.router.SendMessageBody;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class RouterUnitTest {

    @Test
    public void testSendCall_isOnAndNeighbors_callsSend() throws SendError {
        ConnectionHandler handler = mock(ConnectionHandler.class);
        ArrayList<Device> devices = createDevices(5);

        when(handler.isOn()).thenReturn(true);
        when(handler.getNeighbourDevices()).thenReturn(devices);

        UUID id = UUID.randomUUID();
        HashMap<ConnectionHandlersEnum, ConnectionHandler> handlers = new HashMap<>();
        handlers.put(ConnectionHandlersEnum.BLE, handler);
        Router router = new Router(handlers, id, new HashSet<>());
        MeshProtocol<SendMessageBody> protocol = new ConcreteMeshProtocol<>(1, -1, -1, id, devices.get(0).uuid, new SendMessageBody(4, false, "hello world"));

        //Perfect path
        router.sendData(protocol, mock(SendListener.class));
        verify(handler).send(any(byte[].class));

        //errors when no neighbor
        when(handler.getNeighbourDevices()).thenReturn(new ArrayList<>());
        SendListener listener = mock(SendListener.class);
        router.sendData(protocol, listener);
        verify(listener).onError(any(SendError.class));


        //errors when not on
        when(handler.getNeighbourDevices()).thenReturn(createDevices(3));
        when(handler.isOn()).thenReturn(false);
        SendListener listener2 = mock(SendListener.class);
        router.sendData(protocol, listener2);
        verify(listener2).onError(any(SendError.class));

    }

    @Test
    public void testAck_receivesData_respondsWithAck() throws SendError {
        ConnectionHandler handler = mock(ConnectionHandler.class);
        UUID senderId = UUID.randomUUID();
        Device sender = new Device(senderId, "sender") {
        };
        ArrayList<Device> devices = new ArrayList<>();
        devices.add(sender);

        when(handler.isOn()).thenReturn(true);
        when(handler.getNeighbourDevices()).thenReturn(devices);

        HashMap<ConnectionHandlersEnum, ConnectionHandler> handlers = new HashMap<>();
        handlers.put(ConnectionHandlersEnum.BLE, handler);

        UUID id = UUID.randomUUID();
        Router router = new Router(handlers, id, new HashSet<>());
        Router.RouterListener routerListener = mock(Router.RouterListener.class);
        router.setListener(routerListener);

        // capture connection handler listener
        ArgumentCaptor<ConnectionHandlerListener> listenerCaptor = ArgumentCaptor.forClass(ConnectionHandlerListener.class);
        verify(handler).subscribe(listenerCaptor.capture());
        ConnectionHandlerListener listener = listenerCaptor.getValue();

        // imitate receiving send request
        MeshProtocol<SendMessageBody> request = new ConcreteMeshProtocol<>(
                1, -1, -1, senderId, id, new SendMessageBody(4, false, "hello world")
        );
        listener.onDataReceived(sender, request.encode());

        // onData is called, ACK is sent
        verify(routerListener).onData(any(), any());

        ArgumentCaptor<byte[]> responseCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(handler).send(responseCaptor.capture());

        assertEquals(MeshProtocol.getByteType(responseCaptor.getValue()), ProtocolType.ACK);
    }

    @Test
    public void testAckAndResponse_receivesData_repliesWithResponseThenAck() throws SendError {
        ConnectionHandler handler = mock(ConnectionHandler.class);
        UUID senderId = UUID.randomUUID();
        Device sender = new Device(senderId, "sender") {
        };
        ArrayList<Device> devices = new ArrayList<>();
        devices.add(sender);

        when(handler.isOn()).thenReturn(true);
        when(handler.getNeighbourDevices()).thenReturn(devices);

        HashMap<ConnectionHandlersEnum, ConnectionHandler> handlers = new HashMap<>();
        handlers.put(ConnectionHandlersEnum.BLE, handler);

        UUID id = UUID.randomUUID();
        HashSet<ProtocolType> expectResponseTypes = new HashSet<>();
        expectResponseTypes.add(ProtocolType.SEND_MESSAGE); //send message expects a response now
        Router router = new Router(handlers, id, expectResponseTypes);
        Router.RouterListener routerListener = mock(Router.RouterListener.class);
        router.setListener(routerListener);

        // capture connection handler listener
        ArgumentCaptor<ConnectionHandlerListener> listenerCaptor = ArgumentCaptor.forClass(ConnectionHandlerListener.class);
        verify(handler).subscribe(listenerCaptor.capture());
        ConnectionHandlerListener listener = listenerCaptor.getValue();

        // imitate receiving send request
        MeshProtocol<SendMessageBody> request = new ConcreteMeshProtocol<>(
                1, -1, 123, senderId, id, new SendMessageBody(4, false, "request")
        );
        listener.onDataReceived(sender, request.encode());

        // onData is called, ACK is NOT sent
        verify(routerListener).onData(any(), any());
        verify(handler, never()).send(any());

        //response is sent
        MeshProtocol<SendMessageBody> response = new ConcreteMeshProtocol<>(
                1, -1, request.getMessageId(), id, senderId, new SendMessageBody(4, false, "response")
        );
        SendListener responseListener = mock(SendListener.class);
        router.sendData(response, responseListener, true);
        assertEquals(response.getMessageId(), request.getMessageId());

        //ack is received
        MeshProtocol<AckMessageBody> ack = new ConcreteMeshProtocol<>(0, -1, response.getMessageId(), senderId, id, new AckMessageBody("ok"));
        assertEquals(response.getMessageId(), ack.getMessageId());
        listener.onDataReceived(sender, ack.encode());

        // on ack should be called
        verify(responseListener).onAck();
    }


    private ArrayList<Device> createDevices(int count) {
        ArrayList<Device> devices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Device device = new Device(UUID.randomUUID(), "device" + i) {
            };
            devices.add(device);
        }

        return devices;
    }


}
