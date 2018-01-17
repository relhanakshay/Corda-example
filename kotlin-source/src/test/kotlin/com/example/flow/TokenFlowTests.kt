package com.example.flow

import com.example.state.TokenProperties
import com.example.state.TokenState
import net.corda.core.flows.NotaryChangeFlow
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import net.corda.core.utilities.getOrThrow
import net.corda.node.internal.StartedNode
import net.corda.testing.setCordappPackages
import kotlin.test.assertEquals


class TokenFlowTests {

  companion object {
    val ACCOUNT_1 = "account-1"
    val ACCOUNT_2 = "account-2"
  }

  private lateinit var mockNet: MockNetwork
  private lateinit var sector1NotaryNode: StartedNode<MockNetwork.MockNode>
  private lateinit var sector2NotaryNode: StartedNode<MockNetwork.MockNode>
  private lateinit var centralBankNode : StartedNode<MockNetwork.MockNode>
  private lateinit var sector1BankNode : StartedNode<MockNetwork.MockNode>
  private lateinit var sector2BankNode : StartedNode<MockNetwork.MockNode>
  private lateinit var centralBank: Party
  private lateinit var sector1Bank: Party
  private lateinit var sector2Bank: Party
  private lateinit var sector1Notary : Party
  private lateinit var sector2Notary : Party

  @Before
  fun before() {

    setCordappPackages("io.cordite.cielonacio.token.contract")

    mockNet = MockNetwork()
    sector1NotaryNode = mockNet.createNotaryNode(legalName =  CordaX500Name("Sector1Notary","cielonacio","GB") )
    sector1Notary = sector1NotaryNode.info.legalIdentities.first()

    centralBankNode = mockNet.createPartyNode(networkMapAddress = sector1NotaryNode.network.myAddress, legalName =  CordaX500Name("CentralBank", "London", "GB"))
    centralBankNode.registerInitiatedFlow(TokenExchangeFlows.TokenReceiver::class.java)
    centralBank = centralBankNode.info.legalIdentities.first()

    sector1BankNode = mockNet.createPartyNode(networkMapAddress = sector1NotaryNode.network.myAddress, legalName =  CordaX500Name("Sector1Bank", "London", "GB"))
    sector1BankNode.registerInitiatedFlow(TokenExchangeFlows.TokenReceiver::class.java)
    sector1Bank = sector1BankNode.info.legalIdentities.first()

    sector2NotaryNode = mockNet.createNotaryNode(networkMapAddress = sector1NotaryNode.network.myAddress, legalName = CordaX500Name("Sector2Notary","cielonacio", "GB"))
    sector2Notary = sector2NotaryNode.info.legalIdentities.first()

    sector2BankNode = mockNet.createPartyNode(networkMapAddress = sector1NotaryNode.network.myAddress, legalName =  CordaX500Name("Sector2Bank", "London", "GB"))
    sector2BankNode.registerInitiatedFlow(TokenExchangeFlows.TokenReceiver::class.java)
    sector2Bank = sector2BankNode.info.legalIdentities.first()

  }


  @Test
  fun `I can issue a token from the central bank`(){

    centralBank = centralBankNode.info.legalIdentities.single()
    sector1Bank = sector1BankNode.info.legalIdentities.single()

    val xhoTokenId1 = TokenProperties( 1000, "XHO", "cielo-nacio native currency helio")

    val tokenIssueFlowCommand = TokenIssueFlowCommand.TokenIssueCommand(ACCOUNT_1, sector1Bank.name.organisation,"Sector1Notary",xhoTokenId1)
    val tokenIssueFlow = TokenExchangeFlows.TokenSender(sector1Bank, tokenIssueFlowCommand)
    val tokenIssueFlowFuture = centralBankNode.services.startFlow(tokenIssueFlow).resultFuture

    mockNet.runNetwork()

    val tokenIssueResult = tokenIssueFlowFuture.getOrThrow()
    println("result: " + (tokenIssueResult.tx.outputs[0].data as TokenState).toString())
    println("result: " + (tokenIssueResult.tx.outputs[0].data as TokenState).owner.nameOrNull())
    assert(((tokenIssueResult.tx.outputs[0].data as TokenState).owner.nameOrNull().toString()== sector1Bank.name.toString()))
  }

