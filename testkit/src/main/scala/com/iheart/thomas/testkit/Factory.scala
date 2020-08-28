package com.iheart.thomas.testkit

import java.time.OffsetDateTime

import com.iheart.thomas.abtest.model._

import scala.util.Random

object Factory {
  val now = OffsetDateTime.now

  def fakeAb(
      start: Int = 0,
      end: Int = 100,
      feature: String = "AMakeUpFeature" + Random.alphanumeric.take(5).mkString,
      alternativeIdName: Option[MetaFieldName] = None,
      groups: List[Group] = List(Group("A", 0.5, None), Group("B", 0.5, None)),
      userMetaCriteria: UserMetaCriteria = None,
      segRanges: List[GroupRange] = Nil,
      requiredTags: List[Tag] = Nil
    ): AbtestSpec = AbtestSpec(
    name = "test",
    author = "kai",
    feature = feature,
    start = now.plusDays(start.toLong),
    end = Some(now.plusDays(end.toLong)),
    groups = groups,
    alternativeIdName = alternativeIdName,
    userMetaCriteria = userMetaCriteria,
    segmentRanges = segRanges,
    requiredTags = requiredTags,
    groupMetas = Map()
  )
}
