package com.v2ray.ang.dto

import java.io.Serializable

data class TestServiceMessage(
    val key: Int,
    val subscriptionId: String = "",
    val serverGuids: List<String> = emptyList(),
    val onlyTcp: Boolean = false,
    /** Measure how long a fixed amount of data takes to download through each config. */
    val downloadTime: Boolean = false
) : Serializable

