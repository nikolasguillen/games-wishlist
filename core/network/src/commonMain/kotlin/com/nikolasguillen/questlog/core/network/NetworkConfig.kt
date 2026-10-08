package com.nikolasguillen.questlog.core.network

/**
 * What the app decides about the network layer.
 *
 * @property logBodies whether request and response bodies are written to the log. True only on debug builds;
 * the `Authorization` and `Client-ID` headers are masked either way.
 */
class NetworkConfig(val logBodies: Boolean)
