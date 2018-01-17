package io.cordite.cielonacio.token.schema

import net.corda.core.crypto.SecureHash
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Index
import javax.persistence.Table

object TokenSchema

object TokenSchemaV1 : MappedSchema(schemaFamily = TokenSchema.javaClass, version = 1, mappedTypes = listOf(PersistentToken::class.java)) {

  @Entity
  @Table(name = "Token"
    //indexes = arrayOf(Index(name = "index_Token", columnList = "Token_id", unique = true))
  )
  class PersistentToken(
    @Column(name = "token_id", nullable = false)
    var tokenId: String,

    @Column(name = "symbol", nullable = false)
    var symbol: String,

    @Column(name = "description", nullable = false)
    var description: String,

    @Column(name = "amount", nullable = false)
    var amount : Int,

    @Column(name = "owner_key", nullable = false)
    var owner: String,

    @Column(name = "account_id", nullable = false)
    var accountId: String

    // TODO: add some metadata here
  ) : PersistentState()

  data class TxIDAndTokenSchema(val txid: SecureHash, val Token: PersistentToken)

  /*
  fun TokenQuery(TokenId: String): TxIDAndTokenSchema? {
    return measure(TokenSchema::class, "TokenQuery") {
      val tableName = "CONTRACT_Token"
      if (TransactionManager.current().connection.metaData.getTables(null, null, tableName, null).next()) {
        val query = "SELECT * FROM $tableName WHERE Token_ID = '$TokenId'"
          .trimMargin()
          .replace("[\n\r]+".toRegex(), " ")
        val rs = TransactionManager.current().prepareStatement(query).executeQuery()
        while (rs.next()) {
          val txid = rs.getString("TRANSACTION_ID")
          val ownerId = rs.getString("OWNER_KEY")
          return@measure TxIDAndTokenSchema(SecureHash.parse(txid), PersistentToken(TokenId, ownerId))
        }
      }
      return@measure null
    }
  }
  */

}


