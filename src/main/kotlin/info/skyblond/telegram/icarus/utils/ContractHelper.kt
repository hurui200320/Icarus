package info.skyblond.telegram.icarus.utils

import info.skyblond.telegram.icarus.models.WCAQueryResult
import io.neow3j.contract.FungibleToken
import io.neow3j.contract.GasToken
import io.neow3j.contract.SmartContract
import io.neow3j.protocol.Neow3j
import io.neow3j.protocol.core.response.NeoApplicationLog
import io.neow3j.protocol.core.response.NeoSendRawTransaction
import io.neow3j.protocol.http.HttpService
import io.neow3j.transaction.Signer
import io.neow3j.transaction.Transaction
import io.neow3j.types.ContractParameter
import io.neow3j.types.Hash160
import io.neow3j.types.Hash256
import io.neow3j.utils.Await
import io.neow3j.wallet.Account
import io.neow3j.wallet.Wallet
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.pow


object ContractHelper {
    private val logger = LoggerFactory.getLogger(ContractHelper::class.java)
    private const val CAT_TOKEN_CONTRACT_HASH = "0x6e5dc8d90d2704efe6f0342be30d206c788320f6"
    private const val WCA_CONTRACT_HASH = "0x7beaa74d0c29ada7a4462c3c7ce0965997901e14"

    private val neow3j = Neow3j.build(HttpService(ConfigHelper.config.neo.rpcServer))
    val account = ConfigHelper.config.neo.account
    val wallet = ConfigHelper.config.neo.wallet

    val gasToken = GasToken(neow3j)
    val catToken = FungibleToken(Hash160(CAT_TOKEN_CONTRACT_HASH), neow3j)
    val wcaContract = SmartContract(Hash160(WCA_CONTRACT_HASH), neow3j)

    private fun getGasWithDecimals(value: Long): Double {
        return value / (10.0).pow(8)
    }

    fun getCatBalance(address: String): BigDecimal =
        BigDecimal(catToken.getBalanceOf(Account.fromAddress(address)))
            .divide(BigDecimal.TEN.pow(2))

    fun getGasBalance(address: String): BigDecimal =
        BigDecimal(gasToken.getBalanceOf(Account.fromAddress(address)))
            .divide(BigDecimal.TEN.pow(8))

    fun transferToken(token: FungibleToken, fromWallet: Wallet, receiver: Account, amount: Long): Hash256 {
        val tx: NeoSendRawTransaction = token.transferFromDefaultAccount(
            fromWallet,
            receiver.scriptHash,
            BigInteger.valueOf(amount)
        ).signers(Signer.calledByEntry(fromWallet.defaultAccount)).sign().send()

        if (tx.hasError()) {
            throw Exception(tx.error.message)
        }
        return tx.sendRawTransaction.hash
    }

    private fun buildTxAndSend(
        contract: SmartContract, function: String,
        parameters: List<ContractParameter>, signers: List<Signer>,
        wallet: Wallet
    ): Pair<Transaction, NeoSendRawTransaction> {
        val tx = contract
            .invokeFunction(function, *parameters.toTypedArray())
            .signers(*signers.toTypedArray()).wallet(wallet).sign()
        val response = tx.send()
        return Pair(tx, response)
    }

    @Suppress("SameParameterValue")
    private fun invokeFunction(
        contract: SmartContract, function: String,
        parameters: List<ContractParameter>, signers: List<Signer>, wallet: Wallet
    ): Triple<Hash256, Double, NeoApplicationLog> {
        val (tx, resp) = buildTxAndSend(contract, function, parameters, signers, wallet)
        if (resp.hasError()) {
            throw Exception(String.format("Error when invoking %s: %s", function, resp.error.message))
        }
        // have to wait, otherwise execution list is null
        Await.waitUntilTransactionIsExecuted(tx.txId, neow3j)
        return Triple(tx.txId, getGasWithDecimals(tx.systemFee + tx.networkFee), tx.applicationLog)
    }

    fun queryWCAJson(
        identifier: String, wallet: Wallet
    ): Triple<Hash256, Double, WCAQueryResult?> {
        val appLog = invokeFunction(
            wcaContract, "queryWCA",
            listOf(
                ContractParameter.string(identifier)
            ),
            listOf<Signer>(
                Signer.calledByEntry(wallet.defaultAccount)
            ),
            wallet
        )

        return Triple(
            appLog.first,
            appLog.second,
            WCAQueryResult.fromNeoJson(appLog.third.executions[0].stack[0].string)
        )
    }
}