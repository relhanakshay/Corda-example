package com.example.contract

import net.corda.core.crypto.generateKeyPair
import net.corda.testing.ledger
import org.junit.Test
import com.example.contract.KoanCreateContract.Companion.KOAN_CREATE_CONTRACT_ID
import com.example.state.KoanPartyState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party

class KoanCreateContractTest{

    val centralBankKeys = generateKeyPair()
    val centralBank = Party(CordaX500Name("Central Bank", "Bank", "Sector 1", "IN"),centralBankKeys.public)


    val StateId1 = KoanPartyState(centralBank)


    @Test
    fun `state must have 1 output and 0 input`() {
        ledger {
            transaction {
                attachment(KoanCreateContract.KOAN_CREATE_CONTRACT_ID)
                output(KoanCreateContract.KOAN_CREATE_CONTRACT_ID) { StateId1 }
                command(centralBank.owningKey) { KoanCreateContract.Commands.Create()}
                this.verifies()
            }
        }
    }

}