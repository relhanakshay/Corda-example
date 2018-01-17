package com.example.contract

import com.example.state.TokenProperties
import com.example.state.TokenState
import net.corda.core.crypto.generateKeyPair
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.ledger
import org.junit.Test


class TokenContractTests{

  val centralBankOfCieloNacioKeys = generateKeyPair()
  val centralBankOfCieloNacio = Party(CordaX500Name("Central Bank of Cielo Nacio", "Cielo Nacio Bank", "Sector 1", "CN"),centralBankOfCieloNacioKeys.public)

  val sector2BankKeys = generateKeyPair()
  val sector2Bank = Party(CordaX500Name("Sector 2 Bank of Cielo Nacio", "Sector 2 Bank", "Sector 2", "CN"),sector2BankKeys.public)

  val xhoTokenId1 = TokenProperties( 1000, "XHO", "cielo-nacio native currency helio")

  val xhoTokenAStateId1 = TokenState(xhoTokenId1, centralBankOfCieloNacio, "citizenSmith00000001")
  val xhoTokenAStateId1NewOwnerInternal = TokenState(xhoTokenId1, centralBankOfCieloNacio, "citizenKane00000001")
  val xhoTokenAStateId1NewOwnerExternal = TokenState(xhoTokenId1, sector2Bank, "citizenKane00000001")


  @Test
  fun `you can create a token` (){

    ledger{
      transaction {
        attachment(TokenContract.TOKEN_CONTRACT_ID)
        output(TokenContract.TOKEN_CONTRACT_ID) { xhoTokenAStateId1 }
        command (centralBankOfCieloNacio.owningKey){ TokenCommands.Create()}
        this.verifies()
      }
    }
  }

  @Test
  fun `you can move a token internally` (){

    ledger{
      transaction {
        attachment(TokenContract.TOKEN_CONTRACT_ID)
        output(TokenContract.TOKEN_CONTRACT_ID,"TokenId1") { xhoTokenAStateId1 }
        command (centralBankOfCieloNacio.owningKey){ TokenCommands.Create()}
        verifies()
      }

      transaction {
        attachment(TokenContract.TOKEN_CONTRACT_ID)
        command(centralBankOfCieloNacio.owningKey) { TokenCommands.Move()}
        input("TokenId1")
        output(TokenContract.TOKEN_CONTRACT_ID){ xhoTokenAStateId1NewOwnerInternal }
        verifies()
      }
    }
  }

  @Test
  fun `you can move a token externally` (){

    ledger{
      transaction {
        attachment(TokenContract.TOKEN_CONTRACT_ID)
        output(TokenContract.TOKEN_CONTRACT_ID,"TokenId1") { xhoTokenAStateId1 }
        command (centralBankOfCieloNacio.owningKey){ TokenCommands.Create()}
        verifies()
      }

      transaction {
        attachment(TokenContract.TOKEN_CONTRACT_ID)
        command(centralBankOfCieloNacio.owningKey) { TokenCommands.Move()}
        input("TokenId1")
        output(TokenContract.TOKEN_CONTRACT_ID){ xhoTokenAStateId1NewOwnerExternal }
        verifies()
      }
    }
  }

  @Test
  fun `you can't move token to the same account` (){

    ledger{
      transaction {
        attachment(TokenContract.TOKEN_CONTRACT_ID)
        output(TokenContract.TOKEN_CONTRACT_ID,"TokenId1") { xhoTokenAStateId1 }
        command (centralBankOfCieloNacio.owningKey){ TokenCommands.Create()}
        verifies()
      }

      transaction {
        attachment(TokenContract.TOKEN_CONTRACT_ID)
        command(centralBankOfCieloNacio.owningKey) { TokenCommands.Move()}
        input("TokenId1")
        output(TokenContract.TOKEN_CONTRACT_ID){ xhoTokenAStateId1 }
        `fails with`("account Ids are different")
      }
    }
  }

  @Test
  fun `you can burn a token` (){

    ledger{
      transaction {
        attachment(TokenContract.TOKEN_CONTRACT_ID)
        output(TokenContract.TOKEN_CONTRACT_ID,"TokenId1") { xhoTokenAStateId1 }
        command (centralBankOfCieloNacio.owningKey){ TokenCommands.Create()}
        verifies()
      }

      transaction {
        attachment(TokenContract.TOKEN_CONTRACT_ID)
        command(centralBankOfCieloNacio.owningKey) { TokenCommands.Burn()}
        input("TokenId1")
        verifies()
      }
    }
  }
}