package com.example.mesh_base.ble

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mesh_base.global_interfaces.ConnectionHandlersEnum
import com.example.mesh_base.global_interfaces.Device
import com.example.mesh_base.global_interfaces.SendError
import com.example.mesh_base.mesh_manager.MeshManager
import com.example.mesh_base.mesh_manager.MeshManagerListener
import com.example.mesh_base.mesh_manager.Status
import com.example.mesh_base.router.ConcreteMeshProtocol
import com.example.mesh_base.router.MeshProtocol
import com.example.mesh_base.router.ProtocolType
import com.example.mesh_base.router.SendListener
import com.example.mesh_base.router.SendMessageBody
import com.example.mesh_base.ui.theme.MeshBaseTheme


@Composable
fun BleTestScreen(meshManager: MeshManager) {

    MeshBaseTheme {
        val TAG = "my_ble_screen"
        val context = LocalContext.current
        val connectedDevices = remember { mutableStateListOf<Device>() }
        var isOn by remember { mutableStateOf(false) }
        var bleIsOn by remember { mutableStateOf(false) }
        var wifiDirectIsOn by remember { mutableStateOf(false) }
        val id = meshManager.id.toString()


        var message by remember { mutableStateOf("") }


        LaunchedEffect(Unit) {

            val listener = object : MeshManagerListener() {
                override fun onDataReceivedForSelf(protocol: MeshProtocol<*>) {
                    Log.d(TAG, "received data")
                    if (protocol.byteType === ProtocolType.SEND_MESSAGE) {
                        @Suppress("UNCHECKED_CAST")
                        val sendProtocol = protocol as MeshProtocol<SendMessageBody>

                        val response: MeshProtocol<SendMessageBody> = ConcreteMeshProtocol(
                            1, -1, protocol.messageId, meshManager.id, protocol.sender,
                            SendMessageBody(4, false, "a reply to ${sendProtocol.body.msg}")
                        )

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "Responding with mid=" + protocol.messageId + " by saying " + response.body.msg,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        val listener = object : SendListener {
                            override fun onError(error: SendError) {
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(
                                        context,
                                        "error when Responding to =${protocol.sender} " + error.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onAck() {
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(
                                        context,
                                        "response saying " + response.body.msg + " was acked",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onResponse(res: MeshProtocol<*>?) {
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(
                                        context,
                                        "(should never happen) response to response for ${protocol.sender} was received",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        }
                        meshManager.send(response, listener, true)

                    } else {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "Received: data \nfrom device with uuid=${protocol.sender}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onStatusChange(status: Status) {
                    isOn = status.isOn
                    bleIsOn =
                        status.connectionStatuses[ConnectionHandlersEnum.BLE]?.isOn == true
                    //TODO: update value when WifiDirect is implemented
                    wifiDirectIsOn = false

                }

                override fun onNeighborConnected(device: Device) {
                    connectedDevices.add(device)
                }

                override fun onNeighborDisconnected(device: Device?) {
                    connectedDevices.remove(device)
                }

                override fun onError(e: Exception) {
                    Handler(Looper.getMainLooper()).post({
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    })
                }

            }

            meshManager.subscribe(listener)
            meshManager.on()
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("BleIsOn=$bleIsOn WifiDirectIsOn=$wifiDirectIsOn UUID=$id")

                Button(onClick = {
                    if (isOn) meshManager.off()
                    else meshManager.on()
                }) {
                    Text(if (isOn) "Turn Off" else "Turn On")
                }



                LazyColumn {
                    items(connectedDevices) { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${device.name ?: "Unknown"} (${device.uuid})",
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = {

                                val protocol: MeshProtocol<SendMessageBody> =
                                    ConcreteMeshProtocol<SendMessageBody>(
                                        1,
                                        4,
                                        113,
                                        meshManager.id,
                                        device.uuid,
                                        SendMessageBody(
                                            4,
                                            false,
                                            message,
                                        )
                                    )

                                val listener = object : SendListener {
                                    override fun onError(error: SendError) {
                                        Handler(Looper.getMainLooper()).post({
                                            Toast.makeText(
                                                context,
                                                "Send error: ${protocol.body.msg} ${error.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        })
                                    }

                                    override fun onAck() {

                                        Handler(Looper.getMainLooper()).post({
                                            Toast.makeText(
                                                context,
                                                "Ack received for message=${protocol.body.msg}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        })
                                    }

                                    //TODO: consider adding a type argument to onResponse on SendListener
                                    override fun onResponse(protocol: MeshProtocol<*>) {

                                        @Suppress("UNCHECKED_CAST")
                                        val response = protocol as MeshProtocol<SendMessageBody>
                                        //No response expected for SendMessageBody
                                        Handler(Looper.getMainLooper()).post({
                                            Toast.makeText(
                                                context,
                                                "response received=" + response.body.msg,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        })
                                    }
                                }

                                meshManager.send(protocol, listener, false)
                            }) {
                                Text("Send")
                            }
                        }
                    }
                }


                TextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Enter message") },
                    modifier = Modifier.fillMaxWidth()
                )

            }
        }
    }
}
