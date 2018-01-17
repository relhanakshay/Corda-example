package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.TokenCommands
import com.example.contract.TokenContract
import com.example.state.TokenProperties
import com.example.state.TokenState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

object TokenExchangeFlows {

  @InitiatingFlow
  @StartableByRPC
  class TokenSender(val otherParty: Party, val command: TokenFlowCommand) : FlowLogic<SignedTransaction>() {

    companion object {
      object CHECKING_BALANCE : Step("Check we have enough tokens")
      object BUILD_TOKEN_MOVE_TRANSACTION : Step("Build the transaction(s) to move the token between parties")
      object BUILD_TOKEN_ISSUE_TRANSACTION : Step("Build the transaction to issue the token")
      object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
      object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
      object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
        override fun childProgressTracker() = CollectSignaturesFlow.tracker()
      }
    }

    object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
      override fun childProgressTracker() = FinalityFlow.tracker()
    }

    fun tracker(): ProgressTracker {

      return when (command) {
        is TokenIssueFlowCommand.TokenIssueCommand -> {
          ProgressTracker(
            BUILD_TOKEN_ISSUE_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
          )
        }

        is TokenMoveFlowCommand.TokenMoveCommand -> {
          ProgressTracker(
            CHECKING_BALANCE,
            BUILD_TOKEN_MOVE_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
          )
        }
        else -> {
          ProgressTracker()
        }
      }
    }

    override val progressTracker = tracker()


    @Suspendable
    override fun call(): SignedTransaction {
      return when (command) {
        is TokenIssueFlowCommand.TokenIssueCommand -> issueToken(command)
        is TokenMoveFlowCommand.TokenMoveCommand -> moveToken(command)
        else -> { throw TokenException("Invalid Token Flow Command ${command.toString()}")}
      }
    }

    @Suspendable
    fun issueToken(issueTokenCommand: TokenIssueFlowCommand.TokenIssueCommand): SignedTransaction {

      val txBuilder = buildIssueTokenTransaction(issueTokenCommand)

        /*
        After completing the elements of Transaction Builder. We now need the signatures of parties involved.

        We will first verify and sign the transaction which has been completed till Transaction Builder.
         */

      val partiallySignedTx = verifyAndSignTransaction(txBuilder)

        /*
        After signing and verifying the transaction from our side, the transaction is partially as it needs to be signed by other nodes as well.

         */

      val fullySignedTx = getSignatureFromReceiver(partiallySignedTx)



      val finalisedTx = finaliseTransaction(fullySignedTx)
      return finalisedTx
    }

    @Suspendable
    fun moveToken(moveTokenCommand: TokenMoveFlowCommand.TokenMoveCommand): SignedTransaction {

      val txBuilders = buildMoveTokenTransaction(moveTokenCommand)

      var finalisedTxs = mutableListOf<SignedTransaction>()

      //TO DO - HACK WARNING - As we have to use multiple notaries we'll need multiple transactions - but we can only return one signed trans
      //Issue is raised for this here:
      // https://gitlab.com/cordite/cielonacio/issues/10

      txBuilders.forEach{

        val partiallySignedTx = verifyAndSignTransaction(it)
        val fullySignedTx = getSignatureFromReceiver(partiallySignedTx)
        val finalisedTx = finaliseTransaction(fullySignedTx)
        finalisedTxs.add(finalisedTx)
      }

      return finalisedTxs.first()
    }


    fun buildIssueTokenTransaction(issueTokenCommand: TokenIssueFlowCommand.TokenIssueCommand): TransactionBuilder {

      progressTracker.currentStep = BUILD_TOKEN_ISSUE_TRANSACTION

      serviceHub.networkMapCache.notaryIdentities.forEach { println("Notary Name${it.name.organisation}") }

        /*
        Here, we are building issue Token transaction through TransactionBuilder.
        In a TransactionBuilder, several things are included.
        1. Notary Identity
        2. Set of input states
        3. Set of output states
        4. Command indicating the intent of transaction
        5. A timestamp which can be optional
         */


        /*
        First, we are making a notary in the txBuilder present here.
        We will return the notary, from list of notaries, whose organisation name is same as issueTokenCommand.notaryCommonName
         */

      val notary = serviceHub.networkMapCache.notaryIdentities.first { it.name.organisation == issueTokenCommand.notaryCommonName }


        /*
        Now, we are giving the details of the state to Issue, basically we are giving a TokenState.
        In our issueTokenCommand, we have properties --
        1. val toAccountId: String,
        2. val receiverCommonName: String,
        3. val notaryCommonName: String,
        4. val tokenProperties: TokenProperties

        We will use here [issueTokenCommand.tokenProperties] , [otherParty] as the owner, [issueTokenCommand.toAccountId]
        as the accountId.
         */

      val tokenStateToIssue = TokenState(issueTokenCommand.tokenProperties, otherParty, issueTokenCommand.toAccountId)

        /*
        After this, we will give the command.
        Command is used to let know the intent of the transaction.
        Each command is also associated with a list of one or more signers. By taking the union of all the public keys
        listed in the commands, we get the list of the transaction’s required signers.
         */

      val txCommand = Command(TokenCommands.Create(), tokenStateToIssue.participants.map { it.owningKey })


        /*
        After we have specified the necessary fields required for the transaction builder.
        You can see the following line to see how everything get's combined and a transactioin builder is completed.
         */

      val txBuilder = TransactionBuilder(notary).withItems(StateAndContract(tokenStateToIssue, TokenContract.TOKEN_CONTRACT_ID), txCommand)



//it.name.organisation == issueTokenCommand.notaryCommonName


//issueTokenCommand.tokenProperties, otherParty, issueTokenCommand.toAccountId


//TokenCommands.Create(), tokenStateToIssue.participants.map { it.owningKey }


      return txBuilder
    }

    fun buildMoveTokenTransaction(moveTokenCommand: TokenMoveFlowCommand.TokenMoveCommand): List<TransactionBuilder> {

      val tokensToTransfer = getEnoughUnspentTokens(moveTokenCommand)

      progressTracker.currentStep = BUILD_TOKEN_MOVE_TRANSACTION

      val notaries = tokensToTransfer.distinctBy { it.state.notary }.map{ it -> it.state.notary }

      var txBuilders = mutableListOf<TransactionBuilder>()

      notaries.forEach {

        val notary = it

        val tokenInputStates = tokensToTransfer.filter { it.state.notary == notary }
        val totalToTransfer = tokenInputStates.sumBy { it.state.data.tokenProperties.amount }

        val tokenOutputProperties = TokenProperties(totalToTransfer, tokenInputStates.first().state.data.tokenProperties.symbol, tokenInputStates.first().state.data.tokenProperties.description)
        val tokenOutput = TokenState(tokenOutputProperties,otherParty, moveTokenCommand.toAccountId)

        val txCommand = Command(TokenCommands.Move(), tokenInputStates.flatMap { it.state.data.participants.map { it.owningKey } })

        val txBuilder = TransactionBuilder(notary).withItems(txCommand,StateAndContract(tokenOutput, TokenContract.TOKEN_CONTRACT_ID))
        tokenInputStates.forEach { txBuilder.addInputState(it)}

        txBuilders.add(txBuilder)

      }
      return txBuilders
    }

    fun getEnoughUnspentTokens(moveTokenCommand: TokenMoveFlowCommand.TokenMoveCommand): List<StateAndRef<TokenState>> {

      progressTracker.currentStep = CHECKING_BALANCE

      val criteria = QueryCriteria.VaultQueryCriteria(contractStateTypes = setOf(TokenState::class.java))
      val allMyTokens = serviceHub.vaultService.queryBy<TokenState>(criteria).states.filter { it.state.data.accountId == moveTokenCommand.fromAccountId }

      var total: Int = 0

      val enoughTokens = allMyTokens.takeWhile {

        if (total < moveTokenCommand.amount) {

          val tokenState = it.state.data

          total += tokenState.tokenProperties.amount
          true
        } else {
          false
        }
      }

      if (enoughTokens.sumBy { it.state.data.tokenProperties.amount } < moveTokenCommand.amount) {
        throw TokenException("Not enough funds to send ${moveTokenCommand.amount} to ${moveTokenCommand.toAccountId}")
      }

      return enoughTokens
    }


    @Suspendable
    fun verifyAndSignTransaction(txBuilder: TransactionBuilder): SignedTransaction {

      progressTracker.currentStep = VERIFYING_TRANSACTION
      txBuilder.verify(serviceHub)

      progressTracker.currentStep = SIGNING_TRANSACTION
      val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)

      return partiallySignedTx
    }

    @Suspendable
    fun getSignatureFromReceiver(partiallySignedTx: SignedTransaction): SignedTransaction {

      progressTracker.currentStep = GATHERING_SIGS
      val otherPartyFlowSession = initiateFlow(otherParty)
      val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, setOf(otherPartyFlowSession), GATHERING_SIGS.childProgressTracker()))

      return fullySignedTx
    }

    @Suspendable
    fun finaliseTransaction(fullySignedTx: SignedTransaction): SignedTransaction {

      progressTracker.currentStep = FINALISING_TRANSACTION
      val finalisedTx = subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
      return finalisedTx
    }

  }

  @InitiatedBy(TokenSender::class)
  class TokenReceiver(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
      val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
        override fun checkTransaction(stx: SignedTransaction) = requireThat {
          val output = stx.tx.outputs.single().data
          "This must be a Token transaction." using (output is TokenState)
          val tokenState = output as TokenState
          "check that this is going to a valid account" using ( isAccountValid(tokenState.accountId))
        }
      }

      return subFlow(signTransactionFlow)
    }

    fun isAccountValid(accountId : String) : Boolean {
      //TO DO - lookup account in the vault to check it is valid
      return accountId.length >= 8
    }
  }

}