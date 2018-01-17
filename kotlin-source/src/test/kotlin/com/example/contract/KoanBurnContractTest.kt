package com.example.contract

import net.corda.testing.ledger
import org.junit.Test
import com.example.contract.KoanBurnContract.Companion.KOAN_BURN_CONTRACT_ID
import com.example.state.KoanPartyState
import net.corda.core.crypto.generateKeyPair
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import com.example.contract.KoanCreateContract

class KoanBurnContractTest{

    val centralBankKeys = generateKeyPair()
    val centralBank = Party(CordaX500Name("Central Bank", "Bank", "Sector 1", "IN"),centralBankKeys.public)


    val StateId1 = KoanPartyState(centralBank)

    @Test
    fun `state must have no output`() {
        ledger{
            transaction {
                attachment(KoanBurnContract.KOAN_BURN_CONTRACT_ID)
                output(KoanBurnContract.KOAN_BURN_CONTRACT_ID,"TokenId1") { StateId1 }
                command (centralBank.owningKey) { KoanBurnContract.Commands.Create()}
                verifies()
            }

            transaction {
                attachment(KoanBurnContract.KOAN_BURN_CONTRACT_ID)
                command(centralBank.owningKey) { KoanBurnContract.Commands.Burn()}
                input("TokenId1")
                verifies()
            }
        }
    }

}