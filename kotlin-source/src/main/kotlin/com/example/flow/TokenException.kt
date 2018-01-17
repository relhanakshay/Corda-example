package com.example.flow

import net.corda.core.flows.FlowException
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class TokenException(message: String, cause: Throwable? = null) : FlowException(message, cause)

