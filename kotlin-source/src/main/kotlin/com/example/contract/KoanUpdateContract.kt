package com.example.contract

import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

open class KoanUpdateContract : Contract{

    companion object {
        @JvmStatic
        val KOAN_UPDATE_CONTRACT_ID = "com.example.contract.KoanUpdateContract"
    }


    override fun verify(tx: LedgerTransaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}