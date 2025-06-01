package io.github.thibaultbee.krtmp.rtmp.util

internal typealias NetStreamOnStatusLevel = String

internal const val NetStreamOnStatusLevelStatus: NetStreamOnStatusLevel = "status"
internal const val NetStreamOnStatusLevelError: NetStreamOnStatusLevel = "error"


internal typealias NetStreamOnStatusCode = String

internal const val NetStreamOnStatusCodeConnectSuccess: NetStreamOnStatusCode =
    "NetStream.Connect.Success"
internal const val NetStreamOnStatusCodeConnectClosed: NetStreamOnStatusCode =
    "NetStream.Connect.Closed"
internal const val NetStreamOnStatusCodeMuticastStreamReset: NetStreamOnStatusCode =
    "NetStream.MulticastStream.Reset"
internal const val NetStreamOnStatusCodePlayStart: NetStreamOnStatusCode =
    "NetStream.Play.Start"
internal const val NetStreamOnStatusCodePlayFailed: NetStreamOnStatusCode =
    "NetStream.Play.Failed"
internal const val NetStreamOnStatusCodePlayComplete: NetStreamOnStatusCode =
    "NetStream.Play.Complete"
internal const val NetStreamOnStatusCodePublishBadName: NetStreamOnStatusCode =
    "NetStream.Publish.BadName"
internal const val NetStreamOnStatusCodePublishFailed: NetStreamOnStatusCode =
    "NetStream.Publish.Failed"
internal const val NetStreamOnStatusCodePublishStart: NetStreamOnStatusCode =
    "NetStream.Publish.Start"
internal const val NetStreamOnStatusCodeUnpublishSuccess: NetStreamOnStatusCode =
    "NetStream.Unpublish.Success"


typealias NetConnectionConnectCode = String

internal const val NetConnectionConnectCodeSuccess: NetConnectionConnectCode =
    "NetConnection.Connect.Success"
internal const val NetConnectionConnectCodeFailed: NetConnectionConnectCode =
    "NetConnection.Connect.Failed"
internal const val NetConnectionConnectCodeClosed: NetConnectionConnectCode =
    "NetConnection.Connect.Closed"