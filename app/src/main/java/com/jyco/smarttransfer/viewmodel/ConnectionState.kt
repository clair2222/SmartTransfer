package com.jyco.smarttransfer.viewmodel

enum class ConnectionState {
    Idle,
    Discovering,
    Ready,
    P2PConnecting,
    P2PConnected,
    SocketConnecting,
    SocketConnected,
    Authenticating,
    Authenticated,
    Transfer,
    Disconnected,
    Failed
}