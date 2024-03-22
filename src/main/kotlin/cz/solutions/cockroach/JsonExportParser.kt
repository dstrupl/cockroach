@file:OptIn(ExperimentalSerializationApi::class)

package cz.solutions.cockroach

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

class JsonExportParser {


    @OptIn(ExperimentalSerializationApi::class)
    private val jsonReader = Json {
        ignoreUnknownKeys = true
        namingStrategy = FirstUpperCase
        serializersModule = SerializersModule {
            contextual(LocalDateSerializer)
            contextual(PriceSerializer)
        }
    }

    fun parse(data: String): ParsedExport {
        val export: SchwabExport = jsonReader.decodeFromString(data)


        return ParsedExport(
            export.transactions.filterIsInstance(Transaction.RsuDepositTransaction::class.java).map {
                check(it.transactionDetails.size==1)
                RsuRecord(
                    it.date,
                    it.quantity,
                    it.transactionDetails[0].details.vestFairMarketValue,
                    it.transactionDetails[0].details.vestDate,
                )
            },
            export.transactions.filterIsInstance(Transaction.EsppDepositTransaction::class.java).map {
                check(it.transactionDetails.size==1)
                EsppRecord(
                    it.date,
                    it.quantity,
                    it.transactionDetails[0].details.purchasePrice,
                    it.transactionDetails[0].details.subscriptionFairMarketValue,
                    it.transactionDetails[0].details.purchaseFairMarketValue,
                    it.transactionDetails[0].details.purchaseDate
                )
            },
            export.transactions.filterIsInstance(Transaction.DividendTransaction::class.java).map {
                DividendRecord(
                    it.date,
                    it.amount
                )
            },
            export.transactions.filterIsInstance(Transaction.TaxWithholdingTransaction::class.java).map {
                TaxRecord(
                    it.date,
                    it.amount
                )
            },
            export.transactions.filterIsInstance(Transaction.TaxReversalTransaction::class.java).map {
                TaxReversalRecord(
                    it.date,
                    it.amount
                )
            },

            export.transactions.filterIsInstance(Transaction.SaleTransaction::class.java).flatMap {
               it.transactionDetails.map {transactionDetail->
                   SaleRecord(
                       it.date,
                       transactionDetail.details.type(),
                       transactionDetail.details.shares,
                       transactionDetail.details.salePrice,
                       transactionDetail.details.purchasePrice(),
                       transactionDetail.details.purchaseFmv(),
                       transactionDetail.details.purchaseDate()
                   )
               }

            },
            export.transactions.filterIsInstance(Transaction.JournalTransaction::class.java).map {
                JournalRecord(
                    it.date,
                    it.amount?:0.0,
                    it.description
                )
            },
        )

    }

}

object TransactionSerializer : JsonContentPolymorphicSerializer<Transaction>(
    Transaction::class,
) {
    override fun selectDeserializer(
        element: JsonElement,
    ): DeserializationStrategy<Transaction> {
        val json = element.jsonObject
        val action = json.getValue("Action").jsonPrimitive.content
        return when (action) {
            "Dividend" -> Transaction.DividendTransaction.serializer()
            "Sale" -> Transaction.SaleTransaction.serializer()
            "Deposit" -> {
                val desc = json.getValue("Description").jsonPrimitive.content
                when (desc) {
                    "ESPP" -> Transaction.EsppDepositTransaction.serializer()
                    "RS" -> Transaction.RsuDepositTransaction.serializer()
                    "Div Reinv" -> Transaction.DivReinvTransaction.serializer()
                    else -> error("invalid deposit desc $desc")
                }
            }
            "Dividend Reinvested" ->  Transaction.DividentReinvestedTransaction.serializer()
            "Wire Transfer" -> Transaction.WireTransferTransaction.serializer()

            "Tax Withholding" -> Transaction.TaxWithholdingTransaction.serializer()
            "Tax Reversal" -> Transaction.TaxReversalTransaction.serializer()
            "Journal" -> Transaction.JournalTransaction.serializer()

            else -> error("unknown action $action")
        }
    }
}

@Serializable(TransactionSerializer::class)
sealed class Transaction {
    abstract val date: LocalDate

    @Serializable
    data class DividendTransaction(
        @Contextual
        override val date: LocalDate,
        @Contextual
        val amount: Double
    ) : Transaction()

    @Serializable
    data class SaleTransaction(
        @Contextual
        override val date: LocalDate,
        @Contextual
        val amount: Double,
        val transactionDetails: List<SalesTransactionDetails>
    ) : Transaction()


