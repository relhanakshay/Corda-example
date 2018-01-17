package com.example.state

import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import java.util.*




data class KoanState(val symbol : String,
                     override val owner : AbstractParty,
                     val amount : Int,
                     val tokenId : String = UUID.randomUUID().toString()) : OwnableState {


    override val participants: List<AbstractParty>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.


    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}


