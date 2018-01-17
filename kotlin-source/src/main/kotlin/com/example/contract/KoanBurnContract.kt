package com.example.contract

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

/*

Let's start with the easiest Contract which is Burn contract.
In this,

 */

open class KoanBurnContract : Contract{
    companion object {
        @JvmStatic
        val KOAN_BURN_CONTRACT_ID = "com.example.contract.KoanBurnContract"
    }

    /*
        As we know, In burn command, there is only 1 condition.
        That the count of outputs is 0.

        So, write a condition so that outputs.count() == 0

    */

    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Create -> {
                requireThat {
                    "there are no inputs" using (tx.inputs.count() == 0)
                    "there is one output" using (tx.outputs.count() == 1)
                }
            }
            is Commands.Burn -> {
                requireThat {
                    "there are no outputs" using (tx.outputs.count() == 0)
                    //"can only be burnt by ultimate issuer i.e. central bank" BART TO DO - how do you access config within the contract - i.e for central bank?
                }
            }
        }
    }


    interface Commands : CommandData {
        class Burn : Commands
        class Create : Commands
    }

}


