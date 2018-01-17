package com.example.contract

import com.example.state.TokenState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction

class TokenContract : Contract {

  companion object {
   val TOKEN_CONTRACT_ID: ContractClassName = TokenContract::class.java.canonicalName
  }

  override fun verify(tx: LedgerTransaction) {
    val command = tx.commands.requireSingleCommand<TokenCommands>()
    val groups = tx.groupStates { it : TokenState -> it.tokenProperties.symbol}

    for ((inputs, outputs, symbol) in groups) {


      when(command.value) {
        is TokenCommands.Create -> {
          requireThat {
            "there are no inputs" using (inputs.count()==0)
            "there are no duplicate tokens" using (outputs.count() == 1)
            "token id is not empty" using (outputs.all { it.tokenProperties.tokenId.isNotBlank()})
            "tokens are all the same" using (outputs.all {it.tokenProperties.symbol == symbol})
          }
        }
        is TokenCommands.Burn -> {
          requireThat {
            "there are no outputs" using(outputs.count() == 0)
            //"can only be burnt by ultimate issuer i.e. central bank" BART TO DO - how do you access config within the contract - i.e for central bank?
          }
        }
        is TokenCommands.Move -> {
          requireThat {
            "input accounts are all the same" using ( inputs.all{ it.accountId == inputs.first().accountId } )
            "output accounts are all the same" using ( outputs.all{ it.accountId == outputs.first().accountId } )
            "input and outputs balance" using ( inputs.sumBy { it.tokenProperties.amount } == outputs.sumBy { it.tokenProperties.amount } )
            "account Ids are different" using( inputs.first().accountId != outputs.first().accountId )

          }
        }
      }




    }
  }
}

