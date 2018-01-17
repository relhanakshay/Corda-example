package com.example.state

import com.example.contract.TokenCommands
import com.example.contract.TokenContract
import io.cordite.cielonacio.token.schema.TokenSchemaV1
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.toBase58String
import java.util.*



/*

We are making a class called TokenProperties. Basically, the token will have an amount or value,
symbol or currency, description and a randomly generated tokenId.

So, basically 4 properties in total.
1. amount : Int
2. symbol : String
3. description : String
4. tokenId : String = UUID.randomUUID().toString()

Add these properties in the TokenProperties class and remove errors from the file.

 */


@CordaSerializable
data class TokenProperties(val amount : Int,
                           val symbol : String,
                           val description : String,
                           val tokenId: String = UUID.randomUUID().toString()
)


/**
 * Please note: the owner key is a composite of the pubkeys for the actor that requested the creation of this Token
 * and the pubkey for the user that this Token represents
 */


/*
In TokenState class, we are using the TokenProperties class which we made earlier.
Now, a token can have an owner. So, we are defining an owner for it and implementing OwnableState interface.
Moreover, owner of a token can be changed when we are proposing a transaction for the same.
withNewOwner function helps to change the owner without changing any other property of the token.


To map our state to database schema, we are using QueryableState.
Now, we want to have an unique identifier for each token, so LinearState is used.

 */



data class TokenState(val tokenProperties: TokenProperties,
                      override val owner: AbstractParty,
                      val accountId : String) : OwnableState, QueryableState, LinearState {


  override fun withNewOwner(newOwner: AbstractParty) = CommandAndState(TokenCommands.Move(), copy(owner = newOwner))

  val contract: Contract = TokenContract()
  override val participants: List<AbstractParty> = listOf(owner)
  override val linearId: UniqueIdentifier = tokenProperties.tokenId.toTokenStateID()

  override fun generateMappedObject(schema: MappedSchema): PersistentState {
    return when (schema) {
      is TokenSchemaV1 -> TokenSchemaV1.PersistentToken(
        tokenId = tokenProperties.tokenId,
        symbol = tokenProperties.symbol,
        description = tokenProperties.description,
        amount = tokenProperties.amount,
        owner =  owner.owningKey.toBase58String(),
        accountId = accountId)
      else -> throw IllegalArgumentException("Unrecognised schema $schema")
    }
  }
  override fun supportedSchemas(): Iterable<MappedSchema> = listOf(TokenSchemaV1)

}


fun String.toTokenStateID() = UniqueIdentifier(this, UUID.randomUUID())


/*

val amount : Int,
        val symbol : String,
        val description : String,
        val tokenId: String = UUID.randomUUID().toString()


 */