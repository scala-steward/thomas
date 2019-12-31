package com.iheart.thomas
package bandit
package bayesian

import java.time.Instant

import cats.Monoid
import com.iheart.thomas.analysis.{KPIName, Probability}
import cats.implicits._

case class BanditState[R](
    feature: FeatureName,
    title: String,
    author: String,
    arms: List[ArmState[R]],
    start: Instant,
    kpiName: KPIName,
    minimumSizeChange: Double,
    initialSampleSize: Int) {

  def rewardState: Map[ArmName, R] =
    arms.map(as => (as.name, as.rewardState)).toMap

  def distribution: Map[ArmName, Probability] =
    arms.map(as => (as.name, as.likelihoodOptimum)).toMap

  def updateArms(rewards: Map[ArmName, R])(implicit RS: Monoid[R]): BanditState[R] =
    copy(arms = arms.map { arm =>
      arm.copy(
        rewardState = rewards
          .get(arm.name)
          .fold(arm.rewardState)(arm.rewardState |+| _)
      )
    })

  def getArm(armName: ArmName): Option[ArmState[R]] =
    arms.find(_.name === armName)

}

case class ArmState[R](
    name: ArmName,
    rewardState: R,
    likelihoodOptimum: Probability)
