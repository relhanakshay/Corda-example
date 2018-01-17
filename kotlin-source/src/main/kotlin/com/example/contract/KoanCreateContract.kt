package com.example.contract

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction


/*
In this Koan, you will get familiar with the contract for creating or issuing a  new state.
For the create command, there can be many parameter's but we have taken just the basic one's for the start.
The basic condition is -- there should not be any inout state and there should be 1 output state.

So, in this Koan, you have write code in the TODO()
for the count of input and output states in the transaction.
 */

open class KoanCreateContract : Contract{

    companion object {
        @JvmStatic
        val KOAN_CREATE_CONTRACT_ID = "com.example.contract.KoanCreateContract"
    }

    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<KoanCreateContract.Commands.Create>()
        requireThat {
            "there are no inputs" using (TODO())
            "there is 1 output" using (TODO())
        }
    }


    interface Commands : CommandData{
        class Create : Commands
    }

}


//tx.inputs.count() == 0

//tx.outputs.count() == 1