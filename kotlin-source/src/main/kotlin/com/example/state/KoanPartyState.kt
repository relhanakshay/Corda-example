package com.example.state

import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

/*

Let's start with Corda Koans. We will start by making a state in steps.

1. 1st step is to have a party in our state who can be identified and can sign transaction using its public key.
   Let's include an [owner] of type Party in our KoanOwnableParty.

2. Use Ownable State for Implementation.
   Ownable state is used when ---- A contract state that can have a single owner.

 */


data class KoanPartyState(override val owner : Party) : OwnableState{

    override val participants: List<AbstractParty>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}