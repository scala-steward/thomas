package com.iheart.thomas
package client

import java.time.Instant

import com.iheart.thomas.abtest.{AssignGroups, TestsData}
import cats.effect.{ConcurrentEffect, Resource, Timer}
import com.iheart.thomas.abtest.model.{GroupMeta, UserGroupQuery}
import mau.RefreshRef
import cats.implicits._

import scala.concurrent.duration.FiniteDuration

abstract class AutoRefreshAssignGroups[F[_]] {
  def assign(query: UserGroupQuery): F[Map[FeatureName, AssignmentWithMeta]]
}

object AutoRefreshAssignGroups {

  case class Config(
      refreshPeriod: FiniteDuration,
      staleTimeout: FiniteDuration,
      testsRange: Option[FiniteDuration])

  def resource[F[_]: Timer](
      abtestClient: AbtestClient[F],
      config: Config
    )(implicit F: ConcurrentEffect[F],
      nowF: F[Instant]
    ): Resource[F, AutoRefreshAssignGroups[F]] =
    resource[F](
      abtestClient,
      refreshPeriod = config.refreshPeriod,
      staleTimeout = config.staleTimeout,
      testsRange = config.testsRange
    )

  /**
    *
    * @param abtestClient  client to get A/B tests data
    * @param refreshPeriod  how ofter the data is refreshed
    * @param staleTimeout how stale is the data allowed to be (in cases when refresh fails)
    * @param testsRange time range during which valid tests are used to for getting assignment.
    *                   if the `at` field in the UserGroupQuery is outside this range, the assignment
    *                   will fail
    * @return A Resource of An [[AutoRefreshAssignGroups]]
    */
  def resource[F[_]: Timer](
      abtestClient: AbtestClient[F],
      refreshPeriod: FiniteDuration,
      staleTimeout: FiniteDuration,
      testsRange: Option[FiniteDuration]
    )(implicit F: ConcurrentEffect[F],
      nowF: F[Instant]
    ): Resource[F, AutoRefreshAssignGroups[F]] = {
    RefreshRef
      .resource[F, TestsData]((_: TestsData) => F.unit) //todo: possibly add logging here.
      .map { ref =>
        new AutoRefreshAssignGroups[F] {
          def assign(
              query: UserGroupQuery
            ): F[Map[FeatureName, AssignmentWithMeta]] = {
            for {
              data <- ref
                .getOrFetch(refreshPeriod, staleTimeout)(
                  nowF.flatMap { now =>
                    abtestClient
                      .testsData(
                        testsRange.fold(now)(tr => now.minusNanos(tr.toNanos)),
                        testsRange
                      )
                  }
                )(PartialFunction.empty)

              assignment <- AssignGroups.assign[F](data, query, staleTimeout)
            } yield {
              assignment.map {
                case (fn, (gn, test)) =>
                  (fn, AssignmentWithMeta(gn, test.groupMetas.get(gn)))

              }
            }
          }
        }
      }
  }
}

case class AssignmentWithMeta(
    groupName: GroupName,
    meta: Option[GroupMeta])