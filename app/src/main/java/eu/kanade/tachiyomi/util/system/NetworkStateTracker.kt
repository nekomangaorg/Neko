package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

data class NetworkState(
    val isConnected: Boolean,
    val isValidated: Boolean,
    val isUnmetered: Boolean,
) {
    val isOnline = isConnected && isValidated
}

fun Context.activeNetworkState(): NetworkState {
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return NetworkState(
        isConnected =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
        isValidated =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false,
        isUnmetered =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ?: false,
    )
}

fun Context.networkStateFlow() = callbackFlow {
    val networkCallback =
        object : NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(activeNetworkState())
            }

            override fun onLost(network: Network) {
                trySend(activeNetworkState())
            }
        }

    connectivityManager.registerDefaultNetworkCallback(networkCallback)
    trySend(activeNetworkState())
    awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }
}