  @Test
  fun `I can send another bank at token with the same notary`(){

    centralBank = centralBankNode.info.legalIdentities.single()
    sector1Bank = sector1BankNode.info.legalIdentities.single()

    val xhoTokenId1 = TokenProperties( 1000, "XHO", "cielo-nacio native currency helio")

    val tokenIssueFlowCommand = TokenIssueFlowCommand.TokenIssueCommand(ACCOUNT_1, sector1Bank.name.organisation,"Sector1Notary",xhoTokenId1)
    val tokenIssueFlow = TokenExchangeFlows.TokenSender(sector1Bank, tokenIssueFlowCommand)
    val tokenIssueFlowFuture = centralBankNode.services.startFlow(tokenIssueFlow).resultFuture

    mockNet.runNetwork()

    val tokenIssueResult = tokenIssueFlowFuture.getOrThrow()
    println("result: " + (tokenIssueResult.tx.outputs[0].data as TokenState).toString())
    println("result: " + (tokenIssueResult.tx.outputs[0].data as TokenState).owner.nameOrNull())
    assert(((tokenIssueResult.tx.outputs[0].data as TokenState).owner.nameOrNull().toString()== sector1Bank.name.toString()))

    val tokenMoveFlowCommand = TokenMoveFlowCommand.TokenMoveCommand(ACCOUNT_1, ACCOUNT_2, sector2Bank.name.organisation, 1000, "XHO")

    val tokenMoveFlow = TokenExchangeFlows.TokenSender(sector2Bank,tokenMoveFlowCommand)
    val tokenMoveFlowFuture = sector1BankNode.services.startFlow(tokenMoveFlow).resultFuture

    mockNet.runNetwork()

    val tokenMoveResult = tokenMoveFlowFuture.getOrThrow()
    println("result: " + (tokenMoveResult.tx.outputs[0].data as TokenState).toString())
    assert((tokenMoveResult.tx.outputs[0].data as TokenState).tokenProperties.amount==1000)
    assert((tokenMoveResult.tx.outputs[0].data as TokenState).owner.nameOrNull().toString()== sector2Bank.name.toString())
    assert((tokenMoveResult.tx.outputs[0].data as TokenState).accountId== ACCOUNT_2)


    sector2BankNode.database.transaction {

      val criteria = QueryCriteria.VaultQueryCriteria(contractStateTypes = setOf(TokenState::class.java))
      val allMyTokens = sector2BankNode.services.vaultService.queryBy<TokenState>(criteria).states

      println("all tokens in sector 2 bank")
      allMyTokens.forEach { println(it.toString()) }

      assert(allMyTokens.first().state.data.accountId == ACCOUNT_2)

    }
  }

  @Test
  fun `I can issue via two different notaries and then send across banks`(){

    centralBank = centralBankNode.info.legalIdentities.single()
    sector1Bank = sector1BankNode.info.legalIdentities.single()
    sector2Bank = sector2BankNode.info.legalIdentities.single()

    val xhoTokenId1 = TokenProperties( 1000, "XHO", "cielo-nacio native currency helio")
    val xhoTokenId2 = TokenProperties( 500, "XHO", "cielo-nacio native currency helio")


    val tokenIssueFlowCommand1 = TokenIssueFlowCommand.TokenIssueCommand(ACCOUNT_1, sector1Bank.name.organisation,"Sector1Notary",xhoTokenId1)
    val tokenIssueFlow1 = TokenExchangeFlows.TokenSender(sector1Bank, tokenIssueFlowCommand1)
    val tokenIssueFlowFuture1 = centralBankNode.services.startFlow(tokenIssueFlow1).resultFuture

    val tokenIssueFlowCommand2 = TokenIssueFlowCommand.TokenIssueCommand(ACCOUNT_2, sector2Bank.name.organisation,"Sector2Notary",xhoTokenId2)
    val tokenIssueFlow2 = TokenExchangeFlows.TokenSender(sector2Bank, tokenIssueFlowCommand2)
    val tokenIssueFlowFuture2 = centralBankNode.services.startFlow(tokenIssueFlow2).resultFuture


    mockNet.runNetwork()

    val tokenIssueResult1 = tokenIssueFlowFuture1.getOrThrow()
    println("result: " + (tokenIssueResult1.tx.outputs[0].data as TokenState).toString())
    println("result: " + (tokenIssueResult1.tx.outputs[0].data as TokenState).owner.nameOrNull())
    assert(((tokenIssueResult1.tx.outputs[0].data as TokenState).owner.nameOrNull().toString()== sector1Bank.name.toString()))

    val tokenIssueResult2 = tokenIssueFlowFuture2.getOrThrow()
    println("result: " + (tokenIssueResult2.tx.outputs[0].data as TokenState).toString())
    println("result: " + (tokenIssueResult2.tx.outputs[0].data as TokenState).owner.nameOrNull())
    assert(((tokenIssueResult2.tx.outputs[0].data as TokenState).owner.nameOrNull().toString()== sector2Bank.name.toString()))

    val criteria = QueryCriteria.VaultQueryCriteria(contractStateTypes = setOf(TokenState::class.java))

    sector1BankNode.database.transaction {

      val sector1BankTokens = sector1BankNode.services.vaultService.queryBy<TokenState>(criteria).states

      println("all tokens in sector 1 bank")
      sector1BankTokens.forEach { println(it.toString()) }

      assert(sector1BankTokens.first().state.data.accountId == ACCOUNT_1)
      assert(sector1BankTokens.sumBy{ it.state.data.tokenProperties.amount } == 1000)
    }

    sector2BankNode.database.transaction {

      val sector2BankTokens = sector2BankNode.services.vaultService.queryBy<TokenState>(criteria).states

      println("all tokens in sector 2 bank")
      sector2BankTokens.forEach { println(it.toString()) }

      assert(sector2BankTokens.first().state.data.accountId == ACCOUNT_2)
      assert(sector2BankTokens.sumBy{ it.state.data.tokenProperties.amount } == 500)

    }

    val tokenMoveFlowCommand1 = TokenMoveFlowCommand.TokenMoveCommand(ACCOUNT_1, ACCOUNT_2, sector2Bank.name.organisation, 1000, "XHO")
    val tokenMoveFlow1 = TokenExchangeFlows.TokenSender(sector2Bank,tokenMoveFlowCommand1)
    val tokenMoveFlowFuture1 = sector1BankNode.services.startFlow(tokenMoveFlow1).resultFuture

    val tokenMoveFlowCommand2 = TokenMoveFlowCommand.TokenMoveCommand(ACCOUNT_2, ACCOUNT_1, sector1Bank.name.organisation, 500, "XHO")
    val tokenMoveFlow2 = TokenExchangeFlows.TokenSender(sector1Bank,tokenMoveFlowCommand2)
    val tokenMoveFlowFuture2 = sector2BankNode.services.startFlow(tokenMoveFlow2).resultFuture


    mockNet.runNetwork()

    val tokenMoveResult1 = tokenMoveFlowFuture1.getOrThrow()
    println("result: " + (tokenMoveResult1.tx.outputs[0].data as TokenState).toString())
    assert((tokenMoveResult1.tx.outputs[0].data as TokenState).tokenProperties.amount==1000)
    assert((tokenMoveResult1.tx.outputs[0].data as TokenState).owner.nameOrNull().toString()== sector2Bank.name.toString())
    assert((tokenMoveResult1.tx.outputs[0].data as TokenState).accountId== ACCOUNT_2)

    val tokenMoveResult2 = tokenMoveFlowFuture2.getOrThrow()
    println("result: " + (tokenMoveResult2.tx.outputs[0].data as TokenState).toString())
    assert((tokenMoveResult2.tx.outputs[0].data as TokenState).tokenProperties.amount==500)
    assert((tokenMoveResult2.tx.outputs[0].data as TokenState).owner.nameOrNull().toString()== sector1Bank.name.toString())
    assert((tokenMoveResult2.tx.outputs[0].data as TokenState).accountId== ACCOUNT_1)

    sector1BankNode.database.transaction {

      val sector1BankTokens = sector1BankNode.services.vaultService.queryBy<TokenState>(criteria).states

      println("all tokens in sector 1 bank")
      sector1BankTokens.forEach { println(it.toString()) }

      assert(sector1BankTokens.first().state.data.accountId == ACCOUNT_1)
      assert(sector1BankTokens.sumBy { it.state.data.tokenProperties.amount } == 500)
      assert(sector1BankTokens.all { it.state.notary.name.organisation == sector2Notary.name.organisation })
    }

    sector2BankNode.database.transaction {

      val sector2BankTokens = sector2BankNode.services.vaultService.queryBy<TokenState>(criteria).states

      println("all tokens in sector 2 bank")
      sector2BankTokens.forEach { println(it.toString()) }

      assert(sector2BankTokens.first().state.data.accountId == ACCOUNT_2)
      assert(sector2BankTokens.sumBy{ it.state.data.tokenProperties.amount } == 1000)
      assert(sector2BankTokens.all { it.state.notary.name.organisation == sector1Notary.name.organisation })
    }
  }

