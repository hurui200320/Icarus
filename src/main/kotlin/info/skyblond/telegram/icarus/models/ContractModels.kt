package info.skyblond.telegram.icarus.models

import info.skyblond.telegram.icarus.utils.JsonHelper
import io.neow3j.crypto.Base64
import io.neow3j.types.Hash160
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

data class WCAMilestone(
    val description: String,
    val endTimestamp: Long,
    val linkToResult: String?
) {
    override fun toString(): String {
        return "    milestone description: $description\n" +
                "    end time: ${
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimestamp), ZoneOffset.UTC)
                }\n" +
                "    linkToResult: ${if (linkToResult == null || linkToResult == "null") "Not finished." else linkToResult}"
    }
}

data class WCAQueryResult(
    val owner: String,
    val stakePer100Token: Int,
    val maxTokenSoldCount: Long,
    val remainTokenCount: Long,
    val buyerCount: Long,
    val milestonesCount: Int,
    val milestones: List<WCAMilestone>,
    val thresholdMilestoneIndex: Int,
    val nextMilestone: Int,
    val stakePaid: Boolean
) {

    companion object {
        fun fromNeoJson(json: String?): WCAQueryResult? {
            println(json)
            if (json == null || json.isBlank())
                return null
            val jsonIter = JsonHelper.getObjectMapper().readTree(json).iterator()

            return WCAQueryResult(
                // owner
                Hash160(Base64.decode(jsonIter.next().asText()).reversedArray()).toAddress(),
                // stakePer100Token
                jsonIter.next().asInt(),
                // maxTokenSoldCount
                jsonIter.next().asLong(),
                // remainTokenCount
                jsonIter.next().asLong(),
                // buyerCount
                jsonIter.next().asLong(),
                // milestonesCount
                jsonIter.next().asInt(),
                // milestones
                jsonIter.next().map {
                    val iter = it.iterator()

                    WCAMilestone(
                        // description
                        iter.next().asText(),
                        // endTimestamp
                        iter.next().asLong(),
                        // linkToResult
                        iter.next().asText()
                    )
                },
                // thresholdMilestoneIndex
                jsonIter.next().asInt(),
                // nextMilestone
                jsonIter.next().asInt(),
                // stakePaid
                jsonIter.next().asInt() == 1
            )
        }
    }

    override fun toString(): String {
        return "owner address: $owner\n" +
                "stake rate: ${stakePer100Token / 100.0}\n" +
                "sold token limit: ${maxTokenSoldCount / 100.0}\n" +
                "remain token: ${remainTokenCount / 100.0}\n" +
                "buyer count: $buyerCount\n" +
                "milestones count: $milestonesCount\n" +
                "milestones: \n${milestones.joinToString("\n\n")}\n\n" +
                "threshold milestone: ${milestones[thresholdMilestoneIndex].description}\n" +
                "${
                    if (nextMilestone == milestonesCount)
                        "All milestones are finished."
                    else
                        "next milestone: " + milestones[nextMilestone].description
                }\n" +
                "stake paid: ${if (stakePaid) "yes" else "no"}"
    }
}