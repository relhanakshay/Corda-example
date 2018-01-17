package com.example.flow

import com.example.state.TokenProperties
import net.corda.core.crypto.DigitalSignature
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

interface TokenFlowCommand

sealed class TokenIssueFlowCommand {
  /**
   * Command sent to accountant to create an account
   * @param toAccountId the account that we are issuing the tokens into.
   * @param receiverCommonName the legal entity common name the node of where the account is (hopefully)
   */

  @CordaSerializable
  class TokenIssueCommand(
    val toAccountId: String,
    val receiverCommonName: String,
    val notaryCommonName: String,
    val tokenProperties: TokenProperties
  ) : TokenIssueFlowCommand(), TokenFlowCommand
}

sealed class TokenMoveFlowCommand {

  @CordaSerializable
  class TokenMoveCommand(
    val fromAccountId: String,
    val toAccountId: String,
    val receiverCommonName : String,
    val amount : Int,
    val symbol : String
  ) : TokenMoveFlowCommand(), TokenFlowCommand

}