    @Serializable
    data class EsppDepositTransaction(
        @Contextual
        override val date: LocalDate,

        val quantity: Int,
        val transactionDetails: List<EsppTransactionDetails>

    ) : Transaction()

    @Serializable
    data class RsuDepositTransaction(
        @Contextual
        override val date: LocalDate,

        val quantity: Int,
        val transactionDetails: List<RsuTransactionDetails>
    ) : Transaction()

    @Serializable
    data class DivReinvTransaction(
        @Contextual
        override val date: LocalDate,

        val quantity: Double,
    ) : Transaction()

    @Serializable
    data class TaxWithholdingTransaction(
        @Contextual
        override val date: LocalDate,

        @Contextual
        val amount: Double,
    ) : Transaction()

    @Serializable
    data class TaxReversalTransaction(
        @Contextual
        override val date: LocalDate,

        @Contextual
        val amount: Double,
    ) : Transaction()

    @Serializable
    data class JournalTransaction(
        @Contextual
        override val date: LocalDate,

        @Contextual
        val amount: Double?,

        val description: String
    ) : Transaction()

    @Serializable
    data class DividentReinvestedTransaction(
        @Contextual
        override val date: LocalDate,

        @Contextual
        val amount: Double,

        val description: String
    ) : Transaction()


    @Serializable
    data class WireTransferTransaction(
        @Contextual
        override val date: LocalDate,

        @Contextual
        val amount: Double,

        val description: String
    ) : Transaction()


}


@Serializable
data class EsppTransactionDetails(
    val details: EsppTransactionDetails
) {
    @Serializable
    data class EsppTransactionDetails(
        @Contextual
        val purchaseDate: LocalDate,

        @Contextual
        val purchasePrice: Double,

        @Contextual
        val subscriptionDate: LocalDate,


        @Contextual
        val subscriptionFairMarketValue: Double,

        @Contextual
        val purchaseFairMarketValue: Double
    )
}


@Serializable
data class RsuTransactionDetails(
    val details: RsuTransactionDetails
) {

    @Serializable
    data class RsuTransactionDetails(
        @Contextual
        val awardDate: LocalDate,

        @Contextual
        val vestFairMarketValue: Double,

        @Contextual
        val vestDate: LocalDate,

        )
}


@Serializable
data class SalesTransactionDetails(
    val details: SalesTransactionDetails
) {
    @Serializable
    @JsonClassDiscriminator("Type")
    sealed class SalesTransactionDetails {
        abstract val salePrice: Double
        abstract val shares: Int

        abstract fun purchasePrice(): Double
        abstract fun purchaseFmv(): Double
        abstract fun purchaseDate(): LocalDate

        abstract fun type(): String

        @Serializable
        @SerialName("RS")
        data class RSUSalesTransactionDetails(
            @Contextual
            override val salePrice: Double,
            override val shares: Int,

            @Contextual
            val vestDate: LocalDate,

            @Contextual
            val vestFairMarketValue: Double,
        ) : SalesTransactionDetails() {
            override fun purchasePrice(): Double {
                return vestFairMarketValue
            }

            override fun purchaseFmv(): Double {
                return vestFairMarketValue
            }

            override fun purchaseDate(): LocalDate {
                return vestDate
            }

            override fun type(): String {
                return "RS"
            }
        }

        @Serializable
        @SerialName("ESPP")
        data class ESPSalesTransactionDetails(
            @Contextual
            override val salePrice: Double,

            override val shares: Int,

            @Contextual
            val purchaseDate: LocalDate,

            @Contextual
            val purchaseFairMarketValue: Double,

            @Contextual
            val purchasePrice: Double,
        ) : SalesTransactionDetails() {
            override fun purchasePrice(): Double {
                return purchasePrice
            }

            override fun purchaseFmv(): Double {
                return purchaseFairMarketValue
            }

            override fun purchaseDate(): LocalDate {
                return purchaseDate
            }

            override fun type(): String {
                return "ESPP"
            }
        }
    }
}


object LocalDateSerializer : KSerializer<LocalDate> {
    private val formatter = DateTimeFormat.forPattern("MM/dd/yyyy")

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString(), formatter)
    }
}

object PriceSerializer : KSerializer<Double> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeString("$${value}")
    }

    override fun deserialize(decoder: Decoder): Double {
        return decoder.decodeString().replace("$", "").replace(",", "").toDouble()
    }
}

@Serializable
data class SchwabExport(
    val transactions: List<Transaction>
)

object FirstUpperCase : JsonNamingStrategy {
    override fun serialNameForJson(descriptor: SerialDescriptor, elementIndex: Int, serialName: String): String {
        return serialName.replaceFirstChar { it.uppercase() }
    }
}

