package io.github.meshbase.mesh_base_core.mesh_manager;

import android.app.Activity;
import android.util.Log;

import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandler;
import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandlerFactory;
import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandlerListener;
import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandlersEnum;
import io.github.meshbase.mesh_base_core.global_interfaces.Device;
import io.github.meshbase.mesh_base_core.router.MeshProtocol;
import io.github.meshbase.mesh_base_core.router.MeshRouter;
import io.github.meshbase.mesh_base_core.router.ProtocolType;
import io.github.meshbase.mesh_base_core.router.Router;
import io.github.meshbase.mesh_base_core.router.SendListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

//TODO: unit test once BLE and WifiDirect have the same interfaces
public class MeshManager {
    protected final List<MeshManagerListener> listeners = new CopyOnWriteArrayList<>();
    private final HashMap<ConnectionHandlersEnum, ConnectionHandler> connectionHandlers = new HashMap<>();
    //TODO: store uuid in local storage so that the devices address is consistent
    private final UUID id;
    private final MeshRouter router;
    private final String TAG = "my_meshManager";
    private boolean isOn = false;
    private Activity activity;

    public MeshManager(Activity context) {
        this.activity = context;
        Store store = Store.getInstance(context);
        if (store.getId() == null) {
            UUID newId = UUID.randomUUID();
            store.storeId(newId);
            Log.d(TAG, "MeshManager: Generated new ID: " + newId);
        } else {
            Log.d(TAG, "MeshManager: Retrieved existing ID: " + store.getId());
        }
        id = store.getId();

        ConnectionHandlerFactory factory = new ConnectionHandlerFactory();
        Log.d(TAG, "MeshManager: Bootstrapping connection handlers...");

        // TODO: Check for user connection handler preference
        for (ConnectionHandlersEnum _enum : ConnectionHandlersEnum.values()) {
            ConnectionHandler connectionHandler = factory.createConnectionHandler(_enum, context, id);
            Log.d(TAG, "MeshManager: Created connection handler for " + _enum);
            connectionHandlers.put(_enum, connectionHandler);
            connectionHandler.subscribe(
                    new ConnectionHandlerListener() {
                        @Override
                        public void onNeighborConnected(Device device) {
                            Log.d(TAG, "MeshManager: Neighbor connected: " + device);
                            for (MeshManagerListener listener : listeners) {
                                activity.runOnUiThread(()-> listener.onNeighborConnected(device) );
                            }
                        }

                        @Override
                        public void onNeighborDisconnected(Device device) {
                            Log.d(TAG, "MeshManager: Neighbor disconnected: " + device);
                            for (MeshManagerListener listener : listeners) {
                                activity.runOnUiThread(()-> listener.onNeighborDisconnected(device) );
                            }
                        }

                        @Override
                        public void onDisconnected() {
                            Log.d(TAG, "MeshManager: Disconnected");
                            Status status = getStatus();
                            for (MeshManagerListener listener : listeners) {
                                activity.runOnUiThread(()-> listener.onStatusChange(status) );
                            }
                        }

                        @Override
                        public void onConnected() {
                            Log.d(TAG, "MeshManager: Connected");
                            Status status = getStatus();
                            for (MeshManagerListener listener : listeners) {
                                activity.runOnUiThread(()->listener.onStatusChange(status));
                            }
                        }
                    }
            );
            Log.d(TAG, "MeshManager: Subscribed to connection handler for " + _enum);
        }

        Log.d(TAG, "MeshManager: Completed Bootstrapping connection handlers!");

        Log.d(TAG, "MeshManager: Setting up Router...");

        HashSet<ProtocolType> typesExpectingResponses = new HashSet<>();
        //TODO: implement ProtocolType.Receive_Message as a response type, but for now, use SENd_MESSAGE itself
        typesExpectingResponses.add(ProtocolType.SEND_MESSAGE);
        typesExpectingResponses.add(ProtocolType.RAW_BYTES_MESSAGE);
        router = new MeshRouter(connectionHandlers, id, typesExpectingResponses);

        router.setListener(new Router.RouterListener() {
            @Override
            public void onData(MeshProtocol<?> protocol, Device neighbor) {
                Log.d(TAG, "MeshManager: Data received from router, neighbor: " + neighbor);
                for (MeshManagerListener listener : listeners) {
                    activity.runOnUiThread(()-> listener.onDataReceivedForSelf(protocol) );
                }
            }

            @Override
            public void onError(Exception exception) {
                for (MeshManagerListener listener : listeners) {
                    activity.runOnUiThread(()-> listener.onError(exception) );
                }
            }
        });

        Log.d(TAG, "MeshManager: Router set up.");
    }

    public UUID getId() {
        return id;
    }

    public void subscribe(MeshManagerListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "MeshManager: Listener subscribed");
        }
    }

    public void unsubscribe(MeshManagerListener listener) {
        if (listeners.remove(listener)) {
            Log.d(TAG, "MeshManager: Listener unsubscribed");
        }
    }

    public void on() {
        Log.d(TAG, "MeshManager: Turning on mesh");
        for (ConnectionHandler connectionHandler : connectionHandlers.values()) {
            connectionHandler.enable();
        }
        isOn = true;
        Status status = getStatus();
        for (MeshManagerListener listener : listeners) {
            activity.runOnUiThread(()-> listener.onStatusChange(status) );
        }
        Log.d(TAG, "MeshManager: Mesh turned on, notified #"+listeners.size()+" listeners");
    }

    public void onPermissionResult(int requestCode){
        for (ConnectionHandler helper : connectionHandlers.values()) {
            helper.onPermissionResult(requestCode);
        }
    }

    public void off() {
        Log.d(TAG, "MeshManager: Turning off mesh");
        for (ConnectionHandler helper : connectionHandlers.values()) {
            helper.stop();
        }
        isOn = false;
        Status status = getStatus();
        for (MeshManagerListener listener : listeners) {
            activity.runOnUiThread(()-> listener.onStatusChange(status) );
        }
        Log.d(TAG, "MeshManager: Mesh turned off, notified listeners");
    }

    public ArrayList<Device> getNeighbors() {
        ArrayList<Device> neighbors = new ArrayList<Device>();
        for (ConnectionHandler helper : connectionHandlers.values()) {
            neighbors.addAll(helper.getNeighbourDevices());
        }
        return neighbors;
    }

    public Status getStatus() {
        HashMap<ConnectionHandlersEnum, Status.Property> _status = new HashMap<>() {
            {
                for (HashMap.Entry<ConnectionHandlersEnum, ConnectionHandler> entry : connectionHandlers.entrySet()) {
                    ConnectionHandlersEnum key = entry.getKey();
                    ConnectionHandler connectionHandler = entry.getValue();
                    put(key, new Status.Property(connectionHandler.isEnabled(), connectionHandler.isOn(), connectionHandler.isSupported()));
                }
            }
        };
        return new Status(isOn, _status);
    }

    public void send(MeshProtocol<?> protocol, SendListener listener, boolean keepMessageId) {
        Log.d(TAG, "MeshManager: Sending data with protocol" + protocol.getClass().getSimpleName());
        router.sendData(protocol, listener, keepMessageId);
    }

    private void clearListeners() {
        listeners.clear();
        Log.d(TAG, "MeshManager: All listeners cleared");
    }

}