  @Test
  fun `I can switch state from one notary to another`(){

    centralBank = centralBankNode.info.legalIdentities.single()
    sector1Bank = sector1BankNode.info.legalIdentities.single()

    val xhoTokenId1 = TokenProperties(1000, "XHO", "cielo-nacio native currency helio","XXXXX01")

    val tokenIssueFlowCommand = TokenIssueFlowCommand.TokenIssueCommand(ACCOUNT_1, sector1Bank.name.organisation,"Sector1Notary",xhoTokenId1)
    val tokenIssueFlow = TokenExchangeFlows.TokenSender(sector1Bank, tokenIssueFlowCommand)
    val tokenIssueFlowFuture = centralBankNode.services.startFlow(tokenIssueFlow).resultFuture

    mockNet.runNetwork()

    val tokenIssueResult = tokenIssueFlowFuture.getOrThrow()
    println("result: " + (tokenIssueResult.tx.outputs[0].data as TokenState).toString())
    println("result: " + (tokenIssueResult.tx.outputs[0].data as TokenState).owner.nameOrNull())
    assert(((tokenIssueResult.tx.outputs[0].data as TokenState).owner.nameOrNull().toString()== sector1Bank.name.toString()))

    val originalTokenState = tokenIssueResult.tx.outRef<TokenState>(0)

    assertEquals(originalTokenState.state.notary.name.organisation, sector1Notary.name.organisation)

    val notaryChangeFlow = NotaryChangeFlow(originalTokenState, sector2Notary)
    val notaryChangeFuture = sector1BankNode.services.startFlow(notaryChangeFlow)

    mockNet.runNetwork()

    val notaryChangeResult = notaryChangeFuture.resultFuture.getOrThrow()

    assertEquals(notaryChangeResult.state.notary.name, sector2NotaryNode.info.legalIdentities.first().name)
}


  @After
  fun tearDown() {
    mockNet.stopNodes()
  }
}